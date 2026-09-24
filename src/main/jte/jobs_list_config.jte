@import java.util.Map
@import de.vzg.oai_importer.ImportJobConfiguration
@import static de.vzg.oai_importer.view.UrlHelper.url

@param Map<String,ImportJobConfiguration> jobs

@template.layout.page(heading = "Vorgänge",
lead = "Ein Vorgang verknüpft eine Quelle mit einer MyCoRe Anwendung.", content = @`
    <div class="tablewrap">
        <table class="data">
            <thead>
            <tr>
                <th scope="col">Vorgang</th>
                <th scope="col">Quelle</th>
                <th scope="col">Ziel</th>
                <th scope="col">Importer</th>
                <th scope="col">Aktion</th>
            </tr>
            </thead>
            <tbody>
            @if(jobs == null || jobs.isEmpty())
                <tr>
                    <td class="empty" colspan="5">Für Ihre Anwendung sind keine Vorgänge eingerichtet.</td>
                </tr>
            @else
                @for(Map.Entry<String,ImportJobConfiguration> job : jobs.entrySet())
                    <tr>
                        <td class="identifier">${job.getKey()}</td>
                        <td>${job.getValue().getSourceConfigId()}</td>
                        <td>${job.getValue().getTargetConfigId()}</td>
                        <td class="identifier">${job.getValue().getImporter()}</td>
                        <td>
                            <div class="rowactions">
                                <a href="${url("/jobs/" + job.getKey() + "/")}">Neue Dokumente</a>
                                <a href="${url("/jobs/" + job.getKey() + "/update/")}">Importierte Dokumente</a>
                            </div>
                        </td>
                    </tr>
                @endfor
            @endif
            </tbody>
        </table>
    </div>
`)
