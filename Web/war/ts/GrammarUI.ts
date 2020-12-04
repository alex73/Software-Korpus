class GrammarUI {

 // public details: OutGrammarParadigm;

  constructor() {
    this.hideStatusError();
    const w: HTMLElement = document.getElementById("template-inputword");
    w.style.display = 'block';
		(<HTMLElement>w.querySelector(".inputword-remove")).style.display = 'none';
	}
  showError(err: string) {
		console.log("Error: " + err);
		$('#status').hide();
		document.getElementById("error").innerText = err;
		$('#error').show();
	}
	showStatus(s: string) {
		$('#error').hide();
		document.getElementById("status").innerText = s;
		$('#status').show();
  }
  collectFromScreenCluster(): GrammarRequest {
    let r: GrammarRequest = new GrammarRequest();
    let w: HTMLElement = document.getElementById('template-inputword');
    r.word.allForms = (<HTMLInputElement>w.querySelector("input[type='checkbox']")).checked;
    r.word.word = (<HTMLInputElement>w.querySelector("input[type='text']")).value;
    r.word.grammar = (<HTMLElement>w.querySelector(".wordgram-grammar-string")).innerText;
    r.orderReverse = (<HTMLInputElement>document.getElementById("inputOrderReverse")).checked;
    return r;
  }
  showOutput() {
		$('#output').html($.templates("#template-grammaroutput").render({
			lemmas: grammarService.results
		}));
	}
  restoreToScreen(data: GrammarRequest) {
    if (data && data.word) {
      let w: HTMLElement = document.getElementById('template-inputword');
      (<HTMLInputElement>w.querySelector("input[type='text']")).value = data.word.word ? data.word.word : "";
      (<HTMLInputElement>w.querySelector("input[type='checkbox']")).checked = data.word.allForms;
      (<HTMLElement>w.querySelector(".inputword-lemma-prompt")).textContent = data.word.allForms ? "Лема" : "Слова";
      (<HTMLElement>w.querySelector(".wordgram-grammar-string")).innerText = data.word.grammar ? data.word.grammar : "";
      (<HTMLElement>w.querySelector(".wordgram-display")).innerText = DialogWordGrammar.wordGrammarToText(data.word.grammar, grammarService.initial);
    }
    if (data) {
      (<HTMLInputElement>document.getElementById(data.orderReverse ? "inputOrderReverse" : "inputOrderStandard")).checked = true;
    }
  }
  hideStatusError() {
		$('#status').hide();
		$('#error').hide();
	}
}

class OutGrammarParadigm {
    public lemma: string;
    public variants: OutGrammarVariant[];

    public catText: KeyValue[];
    public subtree: { [key:string]:GrammarLetter; };
}

class OutGrammarVariant {
    public forms: OutGrammarForm[];
    public catnames: string[];
}

class OutGrammarForm {
    public tag: string;
    public value: string;
    public data: string[];
    public colspan: number[];
}

var grammarui: GrammarUI = null;
var grammarService: GrammarService = null;
var dialogGrammarDB: DialogGrammarDB = null;
function initializeGrammarPage() {
	grammarui = new GrammarUI();
	grammarService = new GrammarService();
}
