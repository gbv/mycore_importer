@import static de.vzg.oai_importer.view.UrlHelper.url

@template.layout.page(heading = "Seite nicht gefunden",
lead = "Diese Adresse gibt es in der Anwendung nicht.", content = @`
    <a class="btn btn-secondary" href="${url("/")}">Zur Startseite</a>
`)
