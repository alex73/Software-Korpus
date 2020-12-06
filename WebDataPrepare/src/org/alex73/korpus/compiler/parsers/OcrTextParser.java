package org.alex73.korpus.compiler.parsers;

import java.io.BufferedInputStream;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.alex73.korpus.compiler.PrepareCache2;
import org.alex73.korpus.text.parser.Splitter2;
import org.alex73.korpus.text.xml.Content;
import org.alex73.korpus.text.xml.Header;
import org.alex73.korpus.text.xml.P;
import org.alex73.korpus.text.xml.Tag;
import org.alex73.korpus.text.xml.XMLText;
import org.apache.commons.io.IOUtils;

public class OcrTextParser implements IParser {
    @Override
    public void parse(Path file) throws Exception {
        System.out.println(file);

        List<String> pages = new ArrayList<>();
        try (ZipFile zip = new ZipFile(file.toFile())) {
            String prevtext = null;
            for (Enumeration<? extends ZipEntry> it = zip.entries(); it.hasMoreElements();) {
                ZipEntry en = it.nextElement();
                if (en.isDirectory() || en.getSize() == 0) {
                    continue;
                }

                String text;
                try (InputStream in = new BufferedInputStream(zip.getInputStream(en))) {
                    text = IOUtils.toString(in, "UTF-8");
                }
                if (!text.isEmpty() && !text.equals(prevtext)) {
                    pages.add(text);
                    prevtext = text;
                }
            }
        }
        pages.parallelStream().forEach(text -> {
            XMLText doc = new XMLText();
            doc.setHeader(new Header());
            doc.getHeader().getTag().add(new Tag("URL","https://kamunikat.org/?pubid="+ file.getFileName().toString().replaceAll("^([0-9]+).+?$", "$1")));
            doc.setContent(new Content());

            P p = new Splitter2(text, false, PrepareCache2.errors).getP();
            doc.getContent().getPOrTagOrPoetry().add(p);

            if (!doc.getContent().getPOrTagOrPoetry().isEmpty()) {
                try {
                    PrepareCache2.process(doc);
                } catch (Exception ex) {
                    throw new RuntimeException(ex);
                }
            }
        });
    }
}
