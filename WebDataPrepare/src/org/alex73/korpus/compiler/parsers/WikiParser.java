package org.alex73.korpus.compiler.parsers;

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.regex.Pattern;

import javax.xml.parsers.SAXParserFactory;

import org.alex73.korpus.base.TextInfo;
import org.alex73.korpus.compiler.PrepareCache3;
import org.alex73.korpus.text.parser.PtextToKorpus;
import org.alex73.korpus.text.parser.Splitter3;
import org.alex73.korpus.text.structure.corpus.Paragraph;
import org.apache.commons.compress.compressors.bzip2.BZip2CompressorInputStream;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

/**
 * Data from
 * https://dumps.wikimedia.org/bewiki/latest/bewiki-latest-pages-articles.xml.bz2,
 * https://dumps.wikimedia.org/be_x_oldwiki/latest/be_x_oldwiki-latest-pages-articles.xml.bz2
 */
public class WikiParser extends BaseParser {
    // Выкідаем: назва старонкі пачынаецца з 'Катэгорыя:', 'Файл:', 'MediaWiki:',
    // 'Шаблон:'
    static final String[] SKIP_TITLE_MARKERS = new String[] { "Катэгорыя:", "Файл:", "MediaWiki:", "Шаблон:", "Шаблён:",
            "Вікіпедыя:", "Вікіпэдыя:" };
    // Выкідаем: тэкст пачынаецца з #REDIRECT
    static final String[] SKIP_TEXT_MARKERS = new String[] { "#REDIRECT", "#перанакіраваньне" };
    // Выкідаем з тэксту:
    static final Pattern SKIP_TEXT_RE_KAT = Pattern.compile("\\[\\[Катэгорыя:.+?\\]\\]");
    // Выкідаць: назвы палёў у шаблонах, спысылкі

    static final SAXParserFactory FACTORY = SAXParserFactory.newInstance();

    private boolean headersOnly;
    String urlPrefix;
    String source;

    public WikiParser(String subcorpus, Path file) {
        super(subcorpus, file);
    }

    @Override
    public void parse(Executor queue, boolean headersOnly) throws Exception {
        this.headersOnly = headersOnly;

        if (file.getFileName().toString().startsWith("bewiki-")) {
            urlPrefix = "https://be.wikipedia.org/wiki/";
            source = "wiki:be";
        } else if (file.getFileName().toString().startsWith("be_x_oldwiki-")) {
            urlPrefix = "https://be-tarask.wikipedia.org/wiki/";
            source = "wiki:be-x-old";
        } else {
            throw new Exception("Unknown filename: " + file);
        }

        try (InputStream in = file.toString().endsWith(".bz2")
                ? new BZip2CompressorInputStream(new FileInputStream(file.toFile()))
                : new BufferedInputStream(new FileInputStream(file.toFile()))) {
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
                        process(queue, pageTitle, str.toString());
                    }
                }
            });
        }
    }

    protected void process(Executor queue, String inTitle, String inText) {
        queue.execute(() -> {
            try {
                String title = inTitle.trim();
                String text = inText.trim();
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

                TextInfo textInfo = new TextInfo();
                textInfo.sourceFilePath = PrepareCache3.INPUT.relativize(file).toString() + "!" + inTitle;
                textInfo.source = source;
                textInfo.subcorpus = subcorpus;
                textInfo.url = urlPrefix + title;
                textInfo.title = title;
                if (headersOnly) {
                    PrepareCache3.process(textInfo, null);
                } else {
                    text = SKIP_TEXT_RE_KAT.matcher(text).replaceAll("");
                    List<Paragraph> content = new ArrayList<>();
                    StringBuilder ptext = new StringBuilder();
                    Splitter3 splitter = new Splitter3(false, PrepareCache3.errors);
                    for (String s : text.split("\n")) {
                        s = s.trim();
                        if (s.isEmpty()) {
                            if (ptext.length() > 0) {
                                if (!ptext.toString().replace('\n', ' ').trim().isEmpty()) {
                                    Paragraph par = PtextToKorpus.oneLine(splitter.parse(ptext));
                                    if (par != null) {
                                        content.add(par);
                                    }
                                }
                                ptext.setLength(0);
                            }
                        } else {
                            ptext.append(s).append('\n');
                        }
                    }
                    if (!ptext.toString().replace('\n', ' ').trim().isEmpty()) {
                        content.add(PtextToKorpus.oneLine(splitter.parse(ptext)));
                    }
                    if (!content.isEmpty()) {
                        PrepareCache3.process(textInfo, content);
                    }
                }
            } catch (Exception ex) {
                PrepareCache3.errors.reportError("Error parse " + file + "!" + inTitle, ex);
            }
        });
    }
}
