class WordResult {
  public lightNormalized: string;
  public tail: string;
  public lemmas: string;
  public dbTags: string;
  public requestedWord: boolean;
  public isWord: boolean;

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
