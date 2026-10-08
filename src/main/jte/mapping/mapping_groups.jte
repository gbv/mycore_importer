@import org.springframework.data.domain.Page
@import de.vzg.oai_importer.mapping.jpa.MappingGroup
@import static de.vzg.oai_importer.view.AuthHelper.hasAnyAuthority
@import static de.vzg.oai_importer.view.UrlHelper.url

@param Page<MappingGroup> groups

@template.layout.page(heading = "Zuordnungsgruppen",
lead = "Jede Gruppe verbindet eine Klassifikation der Quelle mit einer Klassifikation im Ziel.",
actions = @`
    @if(hasAnyAuthority("mapping-admin"))
        <a class="btn btn-primary" href="${url("/mapping/groups/add/")}">Neue Gruppe</a>
    @endif
`,
content = @`
    <div class="tablewrap">
        <table class="data">
            <thead>
            <tr>
                <th scope="col">Name</th>
                <th scope="col">Beschreibung</th>
                <th scope="col">MyCoRe Anwendung</th>
                <th scope="col">MyCoRe Klassifikation</th>
                <th scope="col">Aktion</th>
            </tr>
            </thead>
            <tbody>
            @if(groups.isEmpty())
                <tr>
                    <td class="empty" colspan="5">Für Ihre Anwendung sind noch keine Zuordnungsgruppen angelegt.</td>
                </tr>
            @endif
            @for(MappingGroup mappingGroup : groups.getContent())
                <tr>
                    <td>${mappingGroup.getName()}</td>
                    <td>${mappingGroup.getDescription()}</td>
                    <td class="identifier">${mappingGroup.getTarget()}</td>
                    <td class="identifier">${mappingGroup.getClassId()}</td>
                    <td>
                        <div class="rowactions">
                            <a href="${url("/mapping/groups/" + mappingGroup.getId() + "/")}">Anzeigen</a>
                            <a href="${url("/mapping/groups/" + mappingGroup.getId() + "/edit/")}">Bearbeiten</a>
                        </div>
                    </td>
                </tr>
            @endfor
            </tbody>
        </table>
    </div>
    @template.tag.pagination(records = groups, baseUrl = "/mapping/groups/")
`)
