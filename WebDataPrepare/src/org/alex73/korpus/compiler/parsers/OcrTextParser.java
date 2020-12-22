package org.alex73.korpus.compiler.parsers;

import java.io.ByteArrayInputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.alex73.korpus.base.TextInfo;
import org.alex73.korpus.compiler.PrepareCache3;
import org.alex73.korpus.text.elements.Paragraph;
import org.alex73.korpus.text.parser.Splitter3;
import org.apache.commons.io.IOUtils;

public class OcrTextParser extends BaseParser {
    public OcrTextParser(String subcorpus, Path file) {
        super(subcorpus, file);
    }

    @Override
    public void parse(Executor queue, boolean headersOnly) throws Exception {
        System.out.println(file);
        byte[] data = headersOnly ? null : Files.readAllBytes(file);

        queue.execute(() -> {
            try {
                TextInfo textInfo = new TextInfo();
                textInfo.sourceFilePath = PrepareCache3.INPUT.relativize(file).toString();
                textInfo.subcorpus = subcorpus;
                textInfo.url = "https://kamunikat.org/?pubid="
                        + file.getFileName().toString().replaceAll("^([0-9]+).+?$", "$1");
                textInfo.title = textInfo.url.replaceAll(".+\\?pub", "");
                if (headersOnly) {
                    PrepareCache3.process(textInfo, null);
                } else {
                    List<Paragraph> content = new ArrayList<>();
                    Splitter3 splitter = new Splitter3(false, PrepareCache3.errors);
                    try (ZipInputStream zip = new ZipInputStream(new ByteArrayInputStream(data))) {
                        String prevtext = null;
                        ZipEntry en;
                        while ((en = zip.getNextEntry()) != null) {
                            if (en.isDirectory() || en.getSize() == 0) {
                                continue;
                            }
                            String text = IOUtils.toString(zip, "UTF-8").trim();
                            if (!text.isEmpty() && !text.equals(prevtext)) {
                                content.add(splitter.parse(text));
                                prevtext = text;
                            }
                        }
                    }
                    PrepareCache3.process(textInfo, content);
                }
            } catch (Exception ex) {
                PrepareCache3.errors.reportError("Error parse " + file, ex);
            }
        });
    }
}
