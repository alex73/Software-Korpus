package org.alex73.korpus.parsers.sites;

import org.alex73.korpus.base.Ctf;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;

public class EtalonlineBy extends SiteBase {

    public static void main(String[] args) throws Exception {
        new EtalonlineBy().runDir(args);
    }

    @Override
    protected Ctf parsePage(String url, byte[] page) throws Exception {
        String fn = url.replaceAll("\\.html$", "");

        String body = new String(page, StandardCharsets.UTF_8);
        Document doc = Jsoup.parse(body);

        removeElement(doc.selectFirst("div.noscript"));

        Element contentElement = oneElement(doc.select("div.usercontent"));

        Element headerElement = doc.selectFirst("title");
        String title = fixText(headerElement.text());

        List<String> paragraphs = parseText(textFromElement(contentElement));
        if (paragraphs.isEmpty()) {
            return null;
        }

        Ctf result = new Ctf();
        Ctf.Page p = new Ctf.Page();
        p.paragraphs = paragraphs.toArray(new String[0]);
        result.setPages("bel", Arrays.asList(p));
        result.languages[0].label = "etalonline.by";
        result.languages[0].sourceName = "etalonline.by";
        result.languages[0].title = title;
        result.languages[0].headers = new String[]{"Крыніца:etalonline.by", "URL:https://etalonline.by/document/?regnum=" + fn, "Назва:" + title};

        return result;
    }
}
