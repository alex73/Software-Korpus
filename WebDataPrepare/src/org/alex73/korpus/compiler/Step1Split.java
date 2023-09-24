package org.alex73.korpus.compiler;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.alex73.korpus.base.Ctf;
import org.alex73.korpus.base.TextInfo;
import org.alex73.korpus.languages.LanguageFactory;
import org.alex73.korpus.text.parser.IProcess;
import org.alex73.korpus.text.parser.Splitter3;
import org.alex73.korpus.text.structure.corpus.Paragraph;
import org.alex73.korpus.text.structure.corpus.Sentence;
import org.alex73.korpus.text.structure.corpus.Word;
import org.alex73.korpus.text.structure.files.ITextLineElement;
import org.alex73.korpus.text.structure.files.InlineTag;
import org.alex73.korpus.text.structure.files.LongTagItem;
import org.alex73.korpus.text.structure.files.SentenceSeparatorItem;
import org.alex73.korpus.text.structure.files.TailItem;
import org.alex73.korpus.text.structure.files.TextLine;
import org.alex73.korpus.text.structure.files.WordItem;

import com.google.gson.Gson;

public class Step1Split {

    private static Map<String, ThreadLocal<Splitter3>> splitters;

    private static ThreadLocal<Gson> gsons = new ThreadLocal<>() {
        @Override
        protected Gson initialValue() {
            return new Gson();
        }
    };

    public static void init(String[] languages, IProcess process) {
        splitters = new TreeMap<>();
        for (String lang : languages) {
            splitters.put(lang, new ThreadLocal<>() {
                @Override
                protected Splitter3 initialValue() {
                    return new Splitter3(LanguageFactory.get(lang).getNormalizer(), true, process);
                }
            });
        }
    }

    public static MessageParsedText run(byte[] bytes) throws Exception {
        Ctf ctf = gsons.get().fromJson(new String(bytes, StandardCharsets.UTF_8), Ctf.class);

        MessageParsedText outText = new MessageParsedText(ctf.languages.length);
        outText.textInfo.styleGenres = ctf.styleGenres;

        List<List<Paragraph>> o = new ArrayList<>();
        for (int la = 0; la < ctf.languages.length; la++) {
            Ctf.Language ctfLang = ctf.languages[la];

            outText.textInfo.subtexts[la] = convertTextInfo(ctfLang);

            Splitter3 splitter = splitters.get(ctfLang.lang).get();
            if (splitter == null) {
                throw new Exception("You should run corpus compiler with lang '" + ctfLang.lang + "'");
            }
            List<Paragraph> ps = new ArrayList<>();
            o.add(ps);
            StringBuilder inlinePage = new StringBuilder();
            for (Ctf.Page ctfPage : ctfLang.pages) {
                for (String s : ctfPage.paragraphs) {
                    TextLine line = splitter.parse(s);
                    Paragraph p = new Paragraph();
                    p.lang = ctfLang.lang;
                    p.page = ctfPage.pageNum;
                    p.sentences = parseTextLine(line, inlinePage);
                    ps.add(p);
                }
            }
            outText.languages[la] = new MessageParsedText.Language();
            outText.languages[la].lang = ctfLang.lang;
            outText.languages[la].paragraphs = ps.toArray(new Paragraph[0]);
        }

        return outText;
    }

    static TextInfo.Subtext convertTextInfo(Ctf.Language lang) {
        TextInfo.Subtext r = new TextInfo.Subtext();
        r.authors = lang.authors;
        r.creationTime = lang.creationTime;
        r.label = lang.label;
        r.lang = lang.lang;
        r.publicationTime = lang.publicationTime;
        r.source = lang.source;
        r.title = lang.title;

        StringBuilder passport = new StringBuilder();
        for (String h : lang.headers) {
            int p = h.indexOf(':');
            if (p < 0) {
                throw new RuntimeException("Няправільны загаловак тэкста: " + h);
            }
            if (!passport.isEmpty()) {
                passport.append("<br/>");
            }
            passport.append("<b>").append(h.substring(0, p).trim()).append(":</b> ").append(h.substring(p + 1).trim());
        }
        r.passport = passport.toString();

        return r;
    }

    static Sentence[] parseTextLine(TextLine line, StringBuilder inlinePage) {
        List<Sentence> sentences = new ArrayList<>();
        List<Word> words = new ArrayList<>();
        if (!inlinePage.isEmpty()) {
            boolean firstIsPageNumber = !line.isEmpty() && (line.get(0) instanceof InlineTag) && RE_INLINE_TAG_PAGE.matcher(line.get(0).getText()).matches();
            if (!firstIsPageNumber) {
                addInlinePageNumber(words, inlinePage);
            }
        }
        for (ITextLineElement w : line) {
            if (w instanceof InlineTag) {
                Matcher m;
                if ((m = RE_INLINE_TAG_PAGE.matcher(w.getText())).matches()) {
                    inlinePage.setLength(0);
                    inlinePage.append(m.group(1));
                    addInlinePageNumber(words, inlinePage);
                }
            } else if (w instanceof LongTagItem) {
                throw new RuntimeException("!!!");
            } else if (w instanceof SentenceSeparatorItem) {
                flushSentence(sentences, words);
            } else if (w instanceof WordItem) {
                WordItem wi = (WordItem) w;
                Word wo = new Word();
                wo.word = wi.word;
                wo.tail = "";
                words.add(wo);
            } else if (w instanceof TailItem) {
                if (!words.isEmpty()) {
                    Word prev = words.get(words.size() - 1);
                    if (prev.tail == null) {
                        prev.tail = w.getText();
                    } else {
                        prev.tail += w.getText();
                    }
                } else {
                    Word wo = new Word();
                    wo.word = null;
                    wo.tail = w.getText();
                    words.add(wo);
                }
            }
        }
        return sentences.toArray(new Sentence[0]);
    }

    static final Pattern RE_INLINE_TAG_PAGE = Pattern.compile("<p:(.+)>");

    static void addInlinePageNumber(List<Word> words, StringBuilder inlinePage) {
        Word wo = new Word();
        wo.word = null;
        wo.tail = "{" + inlinePage + "}";
        wo.type = Word.OtherType.PAZNAKA;
        words.add(wo);
    }

    static void flushSentence(List<Sentence> sentences, List<Word> words) {
        if (words.isEmpty()) {
            return;
        }
        Sentence s = new Sentence();
        s.words = words.toArray(new Word[words.size()]);
        words.clear();
        sentences.add(s);
    }
}
