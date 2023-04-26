class WordResult {
  public word: string;
  public tail: string;
  public requestedWord: boolean;

  constructor(private text: string) {
    this.word = text;
  }
  getOutput(): string {
    return this.word;
  }
}

class Sentence {
  public words: WordResult[];
}

class Paragraph {
  public page: number;
  public lang: string;
  public sentences: Sentence[];
}
