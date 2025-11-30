/**
 * Mathematics for word position processing in text(paragraph).
 */
class TextPos {
	constructor(public text: Paragraph, public sentence: number, public word: number) { }

	addWords(count: number): TextPos {
		let r: TextPos = new TextPos(this.text, this.sentence, this.word);
		if (count >= 0) {
			for (let i: number = 0; i < count; i++) {
				r.nextWord();
			}
		} else {
			for (let i: number = 0; i < -count; i++) {
				r.prevWord();
			}
		}
		return r;
	}

	addSequences(count: number): TextPos {
		let newSentence: number = this.sentence + count;

		let newWord: number;
		if (count >= 0) {
			if (newSentence >= this.text.sentences.length) {
				newSentence = this.text.sentences.length - 1;
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

	private nextWord() {
		if (this.word + 1 < this.text.sentences[this.sentence].words.length) {
			this.word++;
		} else if (this.sentence + 1 < this.text.sentences.length) {
			this.sentence++;
			this.word = 0;
		}
	}

	private prevWord() {
		if (this.word>0) {
			this.word--;
		}else if (this.sentence>0) {
			this.sentence--;
			this.word=this.text.sentences[this.sentence].words.length-1;
		}
	}

	private normalize() {
		while (true) {
			if (this.sentence >= 0 && this.sentence < this.text.sentences.length && this.word >= 0
				&& this.word < this.text.sentences[this.sentence].words.length) {
				return;
			}
			if (this.sentence < 0) {
				this.sentence = 0;
				this.word = 0;
				return;
			} else if (this.sentence >= this.text.sentences.length) {
				this.sentence = this.text.sentences.length - 1;
				this.word = this.text.sentences[this.sentence].words.length - 1;
				return;
			}
			if (this.word >= this.text.sentences[this.sentence].words.length) {
				this.word -= this.text.sentences[this.sentence].words.length;
				this.sentence++;
				continue;
			} else if (this.word < 0) {
				this.sentence--;
				if (this.sentence >= 0) {
					this.word += this.text.sentences[this.sentence].words.length;
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
