package org.alex73.korpus.text.parser;

import java.util.ArrayList;
import java.util.List;

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

public class PtextToKorpus {
    enum BLOCK_MODE {
        ONE_LINE, POETRY
    };

    public final List<Paragraph> paragraphs = new ArrayList<>();
    private final List<Sentence> sentences = new ArrayList<>();
    private final List<Word> words = new ArrayList<>();
    private BLOCK_MODE mode = BLOCK_MODE.ONE_LINE;
    private int page;

    public PtextToKorpus(List<TextLine> lines) {
        for (TextLine line : lines) {
            if (line.isEmpty()) {
                switch (mode) {
                case ONE_LINE:
                    continue;
                case POETRY:
                    flushParagraph();
                    continue;
                default:
                    break;
                }
            }
            for (ITextLineElement w : line) {
                if (w instanceof InlineTag) {
                } else if (w instanceof LongTagItem) {
                    if ("##Poetry:begin".equals(w.getText())) {
                        mode = BLOCK_MODE.POETRY;
                    } else if ("##Poetry:end".equals(w.getText())) {
                        flushParagraph();
                        mode = BLOCK_MODE.ONE_LINE;
                    } else if (w.getText().startsWith("##Page:")) {
                        flushParagraph();
                        page = Integer.parseInt(w.getText().substring(7).trim());
                    }
                } else if (w instanceof SentenceSeparatorItem) {
                    flushSentence();
                } else if (w instanceof WordItem) {
                    WordItem wi = (WordItem) w;
                    Word wo = new Word();
                    wo.lightNormalized = wi.lightNormalized;
                    wo.lemmas = wi.manualLemma != null ? wi.manualLemma : wi.lemmas;
                    wo.tags = wi.manualTag != null ? wi.manualTag : wi.tags;
                    wo.type = wi.type;
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
                        wo.lightNormalized = "";
                        wo.tail = w.getText();
                        words.add(wo);
                    }
                }
            }
        }
        flushParagraph();
    }

    public static Paragraph oneLine(TextLine line) {
        List<TextLine> lines = new ArrayList<>();
        lines.add(line);
        List<Paragraph> r = new PtextToKorpus(lines).paragraphs;
        switch (r.size()) {
        case 0:
            return new Paragraph();
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
