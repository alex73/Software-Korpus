package org.alex73.korpus.compiler.parsers.warc;

import java.util.List;

import org.alex73.korpus.base.TextInfo;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.netpreserve.jwarc.MessageHeaders;

public abstract class WarcBaseParser {
    protected static final String[] GENRES = new String[] { "публіцыстычны/артыкул" };

    protected abstract TextInfo parse(String url, Document doc, List<String> text);

    public TextInfo parse(String url, MessageHeaders headers, byte[] data, List<String> text) {
        Document doc = Jsoup.parse(new String(data));
        return parse(url, doc, text);
    }
}
