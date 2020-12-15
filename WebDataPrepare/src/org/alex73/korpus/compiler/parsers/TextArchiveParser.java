package org.alex73.korpus.compiler.parsers;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Enumeration;
import java.util.concurrent.Executor;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.alex73.korpus.base.TextInfo;
import org.alex73.korpus.compiler.PrepareCache3;
import org.alex73.korpus.compiler.TextUtils;
import org.alex73.korpus.text.TextGeneral;
import org.alex73.korpus.text.xml.Header;
import org.alex73.korpus.text.xml.XMLText;
import org.apache.commons.io.IOUtils;

public class TextArchiveParser extends BaseParser {
    public TextArchiveParser(String subcorpus, Path file) {
        super(subcorpus, file);
    }

//    @Override
//    public void readHeaders() throws Exception {
//        try (ZipFile zip = new ZipFile(file.toFile())) {
//            for (Enumeration<? extends ZipEntry> it = zip.entries(); it.hasMoreElements();) {
//                ZipEntry en = it.nextElement();
//                if (en.isDirectory()) {
//                    continue;
//                }
//                XMLText doc;
//                try (InputStream in = new BufferedInputStream(zip.getInputStream(en))) {
//                    doc = new TextGeneral(zip.getInputStream(en), PrepareCache2.errors).parse();
//                }
//                TextInfo textInfo = new TextInfo();
//                TextUtils.fillFromXml(textInfo, doc);
//                PrepareCache2.processHeader(textInfo);
//            }
//        }
//    }

    @Override
    public void parse(Executor queue) throws Exception {
        System.out.println(file);

        Path headersFile = Paths.get(file.toString() + ".headers");
        Header headers;
        if (Files.exists(headersFile)) {
            try (InputStream in = Files.newInputStream(headersFile)) {
                headers = new TextGeneral(in, PrepareCache3.errors).parse().getHeader();
            }
        } else {
            headers = null;
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
                queue.execute(() -> {
                    try {
                        XMLText doc = new TextGeneral(new ByteArrayInputStream(data), PrepareCache3.errors).parse();
                        if (headers != null) {
                            doc.getHeader().getTag().addAll(headers.getTag());
                        }
                        TextInfo textInfo = new TextInfo();
                        textInfo.sourceFilePath = PrepareCache3.INPUT.relativize(file).toString() + "!" + en.getName();
                        textInfo.subcorpus = subcorpus;
                        TextUtils.fillFromXml(textInfo, doc);
                        if (textInfo.title == null) {
                            textInfo.title = "";
                        }
                        PrepareCache3.process(textInfo, doc.getContent().getPOrTagOrPoetry());
                    } catch (Exception ex) {
                        throw new RuntimeException("Error parse " + file + "!" + en.getName(), ex);
                    }
                });
            }
        }
    }
}
