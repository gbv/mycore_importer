@import java.util.List
@import java.util.Map
@import org.springframework.data.domain.Page
@import de.vzg.oai_importer.foreign.jpa.ForeignEntity
@import static de.vzg.oai_importer.view.UrlHelper.url
@import static de.vzg.oai_importer.view.FormatHelper.dateTime
@import static de.vzg.oai_importer.view.FormatHelper.yesNo

@param Page<Map.Entry<ForeignEntity,List<String>>> records
@param String jobID
@param String actionPath
@param String actionLabel

<div class="tablewrap">
    <table class="data">
        <thead>
        <tr>
            <th scope="col">Id</th>
            <th scope="col">Metadaten</th>
            <th scope="col">Dateien</th>
            <th scope="col">Datum</th>
            <th scope="col" class="shrink">Gelöscht</th>
            <th scope="col">Aktion</th>
        </tr>
        </thead>
        <tbody>
        @if(records.isEmpty())
            <tr>
                <td class="empty" colspan="6">Keine Datensätze zu prüfen.</td>
            </tr>
        @endif
        @for(Map.Entry<ForeignEntity,List<String>> record : records.getContent())
            !{var entity = record.getKey();}
            <tr>
                <td class="identifier">${entity.getForeignId()}</td>
                <td>
                    @template.tag.metadata_modal(id = String.valueOf(entity.getId()),
                        label = entity.getForeignId(), metadata = entity.getMetadata())
                </td>
                <td>
                    @template.tag.files_modal(id = String.valueOf(entity.getId()),
                        label = entity.getForeignId(), files = record.getValue())
                </td>
                <td class="tabular">${dateTime(entity.getDatestamp())}</td>
                <td class="shrink">${yesNo(entity.getDeleted())}</td>
                <td>
                    <div class="rowactions">
                        <a href="${url("/jobs/" + jobID + "/test/" + entity.getForeignId())}">Test</a>
                        <a href="${url("/jobs/" + jobID + actionPath + entity.getForeignId())}">${actionLabel}</a>
                    </div>
                </td>
            </tr>
        @endfor
        </tbody>
    </table>
</div>
