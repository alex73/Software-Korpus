package org.alex73.korpus.compiler.parsers;

import java.io.ByteArrayInputStream;
import java.nio.file.Files;
import java.nio.file.Path;

import org.alex73.korpus.base.TextInfo;
import org.alex73.korpus.compiler.BaseParallelProcessor;
import org.alex73.korpus.compiler.PrepareCache3;
import org.alex73.korpus.compiler.ProcessHeaders;
import org.alex73.korpus.compiler.ProcessTexts;
import org.alex73.korpus.compiler.TextUtils;
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
            TextUtils.fillFromHeaders(textInfo, doc.headers);
            if ("bel".equals(textInfo.lang)) {
                textInfo.lang = null;
            }
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
            AuthorsUtil.fixAuthors(textInfo);
            if (headersOnly) {
                ProcessHeaders.process(textInfo);
            } else {
                ProcessTexts.process(textInfo, new PtextToKorpus(doc.lines, true).paragraphs);
            }
        });
    }
}
