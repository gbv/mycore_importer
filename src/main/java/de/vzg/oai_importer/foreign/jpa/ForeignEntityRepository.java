package de.vzg.oai_importer.foreign.jpa;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.ListCrudRepository;
import org.springframework.data.repository.ListPagingAndSortingRepository;

public interface ForeignEntityRepository extends ListPagingAndSortingRepository<ForeignEntity, Long>, ListCrudRepository<ForeignEntity, Long> {

    ForeignEntity findFirstByConfigIdOrderByDatestampDesc(String config);

    Page<ForeignEntity> findAllByConfigIdAndIsDeletedOrderByDatestampDesc(String config, Boolean isDeleted, Pageable pageable);

    ForeignEntity findFirstByConfigIdAndForeignId(String config, String recordId);

    @Query("SELECT r FROM ForeignEntity r WHERE r.configId = ?1 AND r.isDeleted = false AND r.foreignId NOT IN " +
        "(SELECT m.importID FROM MyCoReObjectInfo m where m.importURL = ?2 AND m.repository = ?3)")
    Page<ForeignEntity> findImportableEntities(String oaiConfig, String oaiSource, String targetRepository, Pageable pageable);

    @Query("SELECT fe, oi FROM ForeignEntity fe, MyCoReObjectInfo oi WHERE fe.configId = ?1 AND fe.isDeleted = false AND fe.foreignId = oi.importID AND oi.importURL = ?2 AND oi.repository = ?3 order by fe.datestamp desc")
    List<Object[]> findUpdateableEntities(String oaiConfig, String oaiSource, String targetRepository);
}
