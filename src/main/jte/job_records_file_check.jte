@import java.util.List
@import java.util.Map
@import org.springframework.data.domain.Page
@import de.vzg.oai_importer.foreign.jpa.ForeignEntity
@import static de.vzg.oai_importer.view.UrlHelper.url

@param Page<Map.Entry<ForeignEntity,List<String>>> records
@param String jobID

@template.layout.page(heading = "Dateien prüfen",
lead = "Diese Dateien würden beim Import des Vorgangs " + jobID + " mitgenommen.",
actions = @`
    <a class="back" href="${url("/jobs/" + jobID + "/")}">Zurück</a>
    <a class="btn btn-secondary" href="${url("/jobs/" + jobID + "/testMapping?update=false")}">Zuordnungen prüfen</a>
    <a class="btn btn-primary" href="${url("/jobs/" + jobID + "/import")}">Import starten</a>
`,
content = @`
    @template.tag.file_check_table(records = records, jobID = jobID, actionPath = "/import/", actionLabel = "Import")
    @template.tag.pagination(records = records, baseUrl = "/jobs/" + jobID + "/fileCheck")
`)
