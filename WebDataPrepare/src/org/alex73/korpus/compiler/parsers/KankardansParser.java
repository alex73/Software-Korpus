package org.alex73.korpus.compiler.parsers;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.function.Consumer;

import org.alex73.korpus.compiler.MessageParsedText;
import org.alex73.korpus.compiler.PrepareCache3;
import org.alex73.korpus.compiler.ProcessTexts;
import org.alex73.korpus.languages.LanguageFactory;
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
    public void parse(Consumer<MessageParsedText> publisher, boolean headersOnly) throws Exception {
        List<String> lines = Files.readAllLines(file);

        boolean inText = false;
        for (String line : lines) {
            line = line.trim();
            if (line.startsWith(PREFIX_TEXT)) {
                flushText(publisher, headersOnly);
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
        flushText(publisher, headersOnly);
    }

    private void flushText(Consumer<MessageParsedText> publisher, boolean headersOnly) {
        if (!text.isEmpty()) {
            MessageParsedText ti = new MessageParsedText(1);
            TextFileParser.OneText doc = new TextFileParser(new ByteArrayInputStream(text.toString().getBytes(StandardCharsets.UTF_8)), headersOnly)
                    .oneTextExpected();
            ti.textInfo.subcorpus = subcorpus;
            ti.textInfo.textOrder = ++textOrder;
            ti.textInfo.sourceFilePath = PrepareCache3.INPUT.relativize(file).toString() + "!" + textTitle;
            String s;
            if ((s = doc.headers.get("Authors")) != null) {
                ti.textInfo.subtexts[0].authors = splitAndTrim(s);
                for (int i = 0; i < ti.textInfo.subtexts[0].authors.length; i++) {
                    if (ti.textInfo.subtexts[0].authors[i].endsWith("?")) {
                        ti.textInfo.subtexts[0].authors[i] = ti.textInfo.subtexts[0].authors[i].replaceAll("\\?$", "");
                    }
                }
            }
            ti.textInfo.subtexts[0].creationTime = getAndCheckYears(doc.headers.get("CreationYear"));
            ti.textInfo.subtexts[0].publicationTime = getAndCheckYears(doc.headers.get("PublicationYear"));
            ti.textInfo.subtexts[0].headers = doc.headers.getAll();
            AuthorsUtil.fixAuthors(ti.textInfo.subtexts[0]);
            ProcessTexts.preprocessor.constructTextPassport(ti.textInfo, ti.textInfo.subtexts[0]);
            if (!headersOnly) {
                doc.parse(LanguageFactory.get(getLang(ti.textInfo.subtexts[0].lang)), false, PrepareCache3.errors);
                ti.paragraphs = get1LangParagraphs(new PtextToKorpus(doc.lines, false).paragraphs);
            }
            publisher.accept(ti);
            text.setLength(0);
        }
    }
}
