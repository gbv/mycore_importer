@import org.springframework.data.domain.Page
@import de.vzg.oai_importer.foreign.jpa.ForeignEntity
@import static de.vzg.oai_importer.view.UrlHelper.url
@import static de.vzg.oai_importer.view.FormatHelper.dateTime
@import static de.vzg.oai_importer.view.FormatHelper.yesNo
@import static de.vzg.oai_importer.view.FormatHelper.count

@param Page<ForeignEntity> records
@param String jobID

@template.layout.page(heading = "Neue Dokumente",
lead = "Vorgang " + jobID + ", " + count(records.getTotalElements(), "Datensatz", "Datensätze")
    + " noch nicht importiert.",
actions = @`
    <a class="back" href="${url("/jobs/")}">Zurück</a>
    <a class="btn btn-secondary" href="${url("/jobs/" + jobID + "/testMapping?update=false")}">Zuordnungen prüfen</a>
    <a class="btn btn-secondary" href="${url("/jobs/" + jobID + "/fileCheck")}">Dateien prüfen</a>
    <a class="btn btn-primary" href="${url("/jobs/" + jobID + "/import")}">Import starten</a>
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
                <th scope="col">Aktion</th>
            </tr>
            </thead>
            <tbody>
            @if(records.isEmpty())
                <tr>
                    <td class="empty" colspan="5">Nichts zu importieren. Alle Datensätze der Quelle sind bereits im
                        Ziel.</td>
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
                    <td>
                        <div class="rowactions">
                            <a href="${url("/jobs/" + jobID + "/test/" + record.getForeignId())}">Test</a>
                            <a href="${url("/jobs/" + jobID + "/import/" + record.getForeignId())}">Import</a>
                        </div>
                    </td>
                </tr>
            @endfor
            </tbody>
        </table>
    </div>
    @template.tag.pagination(records = records, baseUrl = "/jobs/" + jobID + "/")
`)
