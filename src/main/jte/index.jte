@template.layout.page(heading = "Hilfe zur Anwendung",
lead = "Der Importer holt Metadaten aus Quellen und schreibt sie in MyCoRe Anwendungen.",
width = "reading",
content = @`
    <div class="panel panel--pad prose">
        <h2>Login</h2>
        <p>Der Login erfolgt durch Ergänzung des Nutzernamens mit einem anwendungsspezifischen Kürzel, wie zum
            Beispiel 'administrator@lokal'. Welches Kürzel für welche Anwendung zu verwenden ist, wird vom
            Administrator mitgeteilt.</p>

        <h2>Quellen</h2>
        <p>Quellen sind Datenbanken, aus denen Informationen bezogen werden. Angezeigt werden nur Quellen, die über
            einen Vorgang mit der angemeldeten Anwendung verknüpft sind. Durch Klicken auf 'Anzeigen' können
            Details zu den Quelleneinträgen eingesehen werden. 'Aktualisieren' bewirkt das
            Einlesen neuer Daten aus der Quelle in die Datenbank.</p>

        <h2>Ziele</h2>
        <p>Ziele beziehen sich auf MyCoRe Anwendungen, in die Daten geschrieben werden. Angezeigt wird nur
            das Ziel der angemeldeten Anwendung. 'Anzeigen' ermöglicht das
            Betrachten von Daten, die bereits in die Ziele importiert wurden. Die Datenaktualisierung erfolgt durch
            Klicken auf 'Aktualisieren', was das Einlesen der Daten aus dem Ziel in die Datenbank zur Folge hat.
            Sichtbarkeit und Aktualisierung von Einträgen setzt voraus, dass das Nutzerkonto zur entsprechenden
            Zielanwendung gehört.</p>

        <h2>Vorgänge</h2>
        <p>Vorgänge sind Verknüpfungen zwischen Quellen und Zielen. Angezeigt werden nur Vorgänge, deren Ziel
            die angemeldete Anwendung ist. 'Anzeigen' ermöglicht das Betrachten von
            Einträgen, die noch nicht in das Ziel importiert wurden. Diese Einträge sind sichtbar, wenn das
            Nutzerkonto zur Zielanwendung gehört. 'Zuordnung Prüfen' erlaubt die Überprüfung der Importierbarkeit
            von Daten und zeigt erforderliche Zuweisungen an. 'Import starten' initiiert den Import der Daten, wobei
            auch einzelne Einträge importiert werden können.</p>

        <h2>Zuordnung</h2>
        <p>Zuordnungen sind Verbindungen zwischen Klassifikationen aus Quellen und Zielen, wie beispielsweise
            'Zenodo Typ: Interview' und 'MyCoRe Genre: interview'. Diese
            werden in Gruppen nach Quell- und Zielklassifikationen organisiert. Angezeigt werden nur Gruppen,
            deren Ziel die angemeldete Anwendung ist. Bearbeitungen von Zuordnungsgruppen
            sind ausschließlich durch den
            Administrator möglich.</p>
        <p>
            Um vorhandene und fehlende Zuordnungen zu identifizieren, klicken Sie neben der entsprechenden Gruppe
            auf 'Anzeigen'.
            Die Zuordnungen werden in einer Tabelle dargestellt, wobei fehlende Zuordnungen durch einen fehlenden
            Wert in der Spalte 'Zu' gekennzeichnet sind.
        </p>
        <p>
            Eine Zuordnung kann durch Klicken auf 'Bearbeiten' geändert werden. Die Zuordnung wird in einem Formular
            dargestellt, in dem die Werte der Quell- und Zielklassifikationen geändert werden können.
            Einzelne Zuordnungen können durch alle Nutzer bearbeitet werden, sofern die Zielklassifikation aus der
            gleichen Anwendung stammt wie das Nutzerkonto.
        </p>
    </div>
`)
