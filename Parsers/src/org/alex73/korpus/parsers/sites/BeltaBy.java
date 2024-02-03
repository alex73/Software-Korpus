package org.alex73.korpus.parsers.sites;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;

import org.alex73.korpus.base.Ctf;
import org.alex73.korpus.base.Ctf.Page;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

public class BeltaBy extends SiteBase {

    public static void main(String[] args) throws Exception {
        new BeltaBy().run(args);
    }

    protected Ctf parsePage(String url, byte[] page) throws Exception {
        if (!url.matches("https://blr\\.belta\\.by/.+")) {
            return null;
        }
        if (url.matches("https://blr\\.belta\\.by/printv/.+")) {
            return null;
        }

        String body = new String(page, StandardCharsets.UTF_8);
        Document doc = Jsoup.parse(body);

        Element contentElement = doc.selectFirst("div.main");
        if (contentElement == null) {
            return null;
        }

        String title = fixText(contentElement.selectFirst("h1").text());

        Element textElement = oneOrNoneElement(contentElement.select("div.inner_content"));
        if (textElement == null) {
            return null;
        }
        Element rem = oneOrNoneElement(textElement.select("div.invite_in_messagers"));
        if (rem != null) {
            rem.remove();
        }

        List<String> paragraphs = parseText(textElement.wholeText());
        if (paragraphs.isEmpty()) {
            return null;
        }
        paragraphs.add(0, title);

        Ctf result = new Ctf();
        Page p = new Page();
        p.paragraphs = paragraphs.toArray(new String[0]);
        result.setPages("bel", Arrays.asList(p));
        result.languages[0].label = "belta.by";
        result.languages[0].source = "belta.by";
        result.languages[0].title = title;
        result.languages[0].headers = new String[] { "Крыніца:belta.by", "URL:" + url, "Назва:" + title };

        return result;
    }
}
