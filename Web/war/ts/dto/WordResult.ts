class WordResult {
  public source: string;
  public normalized: string;
  public tail: string;
  public lemmas: string;
  public tags: string;
  public requestedWord: boolean;

  constructor(private text: string) {
    this.normalized = text;
  }
  getOutput(): string {
    return this.source != null ? this.source : this.normalized;
  }
}

class Sentence {
  public words: WordResult[];
}

class Paragraph {
  public page: number;
  public sentences: Sentence[];
}
