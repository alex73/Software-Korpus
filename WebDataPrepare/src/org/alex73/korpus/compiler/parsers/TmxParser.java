package org.alex73.korpus.compiler.parsers;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.function.Consumer;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.alex73.korpus.compiler.MessageParsedText;
import org.alex73.korpus.compiler.PrepareCache3;
import org.alex73.korpus.languages.LanguageFactory;
import org.alex73.korpus.text.parser.PtextToKorpus;
import org.alex73.korpus.text.parser.Splitter3;
import org.alex73.korpus.text.structure.corpus.Paragraph;
import org.alex73.korpus.text.structure.files.TextLine;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

public class TmxParser extends BaseParser {
    static final SAXParserFactory FACTORY = SAXParserFactory.newInstance();

    public TmxParser(String subcorpus, Path file) {
        super(subcorpus, file);
    }

    @Override
    public void parse(Consumer<MessageParsedText> publisher, boolean headersOnly) throws Exception {
        List<Map<String, String>> segments = new ArrayList<>();
        Map<String, Map<String, String>> properties = new TreeMap<>();
        Set<String> tmxLanguages = new TreeSet<>();

        SAXParser parser = FACTORY.newSAXParser();
        parser.parse(file.toFile(), new DefaultHandler() {
            StringBuilder path = new StringBuilder();
            StringBuilder s = new StringBuilder();
            Map<String, String> textByLang;
            String tuvLang;
            String segText;

            @Override
            public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
                s.setLength(0);
                path.append('/').append(qName);
                switch (path.toString()) {
                case "/tmx/property":
                    String lang = attributes.getValue("xml:lang");
                    String name = attributes.getValue("name");
                    String value = attributes.getValue("value");
                    if (properties.computeIfAbsent(name, n -> new TreeMap<>()).put(lang, value) != null) {
                        throw new RuntimeException("Wrong TMX: duplicate property: " + name);
                    }
                    break;
                case "/tmx/body/tu":
                    textByLang = new TreeMap<>();
                    break;
                case "/tmx/body/tu/tuv":
                    tuvLang = attributes.getValue("xml:lang");
                    tmxLanguages.add(tuvLang);
                    break;
                }
            }

            @Override
            public void endElement(String uri, String localName, String qName) throws SAXException {
                if (!path.toString().endsWith("/" + qName)) {
                    throw new RuntimeException("Wrong path in XML: " + path);
                }
                switch (path.toString()) {
                case "/tmx/body/tu/tuv/seg":
                    segText = s.toString();
                    break;
                case "/tmx/body/tu/tuv":
                    if (tuvLang == null || segText == null) {
                        throw new RuntimeException("Wrong TMX: no segment or language");
                    }
                    if (textByLang.put(tuvLang, segText) != null) {
                        throw new RuntimeException("Wrong TMX: segment for one language defined twice");
                    }
                    tuvLang = null;
                    segText = null;
                    break;
                case "/tmx/body/tu":
                    if (textByLang.size() != 2) {
                        throw new RuntimeException("Wrong TMX: pair expected");
                    }
                    segments.add(textByLang);
                    break;
                }
                path.setLength(path.length() - qName.length() - 1);
                s.setLength(0);
            }

            @Override
            public void characters(char[] ch, int start, int length) throws SAXException {
                s.append(ch, start, length);
            }
        });

        if (tmxLanguages.size() != 2) {
            throw new RuntimeException("Wrong TMX: expected two languages");
        }
        String[] langs = tmxLanguages.toArray(new String[0]);
        MessageParsedText text = new MessageParsedText(2);
        text.textInfo.sourceFilePath = PrepareCache3.INPUT.relativize(file).toString();
        text.textInfo.subcorpus = subcorpus;
        for (int i = 0; i < langs.length; i++) {
            text.textInfo.subtexts[i].lang = langs[i];
            text.textInfo.subtexts[i].title = properties.get("title").get(langs[i]);
            String s;
            if ((s = properties.computeIfAbsent("authors", a -> Map.of()).get(langs[i])) != null) {
                text.textInfo.subtexts[i].authors = splitAndTrim(s);
            }
            if ((s = properties.computeIfAbsent("edition", a -> Map.of()).get(langs[i])) != null) {
                text.textInfo.subtexts[0].edition = s;
            }
            text.textInfo.subtexts[i].textLabel = text.textInfo.subtexts[i].title;
        }

        AuthorsUtil.fixAuthors(text.textInfo.subtexts[0]);
        if (!headersOnly) {
            List<Paragraph[]> result = new ArrayList<>();
            for (Map<String, String> seg : segments) {
                Splitter3 splitter = new Splitter3(LanguageFactory.get("bel").getNormalizer(), false, PrepareCache3.errors);
                TextLine p = splitter.parse(seg.get("bel"));
                List<Paragraph> ps1 = new PtextToKorpus(List.of(p), false).paragraphs;
                if (ps1.size() != 1) {
                    throw new RuntimeException("Wrong paragraphs count");
                }

                splitter = new Splitter3(LanguageFactory.get("rus").getNormalizer(), false, PrepareCache3.errors);
                p = splitter.parse(seg.get("rus"));
                List<Paragraph> ps2 = new PtextToKorpus(List.of(p), false).paragraphs;
                if (ps2.size() != 1) {
                    throw new RuntimeException("Wrong paragraphs count");
                }

                Paragraph[] pr = new Paragraph[2];
                pr[0] = ps1.get(0);
                pr[1] = ps2.get(0);
                result.add(pr);
            }
            text.paragraphs = result.toArray(new Paragraph[result.size()][]);
        }
        publisher.accept(text);
    }
}
