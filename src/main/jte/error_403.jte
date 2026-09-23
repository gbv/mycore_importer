@import static de.vzg.oai_importer.view.UrlHelper.url

@template.layout.page(heading = "Keine Berechtigung",
lead = "Ihr Nutzerkonto hat keinen Zugriff auf diese Seite.", content = @`
    <a class="btn btn-secondary" href="${url("/")}">Zur Startseite</a>
`)
