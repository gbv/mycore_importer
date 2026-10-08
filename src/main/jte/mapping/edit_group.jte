@import java.util.Collection
@import de.vzg.oai_importer.mapping.jpa.MappingGroup
@import static de.vzg.oai_importer.view.UrlHelper.url

@param Collection<String> targets
@param MappingGroup group = null

@template.layout.page(heading = group == null ? "Neue Zuordnungsgruppe" : "Zuordnungsgruppe bearbeiten",
content = @`
    <form class="panel panel--pad formpanel" method="post">
        <div class="field">
            <label for="name">Name</label>
            <input class="form-control" type="text" id="name" name="name"
                   value="${group == null ? "" : group.getName()}" required>
        </div>
        <div class="field">
            <label for="description">Beschreibung</label>
            <input class="form-control" type="text" id="description" name="description"
                   value="${group == null ? "" : group.getDescription()}">
        </div>
        <div class="field">
            <label for="target">MyCoRe Anwendung</label>
            <select class="form-select" id="target" name="target">
                @for(String target : targets)
                    <option value="${target}"
                            selected="${group != null && target.equals(group.getTarget())}">${target}</option>
                @endfor
            </select>
        </div>
        <div class="field">
            <label for="classId">MyCoRe Klassifikation</label>
            <input class="form-control" type="text" id="classId" name="classId"
                   value="${group == null || group.getClassId() == null ? "" : group.getClassId()}">
        </div>
        @if(group != null)
            <input type="hidden" name="id" value="${group.getId()}">
        @endif
        <div class="formactions">
            <button class="btn btn-primary" type="submit">Speichern</button>
            <a class="btn btn-secondary" href="${url("/mapping/groups/")}">Abbrechen</a>
        </div>
    </form>
`)
