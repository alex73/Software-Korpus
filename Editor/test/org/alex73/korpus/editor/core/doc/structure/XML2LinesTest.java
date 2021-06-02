package org.alex73.korpus.editor.core.doc.structure;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import org.alex73.korpus.text.TextIO;
import org.alex73.korpus.text.structure.files.LongTagItem;
import org.alex73.korpus.text.structure.files.SentenceSeparatorItem;
import org.alex73.korpus.text.xml.Content;
import org.alex73.korpus.text.xml.ITextLineElement;
import org.alex73.korpus.text.xml.InlineTag;
import org.alex73.korpus.text.xml.S;
import org.alex73.korpus.text.xml.W;
import org.alex73.korpus.text.xml.XMLText;
import org.alex73.korpus.text.xml.Z;
import org.custommonkey.xmlunit.XMLTestCase;
import org.junit.Test;
import org.xml.sax.InputSource;

public class XML2LinesTest extends XMLTestCase {
    List<Line> expected = new ArrayList<>();

    @Test
    public void testXml2Lines() throws Exception {
        XMLText xml;
        try (InputStream in = new FileInputStream(
                "test-editor/org/alex73/korpus/editor/core/structure/test1.xml")) {
            xml = TextIO.parseXML(in);
        }
        List<Line> xmlLines = XML2Lines.convertToLines(xml.getContent());

        ex(new W("Першы", "MAOSMAS_MAOSMNS", "пе´ршы", null), new S(' '), new W("сказ", "", "", null),
                new Z("."), new SentenceSeparatorItem(), new S(' '), new InlineTag("<i>"),
                new W("Другі", "MAOSMAS_MAOSMNS_SAFXMAS_SAFXMNS", "другі´", null), new InlineTag("</i>"),
                new Z("."), new S('\n'));
        ex(new LongTagItem("##Poetry: begin"), new S('\n'));
        ex(new W("Слова", "", "", null), new Z(","), new S(' '),
                new W("другое", "MAOSFGS_MAOSNAS_MAOSNNS_SAFXNNS", "другі´", null), new Z(","), new S('\n'));
        ex(new W("трэцяе", "MAOSFDS_MAOSNAS_MAOSNNS_SAFXNNS", "трэ´ці", null), new Z("."), new S('\n'));
        ex(new S('\n'));
        ex(new W("Новы", "", "", null), new S(' '), new W("параграф", "", "", null), new Z("."), new S('\n'));
        ex(new S('\n'));
        ex(new LongTagItem("##Sign: подпіс"), new S('\n'));
        ex(new LongTagItem("##Poetry: end"), new S('\n'));
        ex(new W("Яшчэ", "CSX_E_RXP", "яшчэ_яшчэ´", null), new Z("."), new S('\n'));
        ex(new LongTagItem("##Long: tag"), new S('\n'));
        ex(new S(' '));

        expected.equals(xmlLines);
        assertEquals(expected, xmlLines);
    }

    @Test
    public void testLines2Xml() throws Exception {
        XMLText xml;
        try (InputStream in = new FileInputStream(
                "test-editor/org/alex73/korpus/editor/core/structure/test1.xml")) {
            xml = TextIO.parseXML(in);
        }
        List<Line> xmlLines = XML2Lines.convertToLines(xml.getContent());

        Content c = XML2Lines.convertToXML(xmlLines);
        xml.setContent(c);
        TextIO.saveXML(new File("/tmp/xml"), xml);
        assertXMLEqual(new InputSource("test-editor/org/alex73/korpus/editor/core/structure/test1.xml"),
                new InputSource("/tmp/xml"));
    }

    private void ex(ITextLineElement... elements) {
        Line line = new Line();
        expected.add(line);
        for (ITextLineElement el : elements) {
            line.add(el);
        }
    }
}
