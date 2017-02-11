import { GrammarLetter } from './GrammarInitial';

export class WordRequest {
    public word: string = "";
    public allForms: boolean;
    public grammar: string = null;

    clone(): WordRequest {
      let r = new WordRequest();
      r.word = this.word;
      r.allForms = this.allForms;
      r.grammar = this.grammar;
      return r;
    }

    public display: string = '---';

	updateDisplay(grammarTree: { [key:string]:GrammarLetter; }) {
	    if (this.grammar) {
	      let p: string = this.grammar.charAt(0);
	      this.display = grammarTree[p].desc;
	      let v=/^\.*$/.test(this.grammar.substring(1));
	      if (!/^\.*$/.test(this.grammar.substring(1))) {
	        this.display += ', ...';
	      }
	    } else {
	      this.display = '---';
	    }
	}
}
