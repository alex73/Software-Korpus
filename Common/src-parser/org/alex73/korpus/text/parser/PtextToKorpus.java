package org.alex73.korpus.text.parser;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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

@Deprecated
public class PtextToKorpus {
    enum BLOCK_MODE {
        ONE_LINE, EMPTY_LINE_SEPARATOR
    };

    static final Pattern RE_INLINE_TAG_PAGE = Pattern.compile("<p:(.+)>");

    public final List<Paragraph> paragraphs = new ArrayList<>();
    private final List<Sentence> sentences = new ArrayList<>();
    private final List<Word> words = new ArrayList<>();
    private BLOCK_MODE mode;
    private String page;
    private String inlinePage;

    public PtextToKorpus(List<TextLine> lines, boolean splitEachLine) {
        mode = splitEachLine ? BLOCK_MODE.ONE_LINE : BLOCK_MODE.EMPTY_LINE_SEPARATOR;
        for (TextLine line : lines) {
            if (line.isEmpty() || (line.size() == 1 && line.get(0).getText().isBlank())) {
                switch (mode) {
                case ONE_LINE:
                    continue;
                case EMPTY_LINE_SEPARATOR:
                    flushParagraph();
                    continue;
                default:
                    break;
                }
            }
            if (words.isEmpty() && sentences.isEmpty() && inlinePage != null) {
                boolean firstIsPageNumber = !line.isEmpty() && (line.get(0) instanceof InlineTag)
                        && RE_INLINE_TAG_PAGE.matcher(line.get(0).getText()).matches();
                if (!firstIsPageNumber) {
                    addInlinePageNumber();
                }
            }
            for (ITextLineElement w : line) {
                if (w instanceof InlineTag) {
                    Matcher m;
                    if ((m = RE_INLINE_TAG_PAGE.matcher(w.getText())).matches()) {
                        inlinePage = m.group(1);
                        addInlinePageNumber();
                    }
                } else if (w instanceof LongTagItem) {
                    if ("##Poetry:begin".equals(w.getText())) {
                        mode = BLOCK_MODE.EMPTY_LINE_SEPARATOR;
                    } else if ("##Poetry:end".equals(w.getText())) {
                        flushParagraph();
                        mode = BLOCK_MODE.ONE_LINE;
                    } else if (w.getText().startsWith("##Page:")) {
                        flushParagraph();
                        page = w.getText().substring(7).trim();
                    }
                } else if (w instanceof SentenceSeparatorItem) {
                    flushSentence();
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
            if (mode == BLOCK_MODE.ONE_LINE) {
                flushParagraph();
            }
        }
        flushParagraph();
    }

    private void addInlinePageNumber() {
        Word wo = new Word();
        wo.word = null;
        wo.tail = "{" + inlinePage + "}";
        wo.type = Word.OtherType.PAZNAKA;
        words.add(wo);
    }

    public static Paragraph oneLine(TextLine line) {
        List<TextLine> lines = new ArrayList<>();
        lines.add(line);
        List<Paragraph> r = new PtextToKorpus(lines, true).paragraphs;
        switch (r.size()) {
        case 0:
            return null;
        case 1:
            return r.get(0);
        default:
            throw new RuntimeException();
        }
    }

    private void flushSentence() {
        if (words.isEmpty()) {
            return;
        }
        Sentence s = new Sentence();
        s.words = words.toArray(new Word[words.size()]);
        words.clear();
        sentences.add(s);
    }

    private void flushParagraph() {
        flushSentence();
        if (sentences.isEmpty()) {
            return;
        }
        Paragraph p = new Paragraph();
        p.page = page;
        p.sentences = sentences.toArray(new Sentence[sentences.size()]);
        sentences.clear();
        paragraphs.add(p);
    }
}
