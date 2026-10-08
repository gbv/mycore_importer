@import java.util.List
@import java.util.Map
@import org.springframework.data.domain.Page
@import de.vzg.oai_importer.foreign.jpa.ForeignEntity
@import static de.vzg.oai_importer.view.UrlHelper.url

@param Page<Map.Entry<ForeignEntity,List<String>>> records
@param String jobID

@template.layout.page(heading = "Neue Dateien prüfen",
lead = "Diese Dateien würden zu bereits importierten Dokumenten des Vorgangs " + jobID + " hinzukommen.",
actions = @`
    <a class="back" href="${url("/jobs/" + jobID + "/update/")}">Zurück</a>
    <a class="btn btn-secondary" href="${url("/jobs/" + jobID + "/testMapping?update=true")}">Zuordnungen prüfen</a>
    <a class="btn btn-primary" href="${url("/jobs/" + jobID + "/update/update")}">Alle aktualisieren</a>
`,
content = @`
    @template.tag.file_check_table(records = records, jobID = jobID, actionPath = "/update/", actionLabel = "Update")
    @template.tag.pagination(records = records, baseUrl = "/jobs/" + jobID + "/update/fileCheck")
`)
