package de.vzg.oai_importer.foreign.jpa;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.time.OffsetDateTime;

@Entity()
@Table(name = "foreign_entities",
    uniqueConstraints = @UniqueConstraint(columnNames = { "config_id", "foreign_id" }))
public class ForeignEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "config_id", length = 100, nullable = false)
    private String configId;

    @Column(name = "foreign_id", length = 100, nullable = false)
    private String foreignId;

    @Column(length = 512000, nullable = false)
    private String metadata;

    @Column(nullable = false)
    private OffsetDateTime datestamp;

    @Column(nullable = false)
    private Boolean isDeleted;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getConfigId() {
        return configId;
    }

    public void setConfigId(String oaiConfigId) {
        this.configId = oaiConfigId;
    }

    public String getForeignId() {
        return foreignId;
    }

    public void setForeignId(String recordId) {
        this.foreignId = recordId;
    }

    public String getMetadata() {
        return metadata;
    }

    public void setMetadata(String metadata) {
        this.metadata = metadata;
    }

    public OffsetDateTime getDatestamp() {
        return datestamp;
    }

    public void setDatestamp(OffsetDateTime datestamp) {
        this.datestamp = datestamp;
    }

    public Boolean getDeleted() {
        return isDeleted;
    }

    public void setDeleted(Boolean deleted) {
        isDeleted = deleted;
    }
}
