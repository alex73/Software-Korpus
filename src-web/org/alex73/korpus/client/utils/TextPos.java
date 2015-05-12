package org.alex73.korpus.client.utils;

import org.alex73.korpus.shared.dto.ResultText;

/**
 * Mathematics for word position processing in text(paragraph).
 */
public class TextPos {
    private final ResultText text;
    private int sentence;
    private int word;

    public TextPos(ResultText text, int sentence, int word) {
        this.text = text;
        this.sentence = sentence;
        this.word = word;
    }

    public int getSentence() {
        return sentence;
    }

    public int getWord() {
        return word;
    }

    public TextPos addWords(int count) {
        TextPos r = new TextPos(text, sentence, word);
        if (count >= 0) {
            for (int i = 0; i < count; i++) {
                r.nextWord();
            }
        } else {
            for (int i = 0; i < -count; i++) {
                r.prevWord();
            }
        }
        return r;
    }

    public TextPos addPos(int count) {
        TextPos r = new TextPos(text, sentence, word);
        r.word += count;
        r.normalize();
        return r;
    }

    public TextPos addSequences(int count) {
        int newSentence = sentence + count;

        int newWord;
        if (count >= 0) {
            if (newSentence >= text.words.length) {
                newSentence = text.words.length - 1;
            }
            newWord = text.words[newSentence].length - 1;
        } else {
            if (newSentence < 0) {
                newSentence = 0;
            }
            newWord = 0;
        }
        return new TextPos(text, newSentence, newWord);
    }

    private void nextWord() {
        word++;
        for (; sentence < text.words.length; sentence++) {
            for (; word < text.words[sentence].length; word++) {
                if (text.words[sentence][word].isWord) {
                    return;
                }
            }
            word = 0;
        }
        sentence = text.words.length - 1;
        word = text.words[sentence].length - 1;
    }

    private void prevWord() {
        word--;
        for (; sentence >= 0; sentence--) {
            for (; word >= 0; word--) {
                if (text.words[sentence][word].isWord) {
                    return;
                }
            }
            if (sentence > 0) {
                word = text.words[sentence - 1].length - 1;
            }
        }
        sentence = 0;
        word = 0;
    }

    protected void normalize() {
        while (true) {
            if (sentence >= 0 && sentence < text.words.length && word >= 0
                    && word < text.words[sentence].length) {
                return;
            }
            if (sentence < 0) {
                sentence = 0;
                word = 0;
                return;
            } else if (sentence >= text.words.length) {
                sentence = text.words.length - 1;
                word = text.words[sentence].length - 1;
                return;
            }
            if (word >= text.words[sentence].length) {
                word -= text.words[sentence].length;
                sentence++;
                continue;
            } else if (word < 0) {
                sentence--;
                if (sentence >= 0) {
                    word += text.words[sentence].length;
                }
                continue;
            }
            throw new RuntimeException("Error TextPos.normalize");
        }
    }

    @Override
    public String toString() {
        return sentence + "," + word;
    }

    @Override
    public boolean equals(Object obj) {
        TextPos o = (TextPos) obj;
        return text == o.text && sentence == o.sentence && word == o.word;
    }

    public boolean after(TextPos o) {
        if (sentence > o.sentence) {
            return true;
        } else if (sentence == o.sentence && word > o.word) {
            return true;
        }
        return false;
    }

    public static TextPos min(TextPos o1, TextPos o2) {
        if (o1.sentence < o2.sentence) {
            return o1;
        }
        if (o1.sentence > o2.sentence) {
            return o2;
        }
        if (o1.word < o2.word) {
            return o1;
        } else {
            return o2;
        }
    }

    public static TextPos max(TextPos o1, TextPos o2) {
        if (o1.sentence > o2.sentence) {
            return o1;
        }
        if (o1.sentence < o2.sentence) {
            return o2;
        }
        if (o1.word > o2.word) {
            return o1;
        } else {
            return o2;
        }
    }
}
