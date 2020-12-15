package org.alex73.korpus.compiler.parsers;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipInputStream;

import org.alex73.korpus.base.TextInfo;
import org.alex73.korpus.compiler.PrepareCache3;
import org.alex73.korpus.text.parser.Splitter2;
import org.alex73.korpus.text.xml.P;
import org.apache.commons.io.IOUtils;

public class OcrTextParser extends BaseParser {
    public OcrTextParser(String subcorpus, Path file) {
        super(subcorpus, file);
    }

//    @Override
//    public void readHeaders() throws Exception {
//        System.out.println(file);
//
//        XMLText doc = prepareHeader(file);
//        TextInfo textInfo = new TextInfo();
//        textInfo.subcorpus = subcorpus;
//        textInfo.sourceFileUri = getClass().getSimpleName() + ":" + file;
//        TextUtils.fillFromXml(textInfo, doc);
//        PrepareCache2.processHeader(textInfo);
//    }

    @Override
    public void parse(Executor queue) throws Exception {
        System.out.println(file);
        byte[] data = Files.readAllBytes(file);

        queue.execute(() -> {
            TextInfo textInfo = new TextInfo();
            textInfo.sourceFilePath = PrepareCache3.INPUT.relativize(file).toString();
            textInfo.subcorpus = subcorpus;
            textInfo.url = "https://kamunikat.org/?pubid="
                    + file.getFileName().toString().replaceAll("^([0-9]+).+?$", "$1");
            textInfo.title = textInfo.url.replaceAll(".+\\?", "");

            List<Object> content = new ArrayList<>();
            try {
                try (ZipInputStream zip = new ZipInputStream(new ByteArrayInputStream(data))) {
                    String prevtext = null;
                    ZipEntry en;
                    while ((en = zip.getNextEntry()) != null) {
                        if (en.isDirectory() || en.getSize() == 0) {
                            continue;
                        }

                        String text = IOUtils.toString(zip, "UTF-8");
                        if (!text.isEmpty() && !text.equals(prevtext)) {
                            P p = new Splitter2(text, false, PrepareCache3.errors).getP();
                            if (!p.getSe().isEmpty()) {
                                content.add(p);
                            }
                            prevtext = text;
                        }
                    }
                }
                PrepareCache3.process(textInfo, content);
            } catch (Exception ex) {
                throw new RuntimeException(ex);
            }
        });
    }
}
