package org.alex73.korpus.compiler.parsers;

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.InputStream;
import java.nio.file.Path;

import org.alex73.korpus.compiler.PrepareCache2;
import org.alex73.korpus.text.TextIO;
import org.alex73.korpus.text.xml.XMLText;

public class XmlParser implements IParser {
    @Override
    public void parse(Path file) throws Exception {
        XMLText doc;
        try (InputStream in = new BufferedInputStream(new FileInputStream(file.toFile()), BUFFER)) {
            doc = TextIO.parseXML(in);
        }
        PrepareCache2.process(doc);
    }
}
