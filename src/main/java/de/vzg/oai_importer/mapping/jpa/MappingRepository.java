package de.vzg.oai_importer.mapping.jpa;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.repository.ListCrudRepository;
import org.springframework.data.repository.PagingAndSortingRepository;

import java.util.Optional;

public interface MappingRepository extends PagingAndSortingRepository<Mapping, Long>, ListCrudRepository<Mapping, Long> {

    Page<Mapping> findByMappingGroup(MappingGroup mappingGroup, Pageable pageable);


    Optional<Mapping> findByMappingGroupAndFrom(MappingGroup group, String from);
}
