@import static de.vzg.oai_importer.view.AuthHelper.isFullyAuthenticated

@param String message = null

@template.layout.page(heading = "Es ist ein Fehler aufgetreten",
lead = message != null && isFullyAuthenticated() ? null : "Mehr wissen wir leider auch nicht.",
content = @`
    @if(message != null && isFullyAuthenticated())
        <div class="alert alert-danger" role="alert" style="white-space: pre-line">${message}</div>
    @endif
`)
