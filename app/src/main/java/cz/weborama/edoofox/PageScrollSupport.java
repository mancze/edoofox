package cz.weborama.edoofox;

/** Small WebView scripts for the native page actions; no JavaScript/native bridge. */
final class PageScrollSupport {
    private PageScrollSupport() { }

    // Sample visible content, not every element in the page. Ignore small widgets and
    // hidden panels; include large nested scrolling surfaces used by web applications.
    private static final String SCROLLERS = """
            const root = document.scrollingElement;
            const scrollers = new Set(root ? [root] : []);
            for (const x of [.25, .5, .75]) for (const y of [.25, .5, .75]) {
                for (let e = document.elementFromPoint(innerWidth*x, innerHeight*y); e; e = e.parentElement) {
                    if (e === root || scrollers.has(e)) continue;
                    if (e.clientHeight < innerHeight*.35 || e.clientWidth < innerWidth*.5) continue;
                    if (e.scrollHeight <= e.clientHeight + 1) continue;
                    if (/auto|scroll/.test(getComputedStyle(e).overflowY)) scrollers.add(e);
                }
            }
            const atTop = [...scrollers].every(e => e.scrollTop <= 1);
            """;

    static String probe(boolean replaceWebsiteControl) {
        // Verified against public Edookit CSS. Remove only our override when disabled,
        // so the site's own display rules and future dynamically inserted controls survive.
        return "(() => {" + SCROLLERS + """
                let style = document.getElementById('edoofox-page-actions-style');
                """ + (replaceWebsiteControl ? """
                if (!style) {
                    style = document.createElement('style');
                    style.id = 'edoofox-page-actions-style';
                    style.textContent = '#scroll-to-top { display: none !important; }';
                    (document.head || document.documentElement).appendChild(style);
                }
                """ : "if (style) style.remove();") + "return atTop;})()";
    }

    static String activate() {
        return "(() => {" + SCROLLERS + """
                if (!atTop) for (const e of scrollers) e.scrollTo({top:0, behavior:'smooth'});
                return atTop;
                })()
                """;
    }
}
