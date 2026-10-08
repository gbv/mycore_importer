@import org.springframework.data.domain.Page
@import static de.vzg.oai_importer.view.FormatHelper.count
@import static de.vzg.oai_importer.view.PaginationHelper.GAP
@import static de.vzg.oai_importer.view.PaginationHelper.pageWindow
@import static de.vzg.oai_importer.view.UrlHelper.url

@param Page<?> records
@param String baseUrl

@if(records.getTotalPages() > 1)
    <nav class="pagerbar" aria-label="Seiten">
        <ul class="pager">
            @if(records.hasPrevious())
                <li><a href="${url(baseUrl + "?page=" + (records.getNumber() - 1))}" rel="prev">Vorherige</a></li>
            @endif
            @for(Integer page : pageWindow(records))
                @if(page == GAP)
                    <li><span class="pager__gap" aria-hidden="true">…</span></li>
                @else
                    <li>
                        <a href="${url(baseUrl + "?page=" + (page - 1))}"
                           aria-label="Seite ${page}"
                           aria-current="${page == records.getNumber() + 1 ? "page" : null}">${page}</a>
                    </li>
                @endif
            @endfor
            @if(records.hasNext())
                <li><a href="${url(baseUrl + "?page=" + (records.getNumber() + 1))}" rel="next">Nächste</a></li>
            @endif
        </ul>
        <p class="pagerbar__status">Seite ${records.getNumber() + 1} von ${records.getTotalPages()},
            ${count(records.getTotalElements(), "Eintrag", "Einträge")}</p>
    </nav>
@endif
