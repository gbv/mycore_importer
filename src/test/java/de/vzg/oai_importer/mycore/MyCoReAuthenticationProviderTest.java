package de.vzg.oai_importer.mycore;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

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
        jobs.put("dhirJob", job("zenodo-dhir", "dhir"));
        jobs.put("dhilJob", job("zenodo-dhil", "dhil"));
        jobs.put("oiiJob", job("zenodo-oii", "oii"));
        jobs.put("otherJob", job("zenodo-other", "other"));

        ImporterConfiguration configuration = new ImporterConfiguration();
        configuration.setTargets(targets);
        configuration.setJobs(jobs);

        MappingService mappingService = new MappingService() {
            @Override
            public List<MappingGroup> getGroupsByTarget(String target) {
                return "dhir".equals(target) ? List.of(group(7L)) : List.of();
            }
        };

        provider = new MyCoReAuthenticationProvider();
        ReflectionTestUtils.setField(provider, "configuration", configuration);
        ReflectionTestUtils.setField(provider, "mappingService", mappingService);
    }

    @Test
    public void importerGetsAllTargetsOfSameInstance() {
        assertEquals(Set.of("dhir", "dhil", "oii"), provider.getAccessibleRealms("dhil", List.of("importer")));
    }

    @Test
    public void adminGetsAllTargetsOfSameInstance() {
        assertEquals(Set.of("dhir", "dhil", "oii"), provider.getAccessibleRealms("dhir", List.of("admin")));
    }

    @Test
    public void targetRolesGrantTargetsIndependentOfLoginRealm() {
        List<String> roles = List.of("importer-dhil", "importer-oii");
        assertEquals(Set.of("dhil", "oii"), provider.getAccessibleRealms("dhil", roles));
        assertEquals(Set.of("dhil", "oii"), provider.getAccessibleRealms("oii", roles));
        assertEquals(Set.of("dhil", "oii"), provider.getAccessibleRealms("dhir", roles));
    }

    @Test
    public void rolesDoNotGrantTargetsOfOtherInstances() {
        assertEquals(Set.of("other"), provider.getAccessibleRealms("other", List.of("importer")));
        assertTrue(provider.getAccessibleRealms("other", List.of("importer-dhil")).isEmpty());
        assertTrue(provider.getAccessibleRealms("dhil", List.of("importer-other")).isEmpty());
    }

    @Test
    public void unknownRolesGrantNothing() {
        assertTrue(provider.getAccessibleRealms("dhil", List.of("submitter", "importer-unknown")).isEmpty());
        assertTrue(provider.getAccessibleRealms("dhil", null).isEmpty());
        assertTrue(provider.getAccessibleRealms("unknown", List.of("importer")).isEmpty());
        assertTrue(provider.createAuthorities("dhil", List.of("submitter")).isEmpty());
    }

    @Test
    public void createsAuthoritiesForAllAccessibleTargets() {
        Set<String> authorities = names(provider.createAuthorities("oii", List.of("importer-dhir", "importer-oii")));

        assertTrue(authorities.containsAll(Set.of("source", "target", "job", "mapping",
            "target-dhir", "job-dhir", "job-dhirJob", "source-zenodo-dhir", "mapping-7",
            "target-oii", "job-oii", "job-oiiJob", "source-zenodo-oii")));
        assertFalse(authorities.contains("target-dhil"));
        assertFalse(authorities.contains("job-dhilJob"));
        assertFalse(authorities.contains("target-other"));
        assertFalse(authorities.contains("mapping-admin"));
    }

    @Test
    public void onlyAdminGetsMappingAdmin() {
        assertTrue(names(provider.createAuthorities("dhir", List.of("admin"))).contains("mapping-admin"));
        assertFalse(names(provider.createAuthorities("dhir", List.of("importer"))).contains("mapping-admin"));
    }

    private static Set<String> names(List<GrantedAuthority> authorities) {
        return authorities.stream().map(GrantedAuthority::getAuthority).collect(Collectors.toSet());
    }

    private static MyCoReTargetConfiguration target(String url) {
        MyCoReTargetConfiguration target = new MyCoReTargetConfiguration();
        target.setUrl(url);
        return target;
    }

    private static ImportJobConfiguration job(String source, String target) {
        ImportJobConfiguration job = new ImportJobConfiguration();
        job.setSourceConfigId(source);
        job.setTargetConfigId(target);
        return job;
    }

    private static MappingGroup group(Long id) {
        MappingGroup group = new MappingGroup();
        group.setId(id);
        return group;
    }
}
