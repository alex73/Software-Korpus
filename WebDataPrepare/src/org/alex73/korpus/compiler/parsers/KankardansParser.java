package org.alex73.korpus.compiler.parsers;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
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

        boolean inText = false;
        for (String line : lines) {
            line = line.trim();
            if (line.startsWith(PREFIX_TEXT)) {
                flushText(headersOnly);
                inText = false;
                textTitle = line.substring(PREFIX_TEXT.length()).replaceAll("^[0-9]+\\.", "").trim();
            } else if (line.startsWith(PREFIX_PAGE)) {
                if (!inText) {
                    inText = true;
                    text.append("\n");
                }
                String pageNumber = line.substring(PREFIX_PAGE.length()).trim();
                text.append("##Page: " + pageNumber + "\n");
            } else if (line.startsWith(PREFIX_ROW)) {
            } else if (line.startsWith("№")) {
                throw new Exception("Wrong format: " + line);
            } else if (line.startsWith("##")) {
                if (!inText) {
                    text.append(line + "\n");
                } else {
                    throw new Exception("Wrong format: " + line);
                }
            } else {
                text.append(line.replaceAll("^[0-9]+", "").trim()).append("\n");
            }
        }
        flushText(headersOnly);
    }

    private void flushText(boolean headersOnly) {
        if (!text.isEmpty()) {
            TextFileParser doc = new TextFileParser(
                    new ByteArrayInputStream(text.toString().getBytes(StandardCharsets.UTF_8)), headersOnly);
            doc.parse(PrepareCache3.errors);
            TextInfo textInfo = new TextInfo();
            textInfo.subcorpus = subcorpus;
            textInfo.textOrder = ++textOrder;
            textInfo.sourceFilePath = PrepareCache3.INPUT.relativize(file).toString() + "!" + textTitle;
            textInfo.title = doc.headers.get("Title");
            String s;
            if ((s = doc.headers.get("Authors")) != null) {
                textInfo.authors = splitAndTrim(s);
            }
            textInfo.creationTime = getAndCheckYears(doc.headers.get("CreationYear"));
            textInfo.publicationTime = getAndCheckYears(doc.headers.get("PublicationYear"));
            textInfo.textLabel = textInfo.authors != null ? String.join(",", Arrays.asList(textInfo.authors)) : "———";
            if (headersOnly) {
                ProcessHeaders.process(textInfo);
            } else {
                ProcessTexts.process(textInfo, new PtextToKorpus(doc.lines, false).paragraphs);
            }
            text.setLength(0);
        }
    }
}
