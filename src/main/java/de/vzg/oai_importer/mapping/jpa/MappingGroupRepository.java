package de.vzg.oai_importer.mapping.jpa;

import java.util.List;
import java.util.Optional;

import org.springframework.data.repository.ListCrudRepository;
import org.springframework.data.repository.ListPagingAndSortingRepository;

public interface MappingGroupRepository extends ListPagingAndSortingRepository<MappingGroup, Long>, ListCrudRepository<MappingGroup, Long> {

    Optional<MappingGroup> findByName(String name);

    List<MappingGroup> findByTarget(String target);
}
