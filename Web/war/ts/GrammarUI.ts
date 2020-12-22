class GrammarUI {

 // public details: OutGrammarParadigm;

  constructor() {
    this.hideStatusError();
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
  collectFromScreen(): GrammarRequest {
    let r: GrammarRequest = new GrammarRequest();

    r.multiForm = (<HTMLInputElement>document.getElementById('grammarword-multiform')).checked;
    r.word = (<HTMLInputElement>document.getElementById('grammarword-word')).value;
    r.grammar = document.getElementById("grammarword-grammar").innerText;
    r.orderReverse = (<HTMLInputElement>document.getElementById("inputOrderReverse")).checked;
    r.outputGrammar = document.getElementById("grammarwordshow-grammar").innerText;
    return r;
  }
  showOutput(requestGrammar: string) {
		$('#output').html($.templates("#template-grammaroutput").render({
			lemmas: grammarService.results
		}));
		if (grammarService.results.length > 0) {
			$('#grammar-addition-order').show();
			if (requestGrammar && DialogWordGrammar.hasFormTags(requestGrammar)) {
				$('#grammar-show-forms').show();
				$('#grammar-show-noforms').hide();
			} else {
				$('#grammar-show-forms').hide();
				$('#grammar-show-noforms').show();
			}
		}
	}
  restoreToScreen(data: GrammarRequest) {
    if (data && data) {
      (<HTMLInputElement>document.getElementById("grammarword-word")).value = data.word ? data.word : "";
      (<HTMLInputElement>document.getElementById("grammarword-multiform")).checked = data.multiForm;
      document.getElementById("grammarword-grammar").innerText = data.grammar ? data.grammar : "";
      DialogWordGrammar.wordGrammarToText(data.grammar, document.getElementById("grammarword-grammarshow"));
      document.getElementById("grammarwordshow-grammar").innerText = data.outputGrammar ? data.outputGrammar : "";
      DialogWordGrammar.wordGrammarToText(data.outputGrammar, document.getElementById("grammarwordshow-grammarshow"));
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
