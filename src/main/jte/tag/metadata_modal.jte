@param String id
@param String label
@param String metadata

<button class="btn btn-quiet" type="button" data-bs-toggle="modal" data-bs-target="#metadata_${id}">
    Metadaten
</button>

<div class="modal fade" tabindex="-1" id="metadata_${id}" aria-labelledby="metadata_${id}_title" aria-hidden="true">
    <div class="modal-dialog modal-xl modal-dialog-scrollable">
        <div class="modal-content">
            <div class="modal-header">
                <h2 class="modal-title" id="metadata_${id}_title">Metadaten zu ${label}</h2>
                <button type="button" class="btn-close" data-bs-dismiss="modal" aria-label="Schließen"></button>
            </div>
            <div class="modal-body">
                <pre class="metadata">${metadata}</pre>
            </div>
            <div class="modal-footer">
                <button type="button" class="btn btn-secondary" data-bs-dismiss="modal">Schließen</button>
            </div>
        </div>
    </div>
</div>
