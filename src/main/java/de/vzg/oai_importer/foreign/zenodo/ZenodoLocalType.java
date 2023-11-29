package de.vzg.oai_importer.foreign.zenodo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Data
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ZenodoLocalType {
    private String id;
    private String subtype_name;
    private String type_name;
}
