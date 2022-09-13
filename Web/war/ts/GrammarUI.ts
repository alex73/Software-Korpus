class GrammarUI {

  showExtended: boolean;
  responseReversed: boolean;
  responseHasDuplicateParadigms: boolean;

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
		r.word = (<HTMLInputElement>document.getElementById('grammarword-word')).value.toLowerCase();
		if ($('#grammar-show-grammardetails').is(":visible")) {
			r.grammar = document.getElementById("grammarword-grammar").innerText;
		}
		if ($('#grammar-show-order').is(":visible")) {
			r.orderReverse = (<HTMLInputElement>document.getElementById("inputOrderReverse")).checked;
		}
		if ($('#grammar-show-forms').is(":visible")) {
			r.outputGrammar = document.getElementById("grammarwordshow-grammar").innerText;
		}
		if ($('#grammar-show-grouping').is(":visible")) {
			r.outputGrouping = (<HTMLInputElement>document.getElementById("inputGrouping")).checked;
		}
        r.fullDatabase = (<HTMLInputElement>document.getElementById("grammarword-full")).checked;
		return r;
	}
	restoreToScreen(data: GrammarRequest) {
		if (data) {
			(<HTMLInputElement>document.getElementById("grammarword-word")).value = data.word ? data.word : "";
			(<HTMLInputElement>document.getElementById("grammarword-multiform")).checked = data.multiForm;
			if (data.grammar) {
				document.getElementById("grammarword-grammar").innerText = data.grammar;
				DialogWordGrammar.wordGrammarToText(data.grammar, document.getElementById("grammarword-grammarshow"));
				this.showExtended = true;
			}
			if (data.outputGrammar) {
				document.getElementById("grammarwordshow-grammar").innerText = data.outputGrammar;
				DialogWordGrammar.wordGrammarToText(data.outputGrammar, document.getElementById("grammarwordshow-grammarshow"));
				this.showExtended = true;
			}
			if (data.orderReverse) {
				$("#inputOrderReverse").prop('checked', true);
				this.showExtended = true;
			}
            if (data.fullDatabase) {
                $('#grammar-show-all').show();
                $("#grammarword-full").prop('checked', true);
            }
			this.visibilityChange();
		}
		let gr = this;
		new MutationObserver(function(mutationsList, observer) {
		   document.getElementById('grammarwordshow-grammar').innerText = '';
		   document.getElementById('grammarwordshow-grammarshow').innerText = '---';
		   gr.visibilityChange();
		}).observe(document.getElementById('grammarword-grammar'), {childList: true});
		new MutationObserver(function(mutationsList, observer) {
		   gr.visibilityChange();
		}).observe(document.getElementById('grammarwordshow-grammar'), {childList: true});
	}
	showOutput(requestGrammar: string, reverse: boolean) {
		this.showExtended = true;
		this.responseHasDuplicateParadigms = grammarService.result.hasDuplicateParadigms;
		$('#output').html($.templates("#template-grammaroutput").render({
			lemmas: grammarService.result.output,
			reverse: reverse
		}));
		this.visibilityChange();
	}
	hideStatusError() {
		$('#status').hide();
		$('#error').hide();
	}
	resetControls() {
		this.showExtended = false;
		document.getElementById("grammarword-grammar").innerText = '';
		document.getElementById("grammarword-grammarshow").innerHTML = "---";
		$('#grammar-show-order').hide();
		$("#inputOrderStandard").prop('checked', true);
		$('#grammar-show-forms').hide();
		document.getElementById("grammarwordshow-grammar").innerText = '';
		document.getElementById("grammarword-grammarshow").innerHTML = "---";
		$('#grammar-show-grouping').hide();
		$('#grammar-show-noforms').hide();
		this.visibilityChange();
	}
	visibilityChange() {
		if (this.showExtended) {
			$('#grammar-show-order').show();
			$('#grammar-show-reset').show();
		}
		if (this.responseHasDuplicateParadigms) {
			$('#grammar-show-grouping').show();
		} else {
			$('#grammar-show-grouping').hide();
		}
		if ($('#grammar-show-grouping').is(":visible") && (<HTMLInputElement>document.getElementById("inputGrouping")).checked) {
			$('#grammar-show-order').hide();
		}
		let grammar: string = document.getElementById('grammarword-grammar').innerText;
		if ($('#grammar-show-grammardetails').is(":visible") && grammar && DialogWordGrammar.hasFormTags(grammar)) {
			$('#grammar-show-forms').show();
			$('#grammar-show-noforms').hide();
		} else if (this.showExtended) {
			$('#grammar-show-forms').hide();
			$('#grammar-show-noforms').show();
		}
	}
}

class OutGrammarParadigm {
    public lemma: string;
    public meaning: string;
    public variants: OutGrammarVariant[];
}

class OutGrammarVariant {
    public tag: string;
    public catText: KeyValue[];
    public subtree: { [key:string]:GrammarLetter; };
    public forms: OutGrammarForm[];
    public catnames: string[];
    public sourceForms: LemmaForm[];
    public sourceFormsUsageCount: number[];
    public dictionaries: GrammarDict[];
    public authors: Author[];
    public authorsOtherCount: number;
    public authorsOtherList: string;
}

class OutGrammarForm {
    public tag: string;
    public value: string;
    public data: string[];
    public colspan: number[];
}

var grammarui: GrammarUI = null;
var grammarService: GrammarService = null;
var localization: { [key:string]: string } = null;
var dialogGrammarDB: DialogGrammarDB = null;
function initializeGrammarPage() {
	grammarui = new GrammarUI();
	grammarService = new GrammarService();
    document.onkeydown = function(e) {
        if (e.key == 'F2') {
            $('#grammar-show-all').show();
        }
    };
}
