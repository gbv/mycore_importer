@import gg.jte.Content
@import static de.vzg.oai_importer.view.AuthHelper.isFullyAuthenticated
@import static de.vzg.oai_importer.view.UrlHelper.isSection
@import static de.vzg.oai_importer.view.UrlHelper.url

@param String heading
@param Content content
@param Content actions = null
@param String lead = null
@param String width = "wide"

<!DOCTYPE html>
<html lang="de">
<head>
    <meta charset="utf-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title>${heading} | Importer</title>
    <link rel="stylesheet" href="${url("/webjars/bootstrap/5.3.2/css/bootstrap.min.css")}">
    <link rel="stylesheet" href="${url("/css/importer.css")}">
</head>
<body>
<header class="masthead">
    <div class="masthead__inner">
        <a class="wordmark" href="${url("/")}">Importer</a>
        @if(isFullyAuthenticated())
            <nav aria-label="Hauptnavigation">
                <ul class="mainnav">
                    <li><a href="${url("/sources/")}" aria-current="${isSection("/sources") ? "page" : null}">Quellen</a></li>
                    <li><a href="${url("/targets/")}" aria-current="${isSection("/targets") ? "page" : null}">Ziele</a></li>
                    <li><a href="${url("/jobs/")}" aria-current="${isSection("/jobs") ? "page" : null}">Vorgänge</a></li>
                    <li><a href="${url("/mapping/groups/")}" aria-current="${isSection("/mapping") ? "page" : null}">Zuordnung</a></li>
                </ul>
            </nav>
            <div class="masthead__end">
                <form action="${url("/logout")}" method="post">
                    <button class="linkbutton" type="submit">Abmelden</button>
                </form>
            </div>
        @else
            @if(!isSection("/login"))
                <div class="masthead__end">
                    <a href="${url("/login")}">Anmelden</a>
                </div>
            @endif
        @endif
    </div>
</header>

<main class="${"wide".equals(width) ? "page" : "page page--" + width}">
    <div class="page__head">
        <h1 class="page__title">${heading}</h1>
        @if(lead != null)
            <p class="page__lead">${lead}</p>
        @endif
    </div>

    @if(actions != null)
        <div class="toolbar">
            ${actions}
        </div>
    @endif

    ${content}
</main>

<script src="${url("/webjars/popper.js/2.11.7/umd/popper.js")}"></script>
<script src="${url("/webjars/bootstrap/5.3.2/js/bootstrap.min.js")}"></script>
</body>
</html>
