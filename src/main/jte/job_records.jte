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
    <button class="btn btn-secondary" type="submit" form="import-selected" id="import-selected-button" disabled>
        Markierte importieren
    </button>
    <a class="btn btn-primary" href="${url("/jobs/" + jobID + "/import")}">Import starten</a>
`,
content = @`
    <form id="import-selected" action="${url("/jobs/" + jobID + "/importSelected")}" method="post"></form>
    <div class="tablewrap">
        <table class="data">
            <thead>
            <tr>
                <th scope="col" class="shrink">
                    <input class="form-check-input" type="checkbox" id="import-select-all"
                           aria-label="Alle Datensätze dieser Seite markieren">
                </th>
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
                    <td class="empty" colspan="6">Nichts zu importieren. Alle Datensätze der Quelle sind bereits im
                        Ziel.</td>
                </tr>
            @endif
            @for(ForeignEntity record : records.getContent())
                <tr>
                    <td class="shrink">
                        <input class="form-check-input import-select" type="checkbox" form="import-selected"
                               name="recordID" value="${record.getForeignId()}"
                               aria-label="${record.getForeignId()} markieren">
                    </td>
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
    <script>
        (function () {
            const selectAll = document.getElementById("import-select-all");
            const button = document.getElementById("import-selected-button");
            const boxes = Array.from(document.querySelectorAll(".import-select"));

            function refresh() {
                const selected = boxes.filter(box => box.checked).length;
                button.disabled = selected === 0;
                button.textContent = selected === 0 ? "Markierte importieren" : selected + " markierte importieren";
                selectAll.checked = selected > 0 && selected === boxes.length;
                selectAll.indeterminate = selected > 0 && selected < boxes.length;
            }

            selectAll.disabled = boxes.length === 0;
            selectAll.addEventListener("change", () => {
                boxes.forEach(box => box.checked = selectAll.checked);
                refresh();
            });
            boxes.forEach(box => box.addEventListener("change", refresh));
            document.getElementById("import-selected").addEventListener("submit", () => {
                button.disabled = true;
                button.textContent = "Import läuft …";
            });
            refresh();
        })();
    </script>
`)
