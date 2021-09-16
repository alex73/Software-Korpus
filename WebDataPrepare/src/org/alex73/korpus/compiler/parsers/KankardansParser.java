package org.alex73.korpus.compiler.parsers;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.alex73.korpus.base.TextInfo;
import org.alex73.korpus.compiler.BaseParallelProcessor;
import org.alex73.korpus.compiler.PrepareCache3;
import org.alex73.korpus.compiler.ProcessHeaders;
import org.alex73.korpus.compiler.ProcessTexts;
import org.alex73.korpus.text.parser.PtextToKorpus;
import org.alex73.korpus.text.parser.TextFileParser;

public class KankardansParser extends BaseParser {
    static final String PREFIX_TEXT = "№ тэксту ";
    static final String PREFIX_PAGE = "№ старонкі ";
    static final String PREFIX_ROW = "№ радка";

    private String textTitle = "";
    private int textOrder;
    private StringBuilder text = new StringBuilder();

    public KankardansParser(String subcorpus, Path file) {
        super(subcorpus, file);
    }

    @Override
    public void parse(BaseParallelProcessor queue, boolean headersOnly) throws Exception {
        List<String> lines = Files.readAllLines(file);

        for (String line : lines) {
            line = line.trim();
            if (line.startsWith(PREFIX_TEXT)) {
                flushText(headersOnly);
                textTitle = line.substring(PREFIX_TEXT.length()).replaceAll("^[0-9]+\\.", "").trim();
                text.append("##Title: " + textTitle + "\n\n");
            } else if (line.startsWith(PREFIX_PAGE)) {
                String pageNumber = line.substring(PREFIX_PAGE.length()).trim();
                text.append("##Page: " + pageNumber + "\n");
            } else if (line.startsWith(PREFIX_ROW)) {
            } else if (line.startsWith("№")) {
                throw new RuntimeException(line);
            } else {
                text.append(line.replaceAll("^[0-9]+", "").trim()).append("\n");
            }
        }
        flushText(headersOnly);
    }

    private void flushText(boolean headersOnly) {
        if (!text.isEmpty()) {
            TextFileParser doc = new TextFileParser(
                    new ByteArrayInputStream(text.toString().getBytes(StandardCharsets.UTF_8)), headersOnly,
                    PrepareCache3.errors);
            TextInfo textInfo = new TextInfo();
            textInfo.subcorpus = subcorpus;
            textInfo.textOrder = ++textOrder;
            textInfo.sourceFilePath = PrepareCache3.INPUT.relativize(file).toString() + "!" + textTitle;
            textInfo.title = textTitle;
            if (headersOnly) {
                ProcessHeaders.process(textInfo);
            } else {
                ProcessTexts.process(textInfo, new PtextToKorpus(doc.lines, false).paragraphs);
            }
            text.setLength(0);
        }
    }
}
