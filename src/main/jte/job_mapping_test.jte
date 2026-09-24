@import java.util.List
@import java.util.Map
@import de.vzg.oai_importer.foreign.jpa.ForeignEntity
@import de.vzg.oai_importer.mapping.jpa.Mapping
@import static de.vzg.oai_importer.view.UrlHelper.url
@import static de.vzg.oai_importer.view.FormatHelper.dateTime
@import static de.vzg.oai_importer.view.FormatHelper.yesNo

@param Map<ForeignEntity,List<Mapping>> records

@template.layout.page(heading = "Zuordnungen prüfen",
lead = "Diese Datensätze lassen sich noch nicht importieren, weil Zuordnungen fehlen.",
content = @`
    <div class="tablewrap">
        <table class="data">
            <thead>
            <tr>
                <th scope="col">Id</th>
                <th scope="col">Metadaten</th>
                <th scope="col">Datum</th>
                <th scope="col" class="shrink">Gelöscht</th>
                <th scope="col">Fehlende Zuordnung</th>
            </tr>
            </thead>
            <tbody>
            @if(records.isEmpty())
                <tr>
                    <td class="empty" colspan="5">Alle Zuordnungen sind vorhanden.</td>
                </tr>
            @endif
            @for(Map.Entry<ForeignEntity,List<Mapping>> record : records.entrySet())
                !{var entity = record.getKey();}
                <tr>
                    <td class="identifier">${entity.getForeignId()}</td>
                    <td>
                        @template.tag.metadata_modal(id = String.valueOf(entity.getId()),
                            label = entity.getForeignId(), metadata = entity.getMetadata())
                    </td>
                    <td class="tabular">${dateTime(entity.getDatestamp())}</td>
                    <td class="shrink">${yesNo(entity.getDeleted())}</td>
                    <td>
                        <div class="rowactions">
                            @for(Mapping mapping : record.getValue())
                                !{var editUrl = url("/mapping/groups/" + mapping.getMappingGroup().getId() + "/"
                                    + mapping.getId() + "/edit/");}
                                <a href="${editUrl}">${mapping.getMappingGroup().getName()}:
                                    ${mapping.getFrom()}</a>
                            @endfor
                        </div>
                    </td>
                </tr>
            @endfor
            </tbody>
        </table>
    </div>
`)
