package org.alex73.korpus.compiler.parsers;

import java.nio.file.Path;

import org.alex73.korpus.compiler.PrepareCache2;
import org.alex73.korpus.text.TextGeneral;
import org.alex73.korpus.text.xml.XMLText;

public class TextParser implements IParser {
    @Override
    public void parse(Path file) throws Exception {
        System.out.println(file);

        XMLText doc = new TextGeneral(file.toFile(), PrepareCache2.errors).parse();
        PrepareCache2.process(doc);
    }
}
