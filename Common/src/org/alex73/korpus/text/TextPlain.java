package org.alex73.korpus.text;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;

import org.alex73.korpus.text.parser.BOMBufferedReader;
import org.alex73.korpus.text.parser.IProcess;
import org.alex73.korpus.text.xml.Content;
import org.alex73.korpus.text.xml.Header;
import org.alex73.korpus.text.xml.XMLText;

/**
 * Simple text.
 */
public class TextPlain implements IText {
    private XMLText doc;

    public TextPlain(File f, IProcess errors) throws Exception {
        try (InputStream in = new FileInputStream(f)) {
            load(in, errors);
        }
    }

    @Override
    public XMLText parse() {
        return doc;
    }

    private void load(InputStream in, IProcess errors) throws Exception {
        BufferedReader rd = new BOMBufferedReader(new InputStreamReader(in, "UTF-8"));

        doc = new XMLText();
        doc.setHeader(new Header());
        doc.setContent(new Content());

        String s;
        while ((s = rd.readLine()) != null) {
            s = s.trim();

//            P p = new Splitter2(s, true, errors).getP();
//            doc.getContent().getPOrTagOrPoetry().add(p);
        }
    }

}
