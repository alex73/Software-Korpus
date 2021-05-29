package org.alex73.korpus.text;

import java.io.File;
import java.io.InputStream;

import org.alex73.korpus.text.parser.IProcess;
import org.alex73.korpus.text.xml.Content;
import org.alex73.korpus.text.xml.P;
import org.alex73.korpus.text.xml.Poetry;
import org.alex73.korpus.text.xml.Se;
import org.alex73.korpus.text.xml.W;
import org.alex73.korpus.text.xml.XMLText;
import org.custommonkey.xmlunit.XMLTestCase;
import org.junit.Test;
import org.xml.sax.InputSource;

public class TextGeneralTest extends XMLTestCase {

    public TextGeneralTest() throws Exception {
        //Splitter2.init(new GrammarFiller(new GrammarFinder(GrammarDB2.initializeFromDir("GrammarDB"))));
    }

    @Test
    public void testText2XML() throws Exception {
        XMLText xml;
        try (InputStream in = TextGeneralTest.class.getResourceAsStream("/org/alex73/korpus/text/parserTest1.text")) {
            xml = new TextGeneral(in, new IProcess() {

                @Override
                public void showStatus(String status) {
                }

                @Override
                public void reportError(String error, Throwable ex) {
                    throw new RuntimeException(error,ex);
                }
            }).parse();
        }
        clearCatLemma(xml.getContent());
        TextIO.saveXML(new File("/tmp/xml"), xml);

        assertXMLEqual(
                new InputSource(TextGeneralTest.class.getResourceAsStream("/org/alex73/korpus/text/parserTest1.xml")),
                new InputSource("/tmp/xml"));
    }

    void clearCatLemma(Content c) {
        for (Object obj : c.getPOrTagOrPoetry()) {
            if (obj instanceof P) {
                P p = (P) obj;
                for (Se se : p.getSe()) {
                    for (Object obj2 : se.getWOrSOrZ()) {
                        if (obj2 instanceof W) {
                            W o = (W) obj2;
                            o.setCat(null);
                            o.setLemma(null);
                        }
                    }
                }
            } else if (obj instanceof Poetry) {
                Poetry po = (Poetry) obj;
                for (Object obj3 : po.getPOrTag()) {
                    if (obj3 instanceof P) {
                        P p = (P) obj3;
                        for (Se se : p.getSe()) {
                            for (Object obj2 : se.getWOrSOrZ()) {
                                if (obj2 instanceof W) {
                                    W o = (W) obj2;
                                    o.setCat(null);
                                    o.setLemma(null);
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
