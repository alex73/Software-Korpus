package org.alex73.korpus.compiler.parsers;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Enumeration;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.alex73.korpus.base.TextInfo;
import org.alex73.korpus.compiler.BaseParallelProcessor;
import org.alex73.korpus.compiler.PrepareCache3;
import org.alex73.korpus.compiler.ProcessHeaders;
import org.alex73.korpus.compiler.ProcessTexts;
import org.alex73.korpus.text.parser.Headers;
import org.alex73.korpus.text.parser.PtextToKorpus;
import org.alex73.korpus.text.parser.TextFileParser;
import org.apache.commons.io.IOUtils;

public class TextArchiveParser extends BaseParser {
    public TextArchiveParser(String subcorpus, Path file) {
        super(subcorpus, file);
    }

    @Override
    public void parse(BaseParallelProcessor queue, boolean headersOnly) throws Exception {
        Path headersFile = Paths.get(file.toString() + ".headers");
        Headers commonHeaders;
        if (Files.exists(headersFile)) {
            try (InputStream in = Files.newInputStream(headersFile)) {
                TextFileParser fp = new TextFileParser(in, true);
                commonHeaders = fp.headers;
            }
        } else {
            commonHeaders = new Headers();
        }

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
                    textInfo.source = commonHeaders.get("Source");
                    textInfo.title = doc.headers.get("Title");
                    textInfo.url = doc.headers.get("URL");
                    textInfo.publicationTime = getAndCheckYears(doc.headers.get("PublicationYear"));
                    textInfo.textLabel = textInfo.source;
                    String s;
                    if ((s = doc.headers.get("StyleGenre")) != null) {
                        textInfo.styleGenres = splitAndTrim(s);
                    }
                    if (textInfo.title == null) {
                        textInfo.title = "";
                    }
                    if (headersOnly) {
                        ProcessHeaders.process(textInfo);
                    } else {
                        doc.parse(true, PrepareCache3.errors);
                        ProcessTexts.process(textInfo, new PtextToKorpus(doc.lines, true).paragraphs);
                    }
                });
            }
        }
    }
}
