class SearchResults {
    public doc: TextInfo;
    public text: SearchResultsText;
    
    constructor(o:any) {
      this.doc = o.doc;
      this.text = new SearchResultsText();
      this.text.words = o.text.words;
    }
}
class SearchResultsText {
    public words: WordResult[][]; // paragraph is array of sentences, i.e. of array of words
}

class ResultKwicOutRow {
    public doc: TextInfo;
    public origText: SearchResultsText;
    public kwicBefore: WordResult[];
    public kwicWords: WordResult[];
    public kwicAfter: WordResult[];
    constructor(o:SearchResults) {
      this.doc = o.doc;
      this.origText = o.text;
    }
}

class ResultKwicOut {
    public rows: ResultKwicOutRow[] = [];
    
    constructor(o:any[], wordsInRequest:number) {
      for(let orow of o) {
        let r: SearchResults = new SearchResults(orow);

        for(let i=0; i<r.text.words.length; i++) {
          for(let j=0; j<r.text.words[i].length; j++) {
            if (r.text.words[i][j].requestedWord && j + wordsInRequest < r.text.words[i].length) {
                // show
                let row: ResultKwicOutRow = new ResultKwicOutRow(r);
                let wordsTo:number = this.showRow(row, r.text.words[i], j, wordsInRequest);
                this.rows.push(row);
                j = wordsTo;
            }
          }
        }
      }
    }

    showRow(out: ResultKwicOutRow, sentence: WordResult[], wordsFrom: number, wordsCount: number): number {
      let wordsTo: number = wordsFrom;

      out.kwicBefore = [];
      for (let i = wordsFrom - 1, count = 0; i >= 0 && count < 5; i--) {
        out.kwicBefore.push(sentence[i]);
        if (sentence[i].isWord) {
          count++;
        }
      }
      out.kwicBefore.reverse();
      
      out.kwicWords = [];
      for (let i = wordsFrom, count = 0; i < sentence.length && count < wordsCount; i++) {
        out.kwicWords.push(sentence[i]);
        if (sentence[i].isWord) {
          count++;
          wordsTo = i;
        }
      }
      
      out.kwicAfter = [];
      for (let i = wordsTo + 1, count = 0; i < sentence.length && count < 5; i++) {
        out.kwicAfter.push(sentence[i]);
        if (sentence[i].isWord) {
          count++;
        }
      }

      return wordsTo;
    }
}

class ResultSearchOutRow {
    public doc: TextInfo;
    public origText: SearchResultsText;
    public words: WordResult[] = [];
    constructor(o:SearchResults) {
      this.doc = o.doc;
      this.origText = o.text;
    }
}

class ResultSearchOut {
    public rows: ResultSearchOutRow[] = [];
    
    constructor(o:any[], wordsInRequest:number) {
      for(let orow of o) {
        let r: SearchResults = new SearchResults(orow);
        
        let num = this.getRequestedWordsCountInResult(r.text.words);
        let wordsCount;
        switch(num) {
          case 0:
          case 1:
              wordsCount = 7;
              break;
          case 2:
              wordsCount = 5;
              break;
          case 3:
              wordsCount = 4;
              break;
          default:
              wordsCount = 3;
              break;
        }
        let out: ResultSearchOutRow = new ResultSearchOutRow(r);
        this.outputText(r.text.words, out, wordsCount, 0, " ... ");
        this.rows.push(out);
      }
    }

    outputText(words: WordResult[][], row: ResultSearchOutRow, wordAround:number, sentencesAround:number, separatorText:string) {
        let begin: TextPos = new TextPos(words, 0, 0);
        let end: TextPos = new TextPos(words, words.length - 1, words[words.length - 1].length - 1);

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
    
    output(words: WordResult[][], row: ResultSearchOutRow, from: TextPos, to: TextPos) {
        let curr: TextPos = from;
        while (true) {
            let w: WordResult = words[curr.sentence][curr.word];
            if (!w.isWord && w.orig && w.orig.charAt(0) == '\n') {
                row.words.push(new WordResult(" \\\\ ")); // TODO
            } else {
                row.words.push(w); 
            }
            let next: TextPos = curr.addPos(1);
            if (curr.equals(to)) {
                break;
            }
            curr = next;
        }
    }
    
    getRequestedWordsCountInResult(words: WordResult[][]): number {
        let count: number = 0;

        for (let row of words) {
            for (let w of row) {
                if (w.requestedWord) {
                    count++;
                }
            }
        }
        return count;
    }
    getNextRequestedWordPosAfter(words: WordResult[][], currentPos: TextPos): TextPos {
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
        for (let i: number = startI; i < words.length; i++) {
            for (; j < words[i].length; j++) {
                if (words[i][j].requestedWord) {
                    return new TextPos(words, i, j);
                }
            }
            j = 0;
        }
        return null;
    }
}

class LatestMark {
    public score: number = null;
    public doc: number = null;
    public shardIndex: number = null;
}

class TextInfo {
    public url: string;
    public subcorpus: string;
    public authors: string[];
    public title: string;
    public styleGenres: string[];
    public writtenYear: number;
    public publishedYear: number;
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
