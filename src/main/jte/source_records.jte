@import org.springframework.data.domain.Page
@import de.vzg.oai_importer.foreign.jpa.ForeignEntity
@import static de.vzg.oai_importer.view.UrlHelper.url
@import static de.vzg.oai_importer.view.FormatHelper.dateTime
@import static de.vzg.oai_importer.view.FormatHelper.yesNo
@import static de.vzg.oai_importer.view.FormatHelper.count

@param Page<ForeignEntity> records
@param String source

@template.layout.page(heading = "Quelle " + source,
lead = count(records.getTotalElements(), "Datensatz", "Datensätze") + " in der Datenbank.",
actions = @`
    <a class="back" href="${url("/sources/")}">Zurück</a>
    <a class="btn btn-primary" href="${url("/sources/" + source + "/update")}">Aktualisieren</a>
`,
content = @`
    <div class="tablewrap">
        <table class="data">
            <thead>
            <tr>
                <th scope="col">Id</th>
                <th scope="col">Metadaten</th>
                <th scope="col">Datum</th>
                <th scope="col" class="shrink">Gelöscht</th>
            </tr>
            </thead>
            <tbody>
            @if(records.isEmpty())
                <tr>
                    <td class="empty" colspan="4">Noch keine Datensätze. Über 'Aktualisieren' werden sie aus der
                        Quelle geholt.</td>
                </tr>
            @endif
            @for(ForeignEntity record : records.getContent())
                <tr>
                    <td class="identifier">${record.getForeignId()}</td>
                    <td>
                        @template.tag.metadata_modal(id = String.valueOf(record.getId()),
                            label = record.getForeignId(), metadata = record.getMetadata())
                    </td>
                    <td class="tabular">${dateTime(record.getDatestamp())}</td>
                    <td class="shrink">${yesNo(record.getDeleted())}</td>
                </tr>
            @endfor
            </tbody>
        </table>
    </div>
    @template.tag.pagination(records = records, baseUrl = "/sources/" + source + "/")
`)
