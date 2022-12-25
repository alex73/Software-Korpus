package org.alex73.korpus.compiler.parsers;

import java.io.BufferedInputStream;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.Enumeration;
import java.util.List;
import java.util.function.Consumer;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.alex73.korpus.compiler.MessageParsedText;
import org.alex73.korpus.compiler.PrepareCache3;
import org.alex73.korpus.languages.LanguageFactory;
import org.alex73.korpus.text.parser.PtextToKorpus;
import org.alex73.korpus.text.parser.TextFileParser;

public class OcrTextParser extends BaseParser {
    public OcrTextParser(String subcorpus, Path file) {
        super(subcorpus, file);
    }


    @Override
    public void parse(Consumer<MessageParsedText> publisher, boolean headersOnly) throws Exception {
        try (ZipFile zip = new ZipFile(file.toFile())) {
            for (Enumeration<? extends ZipEntry> it = zip.entries(); it.hasMoreElements();) {
                ZipEntry en = it.nextElement();
                if (en.isDirectory()) {
                    continue;
                }
                TextFileParser doc;
                try (InputStream in = new BufferedInputStream(zip.getInputStream(en))) {
                    doc = new TextFileParser(in, headersOnly);
                }
                MessageParsedText text = new MessageParsedText(1);
                text.textInfo.sourceFilePath = PrepareCache3.INPUT.relativize(file).toString() + "!" + en.getName();
                text.textInfo.subcorpus = subcorpus;
                text.textInfo.subtexts[0].url = doc.headers.get("URL");
                text.textInfo.subtexts[0].file = doc.headers.get("File");
                text.textInfo.subtexts[0].source = doc.headers.get("Source");
                text.textInfo.subtexts[0].title = doc.headers.get("Title");
                text.textInfo.subtexts[0].details = doc.headers.get("Details");
                text.textInfo.subtexts[0].textLabel = text.textInfo.subtexts[0].source;
                if (headersOnly) {
                    // ProcessHeaders.process(textInfo);
                } else {
                    boolean eachLine = fixHyphens(doc.sourceLines);
                    doc.parse(LanguageFactory.get(getLang(text.textInfo.subtexts[0].lang)), false, PrepareCache3.errors);
                    text.paragraphs = get1LangParagraphs(new PtextToKorpus(doc.lines, eachLine).paragraphs);
                    /// ProcessTexts.process(textInfo, );
                }
                publisher.accept(text);
            }
        }
    }

    protected boolean fixHyphens(List<String> doc) {
        boolean hasPages = false;
        for (String line : doc) {
            if (line.isEmpty()) {
                continue;
            }
            if (line.trim().startsWith("##Page:")) {
                hasPages = true;
                break;
            }
        }
        if (!hasPages) {
            return true;
        }
        if (hasPages) {
            for (int i = 1; i < doc.size(); i++) {
                String prev = doc.get(i - 1);
                if (prev.endsWith("\u00AD") || prev.endsWith("-")) {
                    prev = prev.substring(0, prev.length() - 1) + doc.get(i);
                    doc.set(i - 1, prev);
                    doc.remove(i);
                    i--;
                    continue;
                }
            }
        }
        return false;
    }
}
