package org.alex73.korpus.parsers.sites;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;

import org.alex73.korpus.base.Ctf;
import org.alex73.korpus.base.Ctf.Page;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

public class ZviazdaBy extends SiteBase {

    public static void main(String[] args) throws Exception {
        new ZviazdaBy().run(args);
    }

    protected Ctf parsePage(String url, byte[] page) throws Exception {
        if (!url.matches("https://zviazda\\.by/be/news/.+")) {
            return null;
        }

        String body = new String(page, StandardCharsets.UTF_8);
        Document doc = Jsoup.parse(body);

        String title = fixText(doc.selectFirst("h1").text());

        Element contentElement = doc.selectFirst("div.field-name-body");
        if (contentElement == null) {
            return null;
        }

        List<String> paragraphs = parseText(contentElement.wholeText());
        if (paragraphs.isEmpty()) {
            return null;
        }
        paragraphs.add(0, title);

        Ctf result = new Ctf();
        Page p = new Page();
        p.paragraphs = paragraphs.toArray(new String[0]);
        result.setPages("bel", Arrays.asList(p));
        result.languages[0].label = "zviazda.by";
        result.languages[0].sourceName = "zviazda.by";
        result.languages[0].title = title;
        result.languages[0].headers = new String[] { "Крыніца:zviazda.by", "URL:" + url, "Назва:" + title };

        return result;
    }
}
