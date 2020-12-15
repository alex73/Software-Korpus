package org.alex73.korpus.compiler.parsers;

import java.io.ByteArrayInputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.Executor;

import org.alex73.korpus.base.TextInfo;
import org.alex73.korpus.compiler.PrepareCache3;
import org.alex73.korpus.compiler.TextUtils;
import org.alex73.korpus.text.TextGeneral;
import org.alex73.korpus.text.xml.XMLText;

public class TextParser extends BaseParser {
    public TextParser(String subcorpus, Path file) {
        super(subcorpus, file);
    }

//    @Override
//    public void readHeaders() throws Exception {
//        System.out.println(file);
//
//        XMLText doc = new TextGeneral(file.toFile(), PrepareCache2.errors).parse();
//        TextInfo textInfo = new TextInfo();
//        TextUtils.fillFromXml(textInfo, doc);
//        PrepareCache2.processHeader(textInfo);
//    }

    @Override
    public void parse(Executor queue) throws Exception {
        System.out.println(file);
        byte[] data = Files.readAllBytes(file);
        queue.execute(() -> {
            try {
                XMLText doc = new TextGeneral(new ByteArrayInputStream(data), PrepareCache3.errors).parse();
                TextInfo textInfo = new TextInfo();
                textInfo.sourceFilePath = PrepareCache3.INPUT.relativize(file).toString();
                textInfo.subcorpus = subcorpus;
                TextUtils.fillFromXml(textInfo, doc);
                PrepareCache3.process(textInfo, doc.getContent().getPOrTagOrPoetry());
            } catch (Exception ex) {
                throw new RuntimeException("Error parse " + file, ex);
            }
        });
    }
}
