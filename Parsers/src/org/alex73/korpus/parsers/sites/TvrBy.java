package org.alex73.korpus.parsers.sites;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;

import org.alex73.korpus.base.Ctf;
import org.alex73.korpus.base.Ctf.Page;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

public class TvrBy extends SiteBase {
    public static void main(String[] args) throws Exception {
        new TvrBy().run(args);
    }

    protected Ctf parsePage(String url, byte[] page) throws Exception {
        if (!url.matches("https://www\\.tvr\\.by/bel/news/.+")) {
            return null;
        }

        String body = new String(page, StandardCharsets.UTF_8);
        Document doc = Jsoup.parse(body);

        Element contentElement = oneOrNoneElement(doc.select("div.content"));
        if (contentElement == null) {
            return null;
        }

        String title = fixText(doc.selectFirst("h1").text());

        List<String> paragraphs = parseText(contentElement.wholeText());
        if (paragraphs.isEmpty()) {
            return null;
        }
        paragraphs.add(0, title);

        Ctf result = new Ctf();
        Page p = new Page();
        p.paragraphs = paragraphs.toArray(new String[0]);
        result.setPages("bel", Arrays.asList(p));
        result.languages[0].label = "tvr.by";
        result.languages[0].sourceName = "tvr.by";
        result.languages[0].title = title;
        result.languages[0].headers = new String[] { "Крыніца:tvr.by", "URL:" + url, "Назва:" + title };

        return result;
    }
}
