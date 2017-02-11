import { WordResult } from '../dto/WordResult';

/**
 * Mathematics for word position processing in text(paragraph).
 */
export class TextPos {
    constructor(public text: WordResult[][], public sentence: number, public word: number) {}

    addWords(count: number): TextPos {
        let r: TextPos = new TextPos(this.text, this.sentence, this.word);
        if (count >= 0) {
            for (let i:number = 0; i < count; i++) {
                r.nextWord();
            }
        } else {
            for (let i:number = 0; i < -count; i++) {
                r.prevWord();
            }
        }
        return r;
    }

    addPos(count: number): TextPos {
        let r: TextPos = new TextPos(this.text, this.sentence, this.word);
        r.word += count;
        r.normalize();
        return r;
    }

    addSequences(count: number): TextPos {
        let newSentence: number = this.sentence + count;

        let newWord: number;
        if (count >= 0) {
            if (newSentence >= this.text.length) {
                newSentence = this.text.length - 1;
            }
            newWord = this.text[newSentence].length - 1;
        } else {
            if (newSentence < 0) {
                newSentence = 0;
            }
            newWord = 0;
        }
        return new TextPos(this.text, newSentence, newWord);
    }

    nextWord() {
        this.word++;
        for (; this.sentence < this.text.length; this.sentence++) {
            for (; this.word < this.text[this.sentence].length; this.word++) {
                if (this.text[this.sentence][this.word].isWord) {
                    return;
                }
            }
            this.word = 0;
        }
        this.sentence = this.text.length - 1;
        this.word = this.text[this.sentence].length - 1;
    }

    prevWord() {
        this.word--;
        for (; this.sentence >= 0; this.sentence--) {
            for (; this.word >= 0; this.word--) {
                if (this.text[this.sentence][this.word].isWord) {
                    return;
                }
            }
            if (this.sentence > 0) {
                this.word = this.text[this.sentence - 1].length - 1;
            }
        }
        this.sentence = 0;
        this.word = 0;
    }

    normalize() {
        while (true) {
            if (this.sentence >= 0 && this.sentence < this.text.length && this.word >= 0
                    && this.word < this.text[this.sentence].length) {
                return;
            }
            if (this.sentence < 0) {
                this.sentence = 0;
                this.word = 0;
                return;
            } else if (this.sentence >= this.text.length) {
                this.sentence = this.text.length - 1;
                this.word = this.text[this.sentence].length - 1;
                return;
            }
            if (this.word >= this.text[this.sentence].length) {
                this.word -= this.text[this.sentence].length;
                this.sentence++;
                continue;
            } else if (this.word < 0) {
                this.sentence--;
                if (this.sentence >= 0) {
                    this.word += this.text[this.sentence].length;
                }
                continue;
            }
            throw new Error("Error TextPos.normalize");
        }
    }

    equals(o: TextPos): boolean {
        return this.text == o.text && this.sentence == o.sentence && this.word == o.word;
    }

    after(o: TextPos): boolean {
        if (this.sentence > o.sentence) {
            return true;
        } else if (this.sentence == o.sentence && this.word > o.word) {
            return true;
        }
        return false;
    }

    static min(o1: TextPos, o2: TextPos): TextPos {
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

    static max(o1: TextPos, o2: TextPos): TextPos {
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
