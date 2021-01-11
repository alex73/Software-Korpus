class SearchResults {
    public docId: number;
    public doc: TextInfo;
    public text: Paragraph;
    
    constructor(o:any) {
      this.docId = o.docId;
      this.doc = o.doc;
      this.text = new Paragraph();
      this.text.sentences = o.text.sentences;
    }
}

class ResultKwicOutRow {
    public doc: TextInfo;
    public origText: Sentence;
    public kwicBefore: WordResult[];
    public kwicWords: WordResult[];
    public kwicAfter: WordResult[];
    constructor(o:SearchResults) {
      this.doc = o.doc;
      this.origText = o.text.sentences[0];
    }
}

class ResultKwicOut {
    public rows: ResultKwicOutRow[] = [];
    
    constructor(o:any[], wordsInRequest:number) {
      for(let orow of o) {
        let r: SearchResults = new SearchResults(orow);

        for(let i=0; i<r.text.sentences.length; i++) {
          for(let j=0; j<r.text.sentences[i].words.length; j++) {
            if (r.text.sentences[i].words[j].requestedWord && j + wordsInRequest < r.text.sentences[i].words.length) {
                // show
                let row: ResultKwicOutRow = new ResultKwicOutRow(r);
                let wordsTo:number = this.showRow(row, r.text.sentences[i], j, wordsInRequest);
                this.rows.push(row);
                j = wordsTo;
            }
          }
        }
      }
    }

    showRow(out: ResultKwicOutRow, sentence: Sentence, wordsFrom: number, wordsCount: number): number {
      let wordsTo: number = wordsFrom;

      out.kwicBefore = [];
      for (let i = wordsFrom - 1, count = 0; i >= 0 && count < 5; i--) {
        out.kwicBefore.push(sentence.words[i]);
        if (sentence.words[i].lightNormalized) {
          count++;
        }
      }
      out.kwicBefore.reverse();
      
      out.kwicWords = [];
      for (let i = wordsFrom, count = 0; i < sentence.words.length && count < wordsCount; i++) {
        out.kwicWords.push(sentence.words[i]);
        if (sentence.words[i].lightNormalized) {
          count++;
          wordsTo = i;
        }
      }
      
      out.kwicAfter = [];
      for (let i = wordsTo + 1, count = 0; i < sentence.words.length && count < 5; i++) {
        out.kwicAfter.push(sentence.words[i]);
        if (sentence.words[i].lightNormalized) {
          count++;
        }
      }

      return wordsTo;
    }
}

class ResultSearchOutRow {
    public docId: number;
    public doc: TextInfo;
    public origText: Paragraph;
    public words: WordResult[] = [];
    constructor(o:SearchResults) {
      this.docId = o.docId;
      this.doc = o.doc;
      this.origText = o.text;
    }
}

class ResultSearchOut {
    public rows: ResultSearchOutRow[] = [];
    
    constructor(o:any[], wordsInRequest:number) {
      for(let orow of o) {
        let r: SearchResults = new SearchResults(orow);
        
        let num = this.getRequestedWordsCountInResult(r.text);
        let wordsCount;
        switch(num) {
          case 0:
          case 1:
              wordsCount = 5;
              break;
          case 2:
              wordsCount = 3;
              break;
          case 3:
              wordsCount = 2;
              break;
          default:
              wordsCount = 2;
              break;
        }
        let out: ResultSearchOutRow = new ResultSearchOutRow(r);
        this.outputText(r.text, out, wordsCount, 0, " ... ");
        this.rows.push(out);
      }
    }

    outputText(words: Paragraph, row: ResultSearchOutRow, wordAround:number, sentencesAround:number, separatorText:string) {
        let begin: TextPos = new TextPos(words, 0, 0);
        let end: TextPos = new TextPos(words, words.sentences.length - 1, words.sentences[words.sentences.length - 1].words.length - 1);

        let pos: TextPos = this.getNextRequestedWordPosAfter(words, null);
        let currentAroundFrom: TextPos = pos.addWords(-wordAround);
        if (sentencesAround != 0) {
            currentAroundFrom = currentAroundFrom.addSequences(-sentencesAround);
        }
        let currentAroundTo: TextPos = pos.addWords(wordAround);
        if (sentencesAround != 0) {
            currentAroundTo = currentAroundTo.addSequences(sentencesAround);
        }

        if (currentAroundFrom.after(begin)) {
            row.words.push(new WordResult(separatorText));
        }

        while (true) {
            let next: TextPos = this.getNextRequestedWordPosAfter(words, pos);
            if (next == null) {
                break;
            }
            let nextAroundFrom: TextPos = next.addWords(-wordAround);
            if (sentencesAround != 0) {
                nextAroundFrom = nextAroundFrom.addSequences(-sentencesAround);
            }
            let nextAroundTo: TextPos = next.addWords(wordAround);
            if (sentencesAround != 0) {
                nextAroundTo = nextAroundTo.addSequences(sentencesAround);
            }

            if (currentAroundTo.addWords(2).after(nextAroundFrom)) {
                // merge
                currentAroundTo = nextAroundTo;
            } else {
                this.output(words, row, currentAroundFrom, currentAroundTo);
                row.words.push(new WordResult(separatorText));
                currentAroundFrom = nextAroundFrom;
                currentAroundTo = nextAroundTo;
            }
            pos = next;
        }
        this.output(words, row, currentAroundFrom, currentAroundTo);

        if (end.after(currentAroundTo)) {
            row.words.push(new WordResult(separatorText));
        }
    }

    output(words: Paragraph, row: ResultSearchOutRow, from: TextPos, to: TextPos) {
        let curr: TextPos = from;
        while (true) {
            let w: WordResult = words.sentences[curr.sentence].words[curr.word];
            row.words.push(w); 
            let next: TextPos = curr.addWords(1);
            if (curr.equals(to)) {
                break;
            }
            curr = next;
        }
    }

    getRequestedWordsCountInResult(words: Paragraph): number {
        let count: number = 0;

        for (let row of words.sentences) {
            for (let w of row.words) {
                if (w.requestedWord) {
                    count++;
                }
            }
        }
        return count;
    }
    getNextRequestedWordPosAfter(words: Paragraph, currentPos: TextPos): TextPos {
        let startI: number, startJ: number;
        if (currentPos == null) {
            startI = 0;
            startJ = 0;
        } else {
            let next: TextPos = currentPos.addWords(1);
            if (next.equals(currentPos)) {
                return null;
            }
            startI = next.sentence;
            startJ = next.word;
        }
        let j: number = startJ;
        for (let i: number = startI; i < words.sentences.length; i++) {
            for (; j < words.sentences[i].words.length; j++) {
                if (words.sentences[i].words[j].requestedWord) {
                    return new TextPos(words, i, j);
                }
            }
            j = 0;
        }
        return null;
    }
}

class LatestMark {
    public doc: number = null;
    public score: number = null;
    public fields: object[] = null;
    public shardIndex: number = null;
}

class TextInfo {
    public subcorpus: string;
    public source: string;
    public url: string;
    public authors: string[];
    public title: string;
    public translators: string[];
    public lang: string;
    public langOrig: string;
    public styleGenres: string[];
    public edition: string;
    public creationTime: string;
    public publicationTime: string;
}

class ClusterResult {
    public rows: ClusterRow[];
    constructor(r:ClusterRow[]) {
      this.rows = r;
    }
}

class ClusterRow {
    public wordsBefore: string[];
    public word: string;
    public wordsAfter: string[];
    public count: number;
}
