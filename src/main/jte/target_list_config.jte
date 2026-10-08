@import java.util.Map
@import de.vzg.oai_importer.mycore.MyCoReTargetConfiguration
@import static de.vzg.oai_importer.view.UrlHelper.url

@param Map<String,MyCoReTargetConfiguration> targets

@template.layout.page(heading = "Ziele",
lead = "MyCoRe Anwendungen, in die Daten geschrieben werden.", content = @`
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
            @if(targets == null || targets.isEmpty())
                <tr>
                    <td class="empty" colspan="3">Für Ihre Anwendung sind keine Ziele eingerichtet.</td>
                </tr>
            @else
                @for(Map.Entry<String,MyCoReTargetConfiguration> target : targets.entrySet())
                    <tr>
                        <td class="identifier">${target.getKey()}</td>
                        <td>${target.getValue().getUrl()}</td>
                        <td>
                            <div class="rowactions">
                                <a href="${url("/targets/" + target.getKey() + "/")}">Anzeigen</a>
                                <a href="${url("/targets/" + target.getKey() + "/update")}">Aktualisieren</a>
                            </div>
                        </td>
                    </tr>
                @endfor
            @endif
            </tbody>
        </table>
    </div>
`)
