class BaseParams {
    public corpusType: string = "MAIN"; // MAIN, UNSORTED
    public textStandard: StandardTextRequest = new StandardTextRequest();
    public textUnprocessed: UnprocessedTextRequest = new UnprocessedTextRequest();

    importParameters(ps: string) {
      if (ps) {
          for (let p of ps.split("&")) {
            let pos: number = p.indexOf('=');
            if (pos > 0) {
                let key: string = p.substring(0, pos);
                let value: string = p.substring(pos + 1);
                this.importParameter(key, decodeURIComponent(value));
            }
          }
      }
    }
    importParameter(key: string, value: string) {
        if (key == "corpusType") {
          this.corpusType = value;
        }
        this.textStandard.importParameters(key, value);
        this.textUnprocessed.importParameters(key, value);
    }
    exportParameters(type: string): string {
        let out: string = "";

        switch(type) {
        case "search":
          out += "search";
          break;
        case "kwic":
          out += "kwic";
          break;
        case "cluster":
          out += "cluster";
          break;
        }

        out += this.out("corpusType", this.corpusType);
        out += this.textStandard.exportParameters();
        out += this.textUnprocessed.exportParameters();

        return out;
    }
	out(name: string, txt: any): string {
	    return txt ? '&'+name+'='+encodeURIComponent(txt) : "";
	}
}

class SearchParams extends BaseParams {
    public words: WordRequest[] = [];
    public wordsOrder: string = "PRESET"; // "PRESET" | "ANY_IN_SENTENCE" | "ANY_IN_PARAGRAPH";

    getWord(n): WordRequest {
      let i: number = parseInt(n);
      while(this.words.length < i+1) {
        this.words.push(new WordRequest());
      }
      return this.words[i];
    }

    importParameter(key: string, value: string) {
      super.importParameter(key, value);
      
      switch(key) {
        case "wordsOrder":
          this.wordsOrder = value;
          break;
        default:
          let m;
          if (m = /w([0-9]+)\.word/.exec(key)) {
            this.getWord(m[1]).word = value;
          } else if (m = /w([0-9]+)\.allForms/.exec(key)) {
            this.getWord(m[1]).allForms = value === 'true';
          } else if (m = /w([0-9]+)\.grammar/.exec(key)) {
            this.getWord(m[1]).grammar = value;
          }
          break;
      }
    }
    
    exportParameters(type: string): string {
        let out: string = super.exportParameters(type);

        switch(type) {
        case "search":
          out += this.out("wordsOrder", this.wordsOrder);
          break;
        }

        // words
        for (let i = 0; i < this.words.length; i++) {
            let w:WordRequest = this.words[i];
            out += this.out("w" + i + ".word", w.word);
            out += this.out("w" + i + ".allForms", w.allForms);
            out += this.out("w" + i + ".grammar", w.grammar);
        }
        return out;
    }
}

class ClusterParams extends BaseParams {
    public word: WordRequest = new WordRequest();
    public wordsBefore: number = 1;
    public wordsAfter: number = 1;

    exportParameters(): string {
        let out: string = super.exportParameters("cluster");

        out += this.out("w.word", this.word.word);
        out += this.out("w.allForms", this.word.allForms);
        out += this.out("w.grammar", this.word.grammar);

        if (this.wordsBefore != 1) {
          out += this.out("w.wordsBefore", this.wordsBefore);
        }
        if (this.wordsAfter != 1) {
          out += this.out("w.wordsAfter", this.wordsAfter);
        }
  
        return out;
    }

    importParameter(key: string, value: string) {
      super.importParameter(key, value);
      
      switch(key) {
        case "w.word":
          this.word.word = value;
          break;
        case "w.allForms":
          this.word.allForms = value === 'true';
          break;
        case "w.grammar":
          this.word.grammar = value;
          break;
        case "w.wordsBefore":
          this.wordsBefore = parseInt(value);
          break;
        case "w.wordsAfter":
          this.wordsAfter = parseInt(value);
          break;
      }
    }
}
