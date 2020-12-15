declare var $: any;

class KorpusUI {
	constructor() {
		this.hideStatusError();
	}
	getMode(): string {
		if (document.getElementById("tabKWIC").classList.contains('active')) {
			return 'kwic';
		}
		if (document.getElementById("tabCluster").classList.contains('active')) {
			return 'cluster';
		}
		return 'search';
	}
	switchMode(mode: string) {
		document.getElementById("tabSearch").classList.remove("active");
		document.getElementById("tabKWIC").classList.remove("active");
		document.getElementById("tabCluster").classList.remove("active");
		switch (mode) {
			case "search":
				document.getElementById("tabSearch").classList.add("active");
				document.getElementById("partInputOrder").classList.remove("d-none");
				document.getElementById("partWordAdd").classList.remove("d-none");
				document.getElementById("partWordAround").classList.add("d-none");
				break;
			case "kwic":
				document.getElementById("tabKWIC").classList.add("active");
				document.getElementById("partInputOrder").classList.add("d-none");
				document.getElementById("partWordAdd").classList.remove("d-none");
				document.getElementById("partWordAround").classList.add("d-none");
				break;
			case "cluster":
				document.getElementById("tabCluster").classList.add("active");
				document.getElementById("partInputOrder").classList.add("d-none");
				document.getElementById("partWordAdd").classList.add("d-none");
				document.getElementById("partWordAround").classList.remove("d-none");
				let words: NodeListOf<HTMLElement> = this.getWordCollection('inputword');
				for (let i = 1; i < words.length; i++) {
					words.item(i).remove();
				}
				this.repaintRemoveIcons('inputword');
				break;
		}
		this.maximizeHeader();
		korpusService.pages = null;
		korpusService.currentPage = 0;
		korpusService.resultCluster = null;
		korpusService.resultKwic = null;
		korpusService.resultSearch = null;
		this.showOutput();
	}
	addWord(type: string): HTMLElement {
		const templateWord: HTMLElement = document.getElementById("template-" + type);
		const newWord: HTMLElement = <HTMLElement>templateWord.cloneNode(true);
		newWord.removeAttribute('id');
		newWord.style.display = 'block';
		let r = templateWord.parentElement.insertBefore(newWord, templateWord);
		this.repaintRemoveIcons(type);
		return r;
	}
	removeWord(type: string, button: HTMLElement) {
		const block: HTMLElement = button.closest("." + type);
		block.remove();
		this.repaintRemoveIcons(type);
	}
	getWordCollection(type: string): NodeListOf<HTMLElement> {
		return document.querySelectorAll("." + type + ":not(#template-" + type + ")");
	}
	repaintRemoveIcons(type: string) {
		var collection: NodeListOf<HTMLElement> = document.querySelectorAll("." + type + ":not(#template-" + type + ")");
		collection.forEach(el => (<NodeListOf<HTMLElement>>el.querySelectorAll("." + type + "-remove")).forEach(er =>
			er.style.display = collection.length > 1 ? 'block' : 'none'));
	}
	static lemmaChange(type: string, cb: HTMLInputElement) {
		cb.closest('.' + type).querySelector("." + type + "-lemma-prompt").textContent = cb.checked ? "Лема" : "Слова";
	}
	hideStatusError() {
		$('#status').hide();
		$('#error').hide();
	}
	showError(err: string) {
		console.log("Error: " + err);
		$('#status').hide();
		document.getElementById("error").innerText = err;
		$('#error').show();
		$('#output').html('');
	}
	showStatus(s: string) {
		$('#error').hide();
		document.getElementById("status").innerText = s;
		$('#status').show();
	}
	private collectFromScreenBase(p: BaseParams) {
		p.textStandard.subcorpuses = this.separatedStringToArray(document.getElementById('inputFilterCorpus').innerText);
		p.textStandard.authors = this.separatedStringToArray(document.getElementById('inputFilterAuthor').innerText);
		p.textStandard.stylegenres = this.separatedStringToArray(document.getElementById('inputFilterStyle').innerText);
		p.textStandard.yearWrittenFrom = fulltrim((<HTMLInputElement>document.getElementById('inputFilterYearWrittenFrom')).value);
		p.textStandard.yearWrittenTo = fulltrim((<HTMLInputElement>document.getElementById('inputFilterYearWrittenTo')).value);
		p.textStandard.yearPublishedFrom = fulltrim((<HTMLInputElement>document.getElementById('inputFilterYearPublishedFrom')).value);
		p.textStandard.yearPublishedTo = fulltrim((<HTMLInputElement>document.getElementById('inputFilterYearPublishedTo')).value);
	}
	collectFromScreenSearch(): SearchParams {
		let requestedParams: SearchParams = new SearchParams();
		this.collectFromScreenBase(requestedParams);
		requestedParams.words = [];
		this.getWordCollection('inputword').forEach(w => {
			let wrq = new WordRequest();
			wrq.allForms = (<HTMLInputElement>w.querySelector("input[type='checkbox']")).checked;
			wrq.word = fulltrim((<HTMLInputElement>w.querySelector("input[type='text']")).value);
			wrq.grammar = fulltrim((<HTMLElement>w.querySelector(".wordgram-grammar-string")).innerText);
			requestedParams.words.push(wrq);
		});
		if (this.getMode() == 'search') {
			requestedParams.wordsOrder = (<HTMLInputElement>document.querySelector("#part-wordorder input[type='radio']:checked")).value;
		}
		return requestedParams;
	}
	collectFromScreenCluster(): ClusterParams {
		let requestedParams: ClusterParams = new ClusterParams();
		this.collectFromScreenBase(requestedParams);
		this.getWordCollection('inputword').forEach(w => {
			let wrq = new WordRequest();
			wrq.allForms = (<HTMLInputElement>w.querySelector("input[type='checkbox']")).checked;
			wrq.word = fulltrim((<HTMLInputElement>w.querySelector("input[type='text']")).value);
			wrq.grammar = fulltrim((<HTMLElement>w.querySelector(".wordgram-grammar-string")).innerText);
			requestedParams.word = wrq;
		});
		requestedParams.wordsBefore = parseInt((<HTMLInputElement>document.getElementById("inputClusterBefore")).value);
		requestedParams.wordsAfter = parseInt((<HTMLInputElement>document.getElementById("inputClusterAfter")).value);

		return requestedParams;
	}
	restoreToScreen(mode: string, data: BaseParams) {
		this.switchMode(mode);
		(<HTMLInputElement>document.getElementById('inputFilterYearWrittenFrom')).value = data && data.textStandard && data.textStandard.yearWrittenFrom ? data.textStandard.yearWrittenFrom : "";
		(<HTMLInputElement>document.getElementById('inputFilterYearWrittenTo')).value = data && data.textStandard && data.textStandard.yearWrittenTo ? data.textStandard.yearWrittenTo : "";
		(<HTMLInputElement>document.getElementById('inputFilterYearPublishedFrom')).value = data && data.textStandard && data.textStandard.yearPublishedFrom ? data.textStandard.yearPublishedFrom : "";
		(<HTMLInputElement>document.getElementById('inputFilterYearPublishedTo')).value = data && data.textStandard && data.textStandard.yearPublishedTo ? data.textStandard.yearPublishedTo : "";
		document.getElementById('inputFilterCorpus').innerText = data && data.textStandard && data.textStandard.subcorpuses ? data.textStandard.subcorpuses.join(';') : "Усе";
		document.getElementById('inputFilterAuthor').innerText = data && data.textStandard && data.textStandard.authors ? data.textStandard.authors.join(';') : "Усе";
		document.getElementById('inputFilterStyle').innerText = data && data.textStandard && data.textStandard.stylegenres ? data.textStandard.stylegenres.join(';') : "Усе";
		switch (this.getMode()) {
			case 'search':
			case 'kwic':
				let sp: SearchParams = <SearchParams>data;
				if (sp && sp.words) {
					sp.words.forEach(wdata => {
						let w = this.addWord('inputword');
						(<HTMLInputElement>w.querySelector("input[type='text']")).value = wdata.word ? wdata.word : "";
						(<HTMLInputElement>w.querySelector("input[type='checkbox']")).checked = wdata.allForms;
						(<HTMLElement>w.querySelector(".inputword-lemma-prompt")).textContent = wdata.allForms ? "Лема" : "Слова";
						(<HTMLElement>w.querySelector(".wordgram-grammar-string")).innerText = wdata.grammar ? wdata.grammar : "";
						(<HTMLElement>w.querySelector(".wordgram-display")).innerText = DialogWordGrammar.wordGrammarToText(wdata.grammar, korpusService.initial.grammar);
					});
				} else {
					this.addWord('inputword');
				}
				if (sp && sp.wordsOrder) {
					(<HTMLInputElement>document.querySelector("#part-wordorder input[type='radio'][value='" + sp.wordsOrder + "']")).checked = true;
				}
				break;
			case 'cluster':
				let cp: ClusterParams = <ClusterParams>data;
				if (cp && cp.word) {
					let w: HTMLElement = this.addWord('inputword');
					(<HTMLInputElement>w.querySelector("input[type='text']")).value = cp.word.word ? cp.word.word : "";
					(<HTMLInputElement>w.querySelector("input[type='checkbox']")).checked = cp.word.allForms;
					(<HTMLElement>w.querySelector(".inputword-lemma-prompt")).textContent = cp.word.allForms ? "Лема" : "Слова";
					(<HTMLElement>w.querySelector(".wordgram-grammar-string")).innerText = cp.word.grammar ? cp.word.grammar : "";
					(<HTMLElement>w.querySelector(".wordgram-display")).innerText = DialogWordGrammar.wordGrammarToText(cp.word.grammar, korpusService.initial.grammar);
				}
				(<HTMLInputElement>document.getElementById("inputClusterBefore")).value = cp.wordsBefore ? cp.wordsBefore.toString():"1";
				(<HTMLInputElement>document.getElementById("inputClusterAfter")).value = cp.wordsAfter ? cp.wordsAfter.toString():"1";
				break;
		}
	}
	showOutput() {
		$('#output').html($.templates("#template-output").render({
			pages: korpusService.pages,
			currentPage: korpusService.currentPage,
			hasMore: korpusService.hasMore,
			resultSearch: korpusService.resultSearch,
			resultKwic: korpusService.resultKwic,
			resultCluster: korpusService.resultCluster
		}));
	}
	separatedStringToArray(v: string): string[] {
		if (!v || v == 'Усе') {
			return null;
		}
		return v.split(';');
	}
	minimizeHeader() {
		document.getElementById('btnShowSearch').style.display = 'block';
		document.getElementById('divInputSearch').style.display = 'none';
	}
	maximizeHeader() {
		document.getElementById('btnShowSearch').style.display = 'none';
		document.getElementById('divInputSearch').style.display = 'block';
	}
    /*this.serviceSearch.minimizeKwic = () => {
      this.btnShowKwicRef.nativeElement.style.display = 'block';
      this.divInputKwicRef.nativeElement.style.display = 'none';
    };
    this.serviceSearch.maximizeKwic = () => {
      this.btnShowKwicRef.nativeElement.style.display = 'none';
      this.divInputKwicRef.nativeElement.style.display = 'block';
    };
    this.serviceSearch.minimizeCluster = () => {
      this.btnShowClusterRef.nativeElement.style.display = 'block';
      this.divInputClusterRef.nativeElement.style.display = 'none';
    };
    this.serviceSearch.maximizeCluster = () => {
      this.btnShowClusterRef.nativeElement.style.display = 'none';
      this.divInputClusterRef.nativeElement.style.display = 'block';
    };*/
}

$.views.settings.allowCode(true);
$.views.converters("json", function (val) {
	return JSON.stringify(val);
});
$.views.converters("titleShort", function (str) {
    let s = str.toString();
    if (s.length == 0) {
      return "<...>";
    } else if (s.length > 40) {
      return s.substring(0,37)+"...";
    } else {
      return s;
    }
});
$('body')
	.on('mousedown', '.popover', function (e) {
		e.preventDefault();
	});

var korpusui: KorpusUI = null;
var korpusService: KorpusService = null;
var dialogAuthors: DialogAuthors = null;
var dialogSubcorpuses: DialogSubcorpuses = null;
var dialogText: DialogText = null;
var dialogStyleGenres: DialogStyleGenres = null;
var dialogWordGrammar: DialogWordGrammar = null;
function initializeKorpusPage() {
	korpusui = new KorpusUI();
	korpusService = new KorpusService();
}
