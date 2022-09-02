package org.alex73.korpus.compiler.parsers;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.Enumeration;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.alex73.korpus.base.TextInfo;
import org.alex73.korpus.compiler.BaseParallelProcessor;
import org.alex73.korpus.compiler.PrepareCache3;
import org.alex73.korpus.compiler.ProcessHeaders;
import org.alex73.korpus.compiler.ProcessTexts;
import org.alex73.korpus.text.parser.PtextToKorpus;
import org.alex73.korpus.text.parser.TextFileParser;
import org.apache.commons.io.IOUtils;

public class OcrTextParser extends BaseParser {
    public OcrTextParser(String subcorpus, Path file) {
        super(subcorpus, file);
    }

    @Override
    public void parse(BaseParallelProcessor queue, boolean headersOnly) throws Exception {
        try (ZipFile zip = new ZipFile(file.toFile())) {
            for (Enumeration<? extends ZipEntry> it = zip.entries(); it.hasMoreElements();) {
                ZipEntry en = it.nextElement();
                if (en.isDirectory()) {
                    continue;
                }
                byte[] data;
                try (InputStream in = new BufferedInputStream(zip.getInputStream(en))) {
                    data = IOUtils.toByteArray(in);
                }
                queue.run(() -> {
                    TextFileParser doc = new TextFileParser(new ByteArrayInputStream(data), headersOnly);
                    TextInfo textInfo = new TextInfo();
                    textInfo.sourceFilePath = PrepareCache3.INPUT.relativize(file).toString() + "!" + en.getName();
                    textInfo.subcorpus = subcorpus;
                    textInfo.url = doc.headers.get("URL");
                    textInfo.file = doc.headers.get("File");
                    textInfo.source = doc.headers.get("Source");
                    textInfo.title = doc.headers.get("Title");
                    textInfo.details = doc.headers.get("Details");
                    textInfo.textLabel = textInfo.source;
                    if (headersOnly) {
                        ProcessHeaders.process(textInfo);
                    } else {
                        boolean eachLine = fixHyphens(doc.sourceLines);
                        doc.parse(false, PrepareCache3.errors);
                        ProcessTexts.process(textInfo, new PtextToKorpus(doc.lines, eachLine).paragraphs);
                    }
                });
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
