@import org.springframework.data.domain.Page
@import de.vzg.oai_importer.ImporterService
@import de.vzg.oai_importer.foreign.jpa.ForeignEntity
@import de.vzg.oai_importer.mycore.jpa.MyCoReObjectInfo
@import static de.vzg.oai_importer.view.UrlHelper.url
@import static de.vzg.oai_importer.view.FormatHelper.dateTime
@import static de.vzg.oai_importer.view.FormatHelper.yesNo
@import static de.vzg.oai_importer.view.FormatHelper.count

@param Page<ImporterService.Pair<ForeignEntity,MyCoReObjectInfo>> records
@param String jobID
@param String success = null

@template.layout.page(heading = "Importierte Dokumente",
lead = "Vorgang " + jobID + ", " + count(records.getTotalElements(), "Datensatz", "Datensätze")
    + " bereits im Ziel.",
actions = @`
    <a class="back" href="${url("/jobs/")}">Zurück</a>
    <a class="btn btn-secondary" href="${url("/jobs/" + jobID + "/update/fileCheck")}">Neue Dateien prüfen</a>
    <a class="btn btn-secondary" href="${url("/jobs/" + jobID + "/testMapping?update=true")}">Zuordnungen prüfen</a>
    <a class="btn btn-primary" href="${url("/jobs/" + jobID + "/update/update")}">Alle aktualisieren</a>
`,
content = @`
    @if("true".equals(success))
        <div class="notice notice--ok">Import erfolgreich.</div>
    @endif
    @if("false".equals(success))
        <div class="notice notice--bad">Import fehlgeschlagen. Details stehen im Protokoll der Anwendung.</div>
    @endif
    <div class="tablewrap">
        <table class="data">
            <thead>
            <tr>
                <th scope="col">Id</th>
                <th scope="col">MyCoRe-ID</th>
                <th scope="col">Metadaten</th>
                <th scope="col">Datum</th>
                <th scope="col" class="shrink">Gelöscht</th>
                <th scope="col">Aktion</th>
            </tr>
            </thead>
            <tbody>
            @if(records.isEmpty())
                <tr>
                    <td class="empty" colspan="6">Noch nichts importiert.</td>
                </tr>
            @endif
            @for(ImporterService.Pair<ForeignEntity,MyCoReObjectInfo> record : records.getContent())
                <tr>
                    <td class="identifier">${record.first().getForeignId()}</td>
                    <td class="identifier">${record.second().getMycoreId()}</td>
                    <td>
                        @template.tag.metadata_modal(id = String.valueOf(record.first().getId()),
                            label = record.first().getForeignId(), metadata = record.first().getMetadata())
                    </td>
                    <td class="tabular">${dateTime(record.first().getDatestamp())}</td>
                    <td class="shrink">${yesNo(record.first().getDeleted())}</td>
                    <td>
                        <div class="rowactions">
                            <a href="${url("/jobs/" + jobID + "/test/" + record.first().getForeignId())}">Test</a>
                            <a href="${url("/jobs/" + jobID + "/update/" + record.first().getForeignId())}">Update</a>
                        </div>
                    </td>
                </tr>
            @endfor
            </tbody>
        </table>
    </div>
    @template.tag.pagination(records = records, baseUrl = "/jobs/" + jobID + "/update/")
`)
