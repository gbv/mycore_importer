@import java.util.List
@import de.vzg.oai_importer.mapping.jpa.Mapping
@import de.vzg.oai_importer.mapping.jpa.MappingGroup
@import de.vzg.oai_importer.mycore.MyCoReRestAPIService
@import static de.vzg.oai_importer.view.UrlHelper.url

@param MappingGroup group
@param Mapping mapping = null
@param List<MyCoReRestAPIService.Category> categories = null

@template.layout.page(heading = mapping == null ? "Neue Zuordnung" : "Zuordnung bearbeiten",
lead = "Gruppe " + group.getName(),
content = @`
    <form class="panel panel--pad formpanel" method="post">
        <div class="field">
            <label for="from">Von</label>
            <input class="form-control" type="text" id="from" name="from"
                   value="${mapping == null ? "" : mapping.getFrom()}" required>
        </div>
        <div class="field">
            <label for="to">Zu</label>
            @if(categories != null)
                <select class="form-select" id="to" name="to">
                    <option value="">Bitte wählen</option>
                    @for(MyCoReRestAPIService.Category category : categories)
                        !{var selected = mapping != null && category.id().equals(mapping.getTo());}
                        <option value="${category.id()}" selected="${selected}">${category.label()}</option>
                    @endfor
                </select>
            @else
                <input class="form-control" type="text" id="to" name="to"
                       value="${mapping == null ? "" : mapping.getTo()}">
            @endif
        </div>
        @if(mapping != null)
            <input type="hidden" name="id" value="${mapping.getId()}">
        @endif
        <input type="hidden" name="group" value="${group.getId()}">
        <div class="formactions">
            <button class="btn btn-primary" type="submit">Speichern</button>
            <a class="btn btn-secondary" href="${url("/mapping/groups/" + group.getId() + "/")}">Abbrechen</a>
        </div>
    </form>
`)
