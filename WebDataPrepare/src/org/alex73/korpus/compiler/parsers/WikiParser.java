package org.alex73.korpus.compiler.parsers;

import java.io.FileInputStream;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.regex.Pattern;

import javax.xml.parsers.SAXParserFactory;

import org.alex73.korpus.compiler.PrepareCache2;
import org.alex73.korpus.text.parser.Splitter2;
import org.alex73.korpus.text.xml.Content;
import org.alex73.korpus.text.xml.Header;
import org.alex73.korpus.text.xml.Tag;
import org.alex73.korpus.text.xml.XMLText;
import org.apache.commons.compress.compressors.bzip2.BZip2CompressorInputStream;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

/**
 * Data from
 * https://dumps.wikimedia.org/bewiki/latest/bewiki-latest-pages-articles.xml.bz2,
 * https://dumps.wikimedia.org/be_x_oldwiki/latest/be_x_oldwiki-latest-pages-articles.xml.bz2
 */
public class WikiParser implements IParser {
    // Выкідаем: назва старонкі пачынаецца з 'Катэгорыя:', 'Файл:', 'MediaWiki:',
    // 'Шаблон:'
    static final String[] SKIP_TITLE_MARKERS = new String[] { "Катэгорыя:", "Файл:", "MediaWiki:", "Шаблон:", "Шаблён:" };
    // Выкідаем: тэкст пачынаецца з #REDIRECT
    static final String[] SKIP_TEXT_MARKERS = new String[] { "#REDIRECT", "#перанакіраваньне" };
    // Выкідаем з тэксту:
    static final Pattern SKIP_TEXT_RE_KAT = Pattern.compile("\\[\\[Катэгорыя:.+?\\]\\]");
    // Выкідаць: назвы палёў у шаблонах, спысылкі

    static final SAXParserFactory FACTORY = SAXParserFactory.newInstance();

    String urlPrefix;

    @Override
    public void parse(Path file) throws Exception {
        System.out.println(file);
        if (file.getFileName().toString().startsWith("bewiki-")) {
            urlPrefix = "https://be.wikipedia.org/wiki/";
        } else if (file.getFileName().toString().startsWith("be_x_oldwiki-")) {
            urlPrefix = "https://be-tarask.wikipedia.org/wiki/";
        } else {
            throw new Exception("Unknown filename: " + file);
        }

        try (InputStream in = new BZip2CompressorInputStream(new FileInputStream(file.toFile()))) {
            FACTORY.newSAXParser().parse(in, new DefaultHandler() {
                String pageTitle;
                StringBuilder str = new StringBuilder();

                @Override
                public void startElement(String uri, String localName, String qName, Attributes attributes)
                        throws SAXException {
                    str.setLength(0);
                }

                @Override
                public void characters(char[] ch, int start, int length) throws SAXException {
                    str.append(ch, start, length);
                }

                @Override
                public void endElement(String uri, String localName, String qName) throws SAXException {
                    if (qName.equals("title")) {
                        pageTitle = str.toString();
                    } else if (qName.equals("text")) {
                        process(pageTitle, str.toString());
                    }
                }
            });
        }
    }

    protected void process(String title, String text) {
        title = title.trim();
        text = text.trim();
        for (String st : SKIP_TITLE_MARKERS) {
            if (title.startsWith(st)) {
                return;
            }
        }
        for (String st : SKIP_TEXT_MARKERS) {
            if (text.startsWith(st)) {
                return;
            }
        }

        text = SKIP_TEXT_RE_KAT.matcher(text).replaceAll("");

        XMLText doc = new XMLText();
        doc.setHeader(new Header());
        doc.getHeader().getTag().add(new Tag("URL", urlPrefix + title));
        doc.getHeader().getTag().add(new Tag("Title", title));
        doc.setContent(new Content());

        Arrays.asList(text.split("\n")).parallelStream().map(s -> new Splitter2(s, false, PrepareCache2.errors).getP())
                .sequential().forEach(p -> doc.getContent().getPOrTagOrPoetry().add(p));

        if (!doc.getContent().getPOrTagOrPoetry().isEmpty()) {
            try {
                PrepareCache2.process(doc);
            } catch (Exception ex) {
                throw new RuntimeException(ex);
            }
        }
    }
}
