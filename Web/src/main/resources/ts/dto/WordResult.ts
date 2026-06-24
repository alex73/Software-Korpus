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

// See org.alex73.korpus.text.structure.corpus.Paragraph.
class Paragraph {
  public page: number;
  public lang: string;
  public previewLink: string;
  public sourceLink: string;
  public sentences: Sentence[];
}
