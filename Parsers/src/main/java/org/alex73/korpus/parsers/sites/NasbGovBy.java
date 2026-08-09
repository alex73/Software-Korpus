package org.alex73.korpus.parsers.sites;

import org.alex73.korpus.base.Ctf;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;

public class NasbGovBy extends SiteBase {
    public static void main(String[] args) throws Exception {
        new NasbGovBy().runWarc(args);
    }

    protected Ctf parsePage(String url, byte[] page) throws Exception {
        if (!url.startsWith("https://nasb.gov.by/bel/")) {
            return null;
        }
        if (url.contains("?")) {
            return null;
        }
        if (url.contains("/rus/")) {
            return null;
        }

        String body = new String(page, StandardCharsets.UTF_8);
        Document doc = Jsoup.parse(body);

        removeElement(doc.selectFirst("header"));
        removeElement(doc.selectFirst("footer"));
        removeElement(doc.selectFirst(".page-title-wrap"));

        Element titleElement = doc.selectFirst("title");
        if (titleElement == null) {
            return null;
        }
        String title = fixText(titleElement.text().trim());
        if ("Карта сайта".equals(title)) {
            return null;
        }

        List<String> paragraphs = parseText(textFromElement(doc.selectFirst("body")));
        if (paragraphs.isEmpty()) {
            return null;
        }
        paragraphs.add(0, title);

        Ctf result = new Ctf();
        Ctf.Page p = new Ctf.Page();
        p.paragraphs = paragraphs.toArray(new String[0]);
        result.setPages("bel", Arrays.asList(p));
        result.languages[0].label = "nasb.gov.by";
        result.languages[0].sourceName = "nasb.gov.by";
        result.languages[0].title = title;
        result.languages[0].headers = new String[]{"Крыніца:nasb.gov.by", "URL:" + url, "Назва:" + title};

        return result;
    }
}
