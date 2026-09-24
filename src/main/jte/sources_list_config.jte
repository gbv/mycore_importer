@import java.util.Map
@import de.vzg.oai_importer.foreign.Configuration
@import static de.vzg.oai_importer.view.UrlHelper.url

@param Map<String,Configuration> sources

@template.layout.page(heading = "Quellen",
lead = "Datenbanken, aus denen Metadaten geholt werden.", content = @`
    <div class="tablewrap">
        <table class="data">
            <thead>
            <tr>
                <th scope="col">Name</th>
                <th scope="col">URL</th>
                <th scope="col">Aktion</th>
            </tr>
            </thead>
            <tbody>
            @if(sources == null || sources.isEmpty())
                <tr>
                    <td class="empty" colspan="3">Für Ihre Anwendung sind keine Quellen eingerichtet.</td>
                </tr>
            @else
                @for(Map.Entry<String,Configuration> source : sources.entrySet())
                    <tr>
                        <td class="identifier">${source.getKey()}</td>
                        <td>${source.getValue().getUrl()}</td>
                        <td>
                            <div class="rowactions">
                                <a href="${url("/sources/" + source.getKey() + "/")}">Anzeigen</a>
                                <a href="${url("/sources/" + source.getKey() + "/update")}">Aktualisieren</a>
                            </div>
                        </td>
                    </tr>
                @endfor
            @endif
            </tbody>
        </table>
    </div>
`)
