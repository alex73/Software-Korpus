package org.alex73.korpus.parsers.sites;

import org.alex73.korpus.base.Ctf;
import org.alex73.korpus.base.Ctf.Page;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;

/**
 * BeltaByFromZip <zip_file_with_html_pages> <zip_file_with_ctf_pages>
 */
public class BeltaByFromZip extends SiteBase {

    public static void main(String[] args) throws Exception {
        new BeltaByFromZip().runZip(args);
    }

    @Override
    protected String filterUrl(String url) {
        url = "https://" + url;
        if (!url.startsWith("https://blr.belta.by/printv/")) {
            return null;
        }
        url = url.replaceFirst("https://blr.belta.by/printv/", "https://blr.belta.by/");
        return url;
    }

    @Override
    protected Ctf parsePage(String url, byte[] page) throws Exception {
        String body = new String(page, StandardCharsets.UTF_8);
        Document doc = Jsoup.parse(body);

        Element contentElement = doc.selectFirst("#pv_main_part");
        if (contentElement == null) {
            return null;
        }

        Element headerElement = contentElement.selectFirst("h1");
        String title = "";
        if (headerElement != null) {
            title = fixText(headerElement.text());
            headerElement.remove();
        }

        Element rem = oneOrNoneElement(contentElement.select("div.pv_date"));
        if (rem != null) {
            rem.remove();
        }

        List<String> paragraphs = parseText(textFromElement(contentElement));
        if (paragraphs.isEmpty()) {
            return null;
        }
        paragraphs.add(0, title);

        Ctf result = new Ctf();
        Page p = new Page();
        p.paragraphs = paragraphs.toArray(new String[0]);
        result.setPages("bel", Arrays.asList(p));
        result.languages[0].label = "belta.by";
        result.languages[0].sourceName = "belta.by";
        result.languages[0].title = title;
        result.languages[0].headers = new String[]{"Крыніца:belta.by", "URL:" + url, "Назва:" + title};

        return result;
    }
}
