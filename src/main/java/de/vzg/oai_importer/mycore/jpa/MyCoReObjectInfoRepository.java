package de.vzg.oai_importer.mycore.jpa;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.repository.ListCrudRepository;
import org.springframework.data.repository.ListPagingAndSortingRepository;

public interface MyCoReObjectInfoRepository extends ListPagingAndSortingRepository<MyCoReObjectInfo, Long>, ListCrudRepository<MyCoReObjectInfo, Long> {

    MyCoReObjectInfo findByMycoreIdAndRepository(String mycoreId, String repository);

    MyCoReObjectInfo findFirstByRepositoryOrderByCreatedDesc(String repository);

    MyCoReObjectInfo findFirstByRepositoryAndImportURLAndImportID(String repository, String importURL, String importID);

    Page<MyCoReObjectInfo> findByRepository(String repository, Pageable page);

}
