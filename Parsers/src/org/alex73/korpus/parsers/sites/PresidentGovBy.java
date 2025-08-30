package org.alex73.korpus.parsers.sites;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;

import org.alex73.korpus.base.Ctf;
import org.alex73.korpus.base.Ctf.Page;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

public class PresidentGovBy extends SiteBase {
    public static void main(String[] args) throws Exception {
        new PresidentGovBy().run(args);
    }

    protected Ctf parsePage(String url, byte[] page) throws Exception {
        if (!url.matches("https://president\\.gov\\.by/be/.+")) {
            return null;
        }

        String body = new String(page, StandardCharsets.UTF_8);
        Document doc = Jsoup.parse(body);

        Elements contentElement = doc.select("div.wysiwyg");
        if (contentElement.size() != 1) {
            return null;
        }

        Element titleElement = oneOrNoneElement(doc.select("h1"));
        if (titleElement == null) {
            return null;
        }
        String title = fixText(titleElement.text());

        List<String> paragraphs = parseText(contentElement.get(0).wholeText());
        if (url.contains("addzyal")) {
            System.out.println();
        }
        if (paragraphs.isEmpty()) {
            return null;
        }
        paragraphs.add(0, title);

        Ctf result = new Ctf();
        Page p = new Page();
        p.paragraphs = paragraphs.toArray(new String[0]);
        result.setPages("bel", Arrays.asList(p));
        result.languages[0].label = "president.gov.by";
        result.languages[0].sourceName = "president.gov.by";
        result.languages[0].title = title;
        result.languages[0].headers = new String[] { "Крыніца:president.gov.by", "URL:" + url, "Назва:" + title };

        return result;
    }
}
