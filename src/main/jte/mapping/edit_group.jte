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
            <select class="form-select" id="classId" name="classId"
                    data-current="${group == null || group.getClassId() == null ? "" : group.getClassId()}"
                    data-url="${url("/mapping/groups/classifications/")}">
                <option value="">(keine)</option>
                @if(group != null && group.getClassId() != null)
                    <option value="${group.getClassId()}" selected>${group.getClassId()}</option>
                @endif
            </select>
            <div class="form-text" id="classIdHint"></div>
        </div>
        @if(group != null)
            <input type="hidden" name="id" value="${group.getId()}">
        @endif
        <div class="formactions">
            <button class="btn btn-primary" type="submit">Speichern</button>
            <a class="btn btn-secondary" href="${url("/mapping/groups/")}">Abbrechen</a>
        </div>
    </form>
    <script>
        (function () {
            const target = document.getElementById('target');
            const select = document.getElementById('classId');
            const hint = document.getElementById('classIdHint');
            let current = select.dataset.current;

            function load() {
                const selected = select.value || current;
                hint.textContent = 'Klassifikationen werden geladen ...';
                fetch(select.dataset.url + '?target=' + encodeURIComponent(target.value),
                    {headers: {'Accept': 'application/json'}})
                    .then(r => {
                        if (!r.ok) {
                            throw new Error(r.status);
                        }
                        return r.json();
                    })
                    .then(list => {
                        select.replaceChildren(new Option('(keine)', ''));
                        let found = false;
                        list.forEach(c => {
                            const label = c.label && c.label !== c.id ? c.id + ' (' + c.label + ')' : c.id;
                            const option = new Option(label, c.id, false, c.id === selected);
                            found = found || c.id === selected;
                            select.add(option);
                        });
                        if (selected && !found) {
                            select.add(new Option(selected, selected, false, true));
                        }
                        hint.textContent = '';
                    })
                    .catch(() => {
                        hint.textContent = 'Klassifikationen konnten nicht geladen werden.';
                    });
            }

            target.addEventListener('change', () => {
                current = '';
                select.value = '';
                load();
            });
            if (target.value) {
                load();
            }
        })();
    </script>
`)
