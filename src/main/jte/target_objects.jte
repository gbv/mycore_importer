@import org.springframework.data.domain.Page
@import de.vzg.oai_importer.mycore.jpa.MyCoReObjectInfo
@import static de.vzg.oai_importer.view.UrlHelper.url
@import static de.vzg.oai_importer.view.FormatHelper.dateTime
@import static de.vzg.oai_importer.view.FormatHelper.count

@param Page<MyCoReObjectInfo> records
@param String target

@template.layout.page(heading = "Ziel " + target,
lead = count(records.getTotalElements(), "Objekt", "Objekte") + " in der Datenbank.",
actions = @`
    <a class="back" href="${url("/targets/")}">Zurück</a>
    <a class="btn btn-primary" href="${url("/targets/" + target + "/update")}">Aktualisieren</a>
`,
content = @`
    <div class="tablewrap">
        <table class="data">
            <thead>
            <tr>
                <th scope="col">MyCoRe-ID</th>
                <th scope="col">Parent</th>
                <th scope="col" class="shrink">Status</th>
                <th scope="col">Importquelle</th>
                <th scope="col">Import-ID</th>
                <th scope="col">Erstellt</th>
                <th scope="col">Geändert</th>
                <th scope="col">Ersteller</th>
            </tr>
            </thead>
            <tbody>
            @if(records.isEmpty())
                <tr>
                    <td class="empty" colspan="8">Noch keine Objekte. Über 'Aktualisieren' werden sie aus dem Ziel
                        geholt.</td>
                </tr>
            @endif
            @for(MyCoReObjectInfo object : records.getContent())
                <tr>
                    <td class="identifier">${object.getMycoreId()}</td>
                    <td class="identifier">${object.getParentMycoreId()}</td>
                    <td class="shrink">${object.getState()}</td>
                    <td>${object.getImportURL()}</td>
                    <td class="identifier">${object.getImportID()}</td>
                    <td class="tabular">${dateTime(object.getCreated())}</td>
                    <td class="tabular">${dateTime(object.getLastModified())}</td>
                    <td>${object.getCreatedBy()}</td>
                </tr>
            @endfor
            </tbody>
        </table>
    </div>
    @template.tag.pagination(records = records, baseUrl = "/targets/" + target + "/")
`)
