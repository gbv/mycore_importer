@import java.util.List

@param String id
@param String label
@param List<String> files

@if(files.isEmpty())
    <span class="chip">Keine Dateien</span>
@else
    <button class="btn btn-quiet" type="button" data-bs-toggle="modal" data-bs-target="#files_${id}">
        ${files.size()} Dateien
    </button>

    <div class="modal fade" tabindex="-1" id="files_${id}" aria-labelledby="files_${id}_title" aria-hidden="true">
        <div class="modal-dialog modal-lg modal-dialog-scrollable">
            <div class="modal-content">
                <div class="modal-header">
                    <h2 class="modal-title" id="files_${id}_title">Dateien zu ${label}</h2>
                    <button type="button" class="btn-close" data-bs-dismiss="modal" aria-label="Schließen"></button>
                </div>
                <div class="modal-body">
                    <ul class="filelist">
                        @for(String file : files)
                            <li><span>${file}</span></li>
                        @endfor
                    </ul>
                </div>
                <div class="modal-footer">
                    <button type="button" class="btn btn-secondary" data-bs-dismiss="modal">Schließen</button>
                </div>
            </div>
        </div>
    </div>
@endif
