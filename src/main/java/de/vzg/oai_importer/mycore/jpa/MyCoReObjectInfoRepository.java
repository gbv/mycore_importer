package de.vzg.oai_importer.mycore.jpa;

import java.util.List;

import org.springframework.data.repository.Repository;

public interface MyCoReObjectInfoRepository extends Repository<MyCoReObjectInfo, Long> {

    MyCoReObjectInfo findByMycoreIdAndRepository(String mycoreId, String repository);

    MyCoReObjectInfo findFirstByRepositoryOrderByCreatedDesc(String repository);

    MyCoReObjectInfo findFirstByRepositoryAndImportURLAndImportID(String repository, String importURL, String importID);

    List<MyCoReObjectInfo> findByRepository(String repository);

    void save(MyCoReObjectInfo objectInfo);

}
