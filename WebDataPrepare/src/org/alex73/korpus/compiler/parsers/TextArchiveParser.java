package org.alex73.korpus.compiler.parsers;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.Enumeration;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.alex73.korpus.compiler.PrepareCache2;
import org.alex73.korpus.text.TextGeneral;
import org.alex73.korpus.text.xml.XMLText;
import org.apache.commons.compress.archivers.sevenz.SevenZArchiveEntry;
import org.apache.commons.compress.archivers.sevenz.SevenZFile;

public class TextArchiveParser implements IParser {
    @Override
    public void parse(Path file) throws Exception {
        System.out.println(file);
        if (true)
            return;

        if (file.toString().endsWith(".zip")) {
            try (ZipFile zip = new ZipFile(file.toFile())) {
                for (Enumeration<? extends ZipEntry> it = zip.entries(); it.hasMoreElements();) {
                    ZipEntry en = it.nextElement();
                    if (en.isDirectory()) {
                        continue;
                    }
                    XMLText doc;
                    try (InputStream in = new BufferedInputStream(zip.getInputStream(en))) {
                        doc = new TextGeneral(zip.getInputStream(en), PrepareCache2.errors).parse();
                    }
                    PrepareCache2.process(doc);
                }
            }
        } else if (file.toString().endsWith(".7z")) {
            try (SevenZFile sevenZFile = new SevenZFile(file.toFile())) {
                SevenZArchiveEntry entry;
                while ((entry = sevenZFile.getNextEntry()) != null) {
                    byte[] content = new byte[(int) entry.getSize()];
                    sevenZFile.read(content, 0, content.length);

                    XMLText doc = new TextGeneral(new ByteArrayInputStream(content), PrepareCache2.errors).parse();
                    PrepareCache2.process(doc);
                }
            }
        } else {
            throw new Exception("Wrong archive name: " + file);
        }
    }
}
