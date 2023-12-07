package de.vzg.oai_importer.foreign.jpa;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface ForeignEntityRepository extends JpaRepository<ForeignEntity, Long> {

    ForeignEntity findFirstByConfigIdOrderByDatestampDesc(String config);

    List<ForeignEntity> findAllByConfigIdAndIsDeletedOrderByDatestampDesc(String config, Boolean isDeleted);

    ForeignEntity findFirstByConfigIdAndForeignId(String config, String recordId);

    @Query("SELECT r FROM ForeignEntity r WHERE r.configId = ?1 AND r.isDeleted = false AND r.foreignId NOT IN " +
        "(SELECT m.importID FROM MyCoReObjectInfo m where m.importURL = ?2 AND m.repository = ?3)")
    List<ForeignEntity> findImportableEntities(String oaiConfig, String oaiSource, String targetRepository);

    @Query("SELECT fe, oi FROM ForeignEntity fe, MyCoReObjectInfo oi WHERE fe.configId = ?1 AND fe.isDeleted = false AND fe.foreignId = oi.importID AND oi.importURL = ?2 AND oi.repository = ?3 order by fe.datestamp desc")
    List<Object[]> findUpdateableEntities(String oaiConfig, String oaiSource, String targetRepository);
}
