package org.alex73.korpus.parsers.sites;

import org.alex73.korpus.base.Ctf;
import org.alex73.korpus.base.Ctf.Page;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ZviazdaBy extends SiteBase {

    public static void main(String[] args) throws Exception {
        new ZviazdaBy().runWarc(args);
    }

    protected Ctf parsePage(String url, byte[] page) throws Exception {
        if (url.endsWith("/")) {
            url = url.substring(0, url.length() - 1);
        }
        if (!url.matches("https://zviazda\\.by/news/[^/]+")) {
            return null;
        }

        String body = new String(page, StandardCharsets.UTF_8);
        Document doc = Jsoup.parse(body);

        Element contentElement = doc.selectFirst("div.item-fulltext");
        if (contentElement == null) {
            return null;
        }

        String title = fixText(doc.selectFirst("div.news-static").selectFirst("h1").text());

        List<String> paragraphs = new ArrayList<>();
        paragraphs.add(title);

        Element contentPreElement = doc.selectFirst("div.item-pretext");
        if (contentPreElement != null) {
            paragraphs.addAll(parseText(textFromElement(contentPreElement)));
        }

        paragraphs.addAll(parseText(textFromElement(contentElement)));

        Ctf result = new Ctf();
        Page p = new Page();
        p.paragraphs = paragraphs.toArray(new String[0]);
        result.setPages("bel", Arrays.asList(p));
        result.languages[0].label = "zviazda.by";
        result.languages[0].sourceName = "zviazda.by";
        result.languages[0].title = title;
        result.languages[0].headers = new String[]{"Крыніца:zviazda.by", "URL:" + url, "Назва:" + title};

        return result;
    }
}
