package de.vzg.oai_importer.mapping.jpa;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "mappings",
        uniqueConstraints = {
                @jakarta.persistence.UniqueConstraint(columnNames = {"mfrom", "mapping_group_id"})

        })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Mapping {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // add mappingGroup
    @ManyToOne
    private MappingGroup mappingGroup;

    @Column(name = "mfrom", length = 64, nullable = false)
    private String from;

    @Column(name = "mto", length = 64)
    private String to;

}
