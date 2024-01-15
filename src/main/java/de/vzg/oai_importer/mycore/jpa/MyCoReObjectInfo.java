package de.vzg.oai_importer.mycore.jpa;

import java.time.OffsetDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;


@Entity
@Table(name = "mycore_object_info",
    uniqueConstraints = { @UniqueConstraint(columnNames = { "mycore_id", "repository" })
    })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class MyCoReObjectInfo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "mycore_id", length = 40, nullable = false)
    private String mycoreId;

    @Column(name = "parent_mycore_id", length = 40)
    private String parentMycoreId;

    @Column(name = "import_id", length = 20)
    private String importID; // saved in recordInfo

    @Column(name = "import_source", length = 1000)
    private String importURL; // saved in recordInfo

    @Column(name = "last_modified")
    private OffsetDateTime lastModified;

    @Column(name = "created")
    private OffsetDateTime created;

    @Column(name = "createdBy", length = 1000, nullable = false)
    private String createdBy;

    @Column(name = "repository", nullable = false)
    private String repository;

    @Column(name = "state", length = 255, nullable = false)
    private String state;

}
