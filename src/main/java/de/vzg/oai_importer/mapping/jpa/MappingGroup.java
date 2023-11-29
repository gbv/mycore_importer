package de.vzg.oai_importer.mapping.jpa;


import java.util.List;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "mapping_group", uniqueConstraints = { @UniqueConstraint(columnNames = { "name" }) })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class MappingGroup {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "name", length = 60, nullable = false)
    private String name;

    @Column(name = "description", length = 1000, nullable = false)
    private String description;

    @Column(name = "mtarget", length = 256, nullable = false)
    private String target;

    @Column(name = "class_id", length = 256, nullable = true)
    private String classId;

    @OneToMany(mappedBy="mappingGroup")
    private List<Mapping> mapping;

}
