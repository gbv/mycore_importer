package de.vzg.oai_importer.mycore;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.test.util.ReflectionTestUtils;

import de.vzg.oai_importer.ImportJobConfiguration;
import de.vzg.oai_importer.ImporterConfiguration;
import de.vzg.oai_importer.mapping.MappingService;
import de.vzg.oai_importer.mapping.jpa.MappingGroup;

public class MyCoReAuthenticationProviderTest {

    private static final String REPPER = "https://repper.example.org/";

    private static final String OTHER = "https://other.example.org/";

    private static final List<MappingGroup> GROUPS = List.of(
        group(1L, "genre", "dhir"),
        group(2L, "license", "dhir"),
        group(3L, "genre oii", "oii"),
        group(4L, "unused", "dhil"),
        group(5L, "genre other", "other"));

    private MyCoReAuthenticationProvider provider;

    @BeforeEach
    public void setUp() {
        Map<String, MyCoReTargetConfiguration> targets = new LinkedHashMap<>();
        targets.put("dhir", target(REPPER));
        targets.put("dhil", target(REPPER));
        // same instance, written without trailing slash and in upper case
        targets.put("oii", target("HTTPS://repper.example.org"));
        targets.put("other", target(OTHER));

        Map<String, ImportJobConfiguration> jobs = new LinkedHashMap<>();
        jobs.put("dhirJob", job("zenodo-dhir", "dhir", Map.of("genre", "genre", "license", "license")));
        // dhil shares the groups of dhir
        jobs.put("dhilJob", job("zenodo-dhil", "dhil", Map.of("genre", "genre", "status", "imported")));
        jobs.put("oiiJob", job("zenodo-oii", "oii", Map.of("genre", "genre oii")));
        jobs.put("otherJob", job("zenodo-other", "other", Map.of("genre", "genre other")));

        ImporterConfiguration configuration = new ImporterConfiguration();
        configuration.setTargets(targets);
        configuration.setJobs(jobs);

        MappingService mappingService = new MappingService() {
            @Override
            public List<MappingGroup> getGroupsByTarget(String target) {
                return GROUPS.stream().filter(group -> target.equals(group.getTarget())).toList();
            }

            @Override
            public List<MappingGroup> getGroupsByNames(Collection<String> names) {
                return GROUPS.stream().filter(group -> names.contains(group.getName())).toList();
            }
        };

        provider = new MyCoReAuthenticationProvider();
        ReflectionTestUtils.setField(provider, "configuration", configuration);
        ReflectionTestUtils.setField(provider, "mappingService", mappingService);
    }

    @Test
    public void importerGetsAllTargetsOfSameInstance() {
        assertEquals(Set.of("dhir", "dhil", "oii"), realms("dhil", "importer", "importer"));
    }

    @Test
    public void adminGetsAllTargetsOfSameInstance() {
        assertEquals(Set.of("dhir", "dhil", "oii"), realms("dhir", "importer", "admin"));
        assertEquals(Set.of("dhir", "dhil", "oii"), realms("dhir", "mapper", "admin"));
    }

    @Test
    public void targetRolesGrantTargetsIndependentOfLoginRealm() {
        assertEquals(Set.of("dhil", "oii"), realms("dhil", "importer", "importer-dhil", "importer-oii"));
        assertEquals(Set.of("dhil", "oii"), realms("oii", "importer", "importer-dhil", "importer-oii"));
        assertEquals(Set.of("dhil", "oii"), realms("dhir", "importer", "importer-dhil", "importer-oii"));
    }

    @Test
    public void rolesDoNotGrantTargetsOfOtherInstances() {
        assertEquals(Set.of("other"), realms("other", "importer", "importer"));
        assertTrue(realms("other", "importer", "importer-dhil").isEmpty());
        assertTrue(realms("dhil", "importer", "importer-other").isEmpty());
        assertTrue(realms("other", "mapper", "mapper-dhil").isEmpty());
        assertTrue(realms("dhil", "mapper", "mapper-other").isEmpty());
    }

    @Test
    public void rolesDoNotMix() {
        assertTrue(realms("dhil", "mapper", "importer", "importer-dhil").isEmpty());
        assertTrue(realms("dhil", "importer", "mapper", "mapper-dhil").isEmpty());
    }

    @Test
    public void unknownRolesGrantNothing() {
        assertTrue(realms("dhil", "importer", "submitter", "importer-unknown").isEmpty());
        assertTrue(provider.getAccessibleRealms("dhil", null, "importer").isEmpty());
        assertTrue(realms("unknown", "importer", "importer").isEmpty());
        assertTrue(provider.createAuthorities("dhil", List.of("submitter")).isEmpty());
    }

    @Test
    public void createsAuthoritiesForAllAccessibleTargets() {
        Set<String> authorities = authorities("oii", "importer-dhir", "importer-oii");

        assertTrue(authorities.containsAll(Set.of("source", "target", "job", "mapping",
            "target-dhir", "job-dhir", "job-dhirJob", "source-zenodo-dhir", "mapping-1", "mapping-2",
            "target-oii", "job-oii", "job-oiiJob", "source-zenodo-oii", "mapping-3")));
        assertFalse(authorities.contains("target-dhil"));
        assertFalse(authorities.contains("job-dhilJob"));
        assertFalse(authorities.contains("mapping-4"));
        assertFalse(authorities.contains("target-other"));
        assertFalse(authorities.contains("mapping-admin"));
    }

    @Test
    public void mapperGetsOnlyMappingGroupsUsedByJobsOfTarget() {
        // the job of dhil uses the group genre, which belongs to dhir
        assertEquals(Set.of("mapping", "mapping-1"), authorities("oii", "mapper-dhil"));
        assertEquals(Set.of("mapping", "mapping-1", "mapping-3"), authorities("dhil", "mapper-dhil", "mapper-oii"));
    }

    @Test
    public void mapperGetsMappingGroupsUsedByJobsOfAllTargetsOfInstance() {
        assertEquals(Set.of("mapping", "mapping-1", "mapping-2", "mapping-3"), authorities("dhir", "mapper"));
        assertEquals(Set.of("mapping", "mapping-5"), authorities("other", "mapper"));
    }

    @Test
    public void onlyAdminGetsMappingAdmin() {
        assertTrue(authorities("dhir", "admin").contains("mapping-admin"));
        assertFalse(authorities("dhir", "importer").contains("mapping-admin"));
        assertFalse(authorities("dhir", "mapper").contains("mapping-admin"));
    }

    private Set<String> realms(String loginRealm, String role, String... roles) {
        return provider.getAccessibleRealms(loginRealm, List.of(roles), role);
    }

    private Set<String> authorities(String loginRealm, String... roles) {
        return provider.createAuthorities(loginRealm, List.of(roles)).stream()
            .map(GrantedAuthority::getAuthority)
            .collect(Collectors.toSet());
    }

    private static MyCoReTargetConfiguration target(String url) {
        MyCoReTargetConfiguration target = new MyCoReTargetConfiguration();
        target.setUrl(url);
        return target;
    }

    private static ImportJobConfiguration job(String source, String target, Map<String, String> importerConfig) {
        ImportJobConfiguration job = new ImportJobConfiguration();
        job.setSourceConfigId(source);
        job.setTargetConfigId(target);
        job.setImporterConfig(importerConfig);
        return job;
    }

    private static MappingGroup group(Long id, String name, String target) {
        MappingGroup group = new MappingGroup();
        group.setId(id);
        group.setName(name);
        group.setTarget(target);
        return group;
    }
}
