class WordResult {
  public lightNormalized: string;
  public tail: string;
  public lemmas: string;
  public tags: string;
  public requestedWord: boolean;

  constructor(private text: string) {
    this.lightNormalized = text;
  }
}

class Sentence {
  public words: WordResult[];
}

class Paragraph {
  public sentences: Sentence[];
}
