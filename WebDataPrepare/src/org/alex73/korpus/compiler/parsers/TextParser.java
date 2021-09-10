package org.alex73.korpus.compiler.parsers;

import java.io.ByteArrayInputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.Executor;

import org.alex73.korpus.base.TextInfo;
import org.alex73.korpus.compiler.PrepareCache3;
import org.alex73.korpus.compiler.TextUtils;
import org.alex73.korpus.text.parser.PtextToKorpus;
import org.alex73.korpus.text.parser.TextFileParser;

public class TextParser extends BaseParser {
    private static Map<String, String> authorsIndex = new HashMap<>();

    public TextParser(String subcorpus, Path file) {
        super(subcorpus, file);
    }

    @Override
    public void parse(Executor queue, boolean headersOnly) throws Exception {
        String fn = file.getFileName().toString();
        if (headersOnly && fn.contains("autary") && fn.endsWith(".list")) {
            initializeAuthors(file);
            return;
        }
        byte[] data = Files.readAllBytes(file);
        queue.execute(() -> {
            try {
                TextFileParser doc = new TextFileParser(new ByteArrayInputStream(data), headersOnly,
                        PrepareCache3.errors);
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
                fixAuthors(textInfo);
                PrepareCache3.process(textInfo, new PtextToKorpus(doc.lines, true).paragraphs);
            } catch (Exception ex) {
                PrepareCache3.errors.reportError("Error parse " + file, ex);
            }
        });
    }

    private static void fixAuthors(TextInfo textInfo) {
        if (textInfo.authors != null) {
            for (int i = 0; i < textInfo.authors.length; i++) {
                String replaced = authorsIndex.get(textInfo.authors[i]);
                if (replaced != null) {
                    textInfo.authors[i] = replaced;
                } else {
                    String[] a = textInfo.authors[i].split("\\s+");
                    switch (a.length) {
                    case 1:
                        break;
                    case 2:
                        textInfo.authors[i] = a[1] + ' ' + a[0];
                        break;
                    default:
                        throw new RuntimeException("Impossible to index author: " + textInfo.authors[i]);
                    }
                }
            }
        }
    }

    public static void initializeAuthors(Path file) {
        try {
            Map<String, String> tags = new TreeMap<>();
            for (String s : Files.readAllLines(file)) {
                s = s.trim();
                if (s.isEmpty()) {
                    addAuthorToIndex(tags);
                    tags.clear();
                } else if (!s.startsWith("##")) {
                    continue;
                } else {
                    int pos = s.indexOf(':');
                    if (pos < 0) {
                        throw new Exception("Error parse " + file + ": " + s);
                    }
                    String key = s.substring(2, pos).trim();
                    String value = s.substring(pos + 1).trim();
                    tags.put(key, value);
                }
            }
            addAuthorToIndex(tags);
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    private static void addAuthorToIndex(Map<String, String> tags) {
        String author = tags.get("Author");
        String authorIndex = tags.get("AuthorIndex");
        if (author != null && authorIndex != null) {
            if (authorsIndex.put(author, authorIndex) != null) {
                throw new RuntimeException("Duplicate author in list: " + tags);
            }
        }
    }
}
