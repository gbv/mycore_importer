@param String result = null

@template.layout.page(heading = "Testergebnis",
lead = "So sähen die Metadaten nach der Umwandlung im Ziel aus.", content = @`
    @if(result != null)
        <pre class="metadata">${result}</pre>
    @else
        <div class="panel panel--pad empty">Kein Ergebnis.</div>
    @endif
`)
