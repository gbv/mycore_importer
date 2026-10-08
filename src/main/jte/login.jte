@import static de.vzg.oai_importer.view.UrlHelper.url

@param boolean error = false
@param boolean logout = false

@template.layout.page(heading = "Anmelden", width = "narrow", content = @`
    <div class="login">
        @if(error)
            <div class="notice notice--bad">Nutzername oder Passwort stimmt nicht.</div>
        @endif
        @if(logout)
            <div class="notice notice--ok">Sie sind abgemeldet.</div>
        @endif
        <form class="panel panel--pad" action="${url("/login")}" method="post">
            <div class="field">
                <label for="username">Nutzername</label>
                <input class="form-control" type="text" id="username" name="username" autocomplete="username"
                       autofocus>
            </div>
            <div class="field">
                <label for="password">Passwort</label>
                <input class="form-control" type="password" id="password" name="password"
                       autocomplete="current-password">
            </div>
            <button class="btn btn-primary" type="submit">Anmelden</button>
        </form>
    </div>
`)
