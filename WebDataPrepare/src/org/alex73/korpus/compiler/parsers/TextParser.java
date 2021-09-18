package org.alex73.korpus.compiler.parsers;

import java.io.ByteArrayInputStream;
import java.nio.file.Files;
import java.nio.file.Path;

import org.alex73.korpus.base.TextInfo;
import org.alex73.korpus.compiler.BaseParallelProcessor;
import org.alex73.korpus.compiler.PrepareCache3;
import org.alex73.korpus.compiler.ProcessHeaders;
import org.alex73.korpus.compiler.ProcessTexts;
import org.alex73.korpus.text.parser.PtextToKorpus;
import org.alex73.korpus.text.parser.TextFileParser;

public class TextParser extends BaseParser {

    public TextParser(String subcorpus, Path file) {
        super(subcorpus, file);
    }

    @Override
    public void parse(BaseParallelProcessor queue, boolean headersOnly) throws Exception {
        byte[] data = Files.readAllBytes(file);
        queue.run(() -> {
            TextFileParser doc = new TextFileParser(new ByteArrayInputStream(data), headersOnly, PrepareCache3.errors);
            TextInfo textInfo = new TextInfo();
            textInfo.sourceFilePath = PrepareCache3.INPUT.relativize(file).toString();
            textInfo.subcorpus = subcorpus;
            textInfo.title = doc.headers.get("Title");
            textInfo.lang = doc.headers.get("Lang");
            if ("bel".equals(textInfo.lang)) {
                textInfo.lang = null;
            }
            textInfo.langOrig = doc.headers.get("LangOrig");
            if ("bel".equals(textInfo.langOrig)) {
                textInfo.langOrig = null;
            }
            if (textInfo.lang != null) {
                // пераклад на іншую мову
                return;
            }
            if (textInfo.langOrig != null && "teksty".equals(subcorpus)) {
                // корпус перакладаў
                textInfo.subcorpus = "pieraklady";
            }
            String s;
            if ((s = doc.headers.get("Authors")) != null) {
                textInfo.authors = splitAndTrim(s);
            }
            if ((s = doc.headers.get("StyleGenre")) != null) {
                textInfo.styleGenres = splitAndTrim(s);
            }
            textInfo.edition = doc.headers.get("Edition");
            textInfo.creationTime = getAndCheckYears(doc.headers.get("CreationYear"));
            textInfo.publicationTime = getAndCheckYears(doc.headers.get("PublicationYear"));

            AuthorsUtil.fixAuthors(textInfo);
            if (headersOnly) {
                ProcessHeaders.process(textInfo);
            } else {
                ProcessTexts.process(textInfo, new PtextToKorpus(doc.lines, true).paragraphs);
            }
        });
    }
}
