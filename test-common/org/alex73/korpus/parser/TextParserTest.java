package org.alex73.korpus.parser;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;

import org.alex73.korpus.editor.core.GrammarDB;
import org.alex73.korpus.text.TextIO;
import org.alex73.korpus.text.parser.IProcess;
import org.alex73.korpus.text.parser.TextParser;
import org.alex73.korpus.text.xml.Content;
import org.alex73.korpus.text.xml.P;
import org.alex73.korpus.text.xml.Poetry;
import org.alex73.korpus.text.xml.Se;
import org.alex73.korpus.text.xml.W;
import org.alex73.korpus.text.xml.XMLText;
import org.alex73.korpus.text.xml.Z;
import org.custommonkey.xmlunit.XMLTestCase;
import org.junit.Test;
import org.xml.sax.InputSource;

public class TextParserTest extends XMLTestCase {

    public TextParserTest() throws Exception {
        GrammarDB.initializeFromDir(new File("GrammarDB"), new GrammarDB.LoaderProgress() {
            public void setFilesCount(int count) {
            }

            public void beforeFileLoading(String file) {
            }

            public void afterFileLoading() {
            }
        });
    }

    @Test
    public void testText2XML() throws Exception {
        XMLText xml;
        try (InputStream in = new FileInputStream("test-common/org/alex73/korpus/parser/parserTest1.text")) {
            xml = TextParser.parseText(in, false, new IProcess() {

                @Override
                public void showStatus(String status) {
                }

                @Override
                public void reportError(String error) {
                    throw new RuntimeException(error);
                }
            });
        }
        clearCatLemma(xml.getContent());
        TextIO.saveXML(new File("/tmp/xml"), xml);

        assertXMLEqual(new InputSource("test-common/org/alex73/korpus/parser/parserTest1.xml"),
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
                        } else if (obj2 instanceof Z) {
                            Z o = (Z) obj2;
                            o.setCat(null);
                        }
                    }
                }
            }else if (obj instanceof Poetry) {
                Poetry po=(Poetry)obj;
                for (Object obj3 : po.getPOrTag()) {
                    if (obj3 instanceof P) {
                        P p = (P) obj3;
                        for (Se se : p.getSe()) {
                            for (Object obj2 : se.getWOrSOrZ()) {
                                if (obj2 instanceof W) {
                                    W o = (W) obj2;
                                    o.setCat(null);
                                    o.setLemma(null);
                                } else if (obj2 instanceof Z) {
                                    Z o = (Z) obj2;
                                    o.setCat(null);
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
