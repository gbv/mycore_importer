@import org.springframework.data.domain.Page
@import de.vzg.oai_importer.mapping.jpa.Mapping
@import de.vzg.oai_importer.mapping.jpa.MappingGroup
@import static de.vzg.oai_importer.view.UrlHelper.url

@param Page<Mapping> mappings
@param MappingGroup group

@template.layout.page(heading = group.getName(), lead = group.getDescription(),
actions = @`
    <a class="back" href="${url("/mapping/groups/")}">Zurück</a>
    <a class="btn btn-primary" href="${url("/mapping/groups/" + group.getId() + "/add/")}">Neue Zuordnung</a>
`,
content = @`
    <div class="tablewrap">
        <table class="data">
            <thead>
            <tr>
                <th scope="col">Von</th>
                <th scope="col">Zu</th>
                <th scope="col">Aktion</th>
            </tr>
            </thead>
            <tbody>
            @if(mappings.isEmpty())
                <tr>
                    <td class="empty" colspan="3">Noch keine Zuordnungen in dieser Gruppe.</td>
                </tr>
            @endif
            @for(Mapping mapping : mappings.getContent())
                <tr>
                    <td class="identifier">${mapping.getFrom()}</td>
                    <td>
                        @if(mapping.getTo() == null || mapping.getTo().isEmpty())
                            <span class="chip">Fehlt</span>
                        @else
                            <span class="identifier">${mapping.getTo()}</span>
                        @endif
                    </td>
                    <td>
                        !{var mappingUrl = "/mapping/groups/" + group.getId() + "/" + mapping.getId();}
                        <div class="rowactions">
                            <a href="${url(mappingUrl + "/edit/")}">Bearbeiten</a>
                            <a href="${url(mappingUrl + "/delete/")}">Löschen</a>
                        </div>
                    </td>
                </tr>
            @endfor
            </tbody>
        </table>
    </div>
`)
