package de.vzg.oai_importer.mapping;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.test.context.TestExecutionListeners;
import org.springframework.test.context.support.DependencyInjectionTestExecutionListener;
import org.springframework.test.context.transaction.TransactionalTestExecutionListener;

import de.vzg.oai_importer.mapping.jpa.MappingGroup;
import de.vzg.oai_importer.mapping.jpa.MappingGroupRepository;

@DataJpaTest
// without the Mockito listeners of Spring Boot, Mockito of this Spring Boot version does not start on Java 25
@TestExecutionListeners(
    listeners = { DependencyInjectionTestExecutionListener.class, TransactionalTestExecutionListener.class },
    mergeMode = TestExecutionListeners.MergeMode.REPLACE_DEFAULTS)
public class MappingGroupRepositoryTest {

    /**
     * The project has two applications, so the test needs its own configuration.
     */
    @SpringBootConfiguration
    @EntityScan(basePackageClasses = MappingGroup.class)
    @EnableJpaRepositories(basePackageClasses = MappingGroupRepository.class)
    static class Config {
    }

    @Autowired
    private MappingGroupRepository repository;

    private MappingGroup genre;

    private MappingGroup license;

    @BeforeEach
    public void setUp() {
        genre = repository.save(group("genre", "dhir"));
        license = repository.save(group("license", "dhil"));
        repository.save(group("other", "other"));
    }

    @Test
    public void findsGroupsByTargetOrId() {
        assertEquals(Set.of("genre", "license"),
            names(repository.findByTargetInOrIdIn(Set.of("dhir"), Set.of(license.getId()), Pageable.ofSize(10))
                .getContent()));
    }

    @Test
    public void handlesEmptyCollections() {
        assertEquals(Set.of("genre"),
            names(repository.findByTargetInOrIdIn(Set.of(), Set.of(genre.getId()), Pageable.ofSize(10)).getContent()));
        assertEquals(Set.of("license"),
            names(repository.findByTargetInOrIdIn(Set.of("dhil"), Set.of(), Pageable.ofSize(10)).getContent()));
        assertEquals(Set.of(), names(repository.findByTargetInOrIdIn(Set.of(), Set.of(), Pageable.ofSize(10))
            .getContent()));
    }

    @Test
    public void findsGroupsByNames() {
        assertEquals(Set.of("genre", "license"),
            names(repository.findByNameIn(Set.of("genre", "license", "imported"))));
    }

    private static Set<String> names(List<MappingGroup> groups) {
        return Set.copyOf(groups.stream().map(MappingGroup::getName).toList());
    }

    private static MappingGroup group(String name, String target) {
        MappingGroup group = new MappingGroup();
        group.setName(name);
        group.setDescription(name);
        group.setTarget(target);
        return group;
    }
}
