declare var $: any;

/*
  Паказваем:
    пошук у корпусе - ланцужкі
    KWIC - толькі адзін ланцужок
    кластар - пакуль як ёсць
 */
class KorpusUI {
	visitedList: number[];
	currentFindLanguage: string;
	chainsInParagraph: boolean;
	constructor(defaultLanguage: string) {
		this.currentFindLanguage = defaultLanguage;
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
		$('#tabSearch').removeClass("active");
		$('#tabKWIC').removeClass("active");
		$('#tabCluster').removeClass("active");
		$('#tabSpisy').removeClass("active");
		$('#divInputSearch').hide();
		$('#divSearch').hide();
		$('#divCluster').hide();
		$('#divSpisy').hide();
		switch (mode) {
			case "search":
				$('#tabSearch').addClass("active");
				$('#divInputSearch').show();
				$('#divSearch').show();
				$(".korpus-words-searchonly").show();
				break;
			case "kwic":
				$('#tabKWIC').addClass("active");
				$('#divInputSearch').show();
				$('#divSearch').show();
				$(".korpus-words-searchonly").hide();
				// remove more than one row
				while(true) {
					var collectionRows: NodeListOf<HTMLElement> = document.querySelectorAll("#divSearch .korpus-words-row-full");
					if (collectionRows.length<=2) {
						break;
					}
					collectionRows.item(collectionRows.length-1).remove();
				}
				break;
			case "cluster":
				$('#tabCluster').addClass("active");
				$('#divCluster').show();
				break;
			case "spisy":
				$('#tabSpisy').addClass("active");
				$('#divSpisy').show();
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
	switchLanguage(lang: string) {
		this.currentFindLanguage = lang;

		$('#parallel_'+lang).prop("checked", true);

		// remove all words
		if ($(".inputword:visible").length > 0) {
			$(".inputword:visible").remove();
			this.addRow(true);
		}
	}
	setWordMode(from: HTMLElement, mode: string) {
		const word: HTMLElement = from.closest(".word-select");
		$(word.querySelector('.word-store-mode')).text(mode);
		switch(mode) {
			case WordMode.USUAL:
				$(word.querySelector('.word-select-dropdown button')).text('Звычайны пошук');
				$(word.querySelector('.word-select-variants')).show();
				$(word.querySelector('.word-select-grammar')).show();
				$(word.querySelector('.word-select-word')).show();
				$(word.querySelector('.word-select-wordprompt')).attr('placeholder', "можна з '*' і '?'");
				$(word.querySelector('.word-select-regexp')).hide();
				break;
			case WordMode.ALL_FORMS:
				$(word.querySelector('.word-select-dropdown button')).text('Усе словаформы');
				$(word.querySelector('.word-select-variants')).show();
				$(word.querySelector('.word-select-grammar')).show();
				$(word.querySelector('.word-select-word')).show();
				$(word.querySelector('.word-select-wordprompt')).attr('placeholder', "адна з формаў");
				$(word.querySelector('.word-select-regexp')).hide();
				break;
			case WordMode.EXACT:
				$(word.querySelector('.word-select-dropdown button')).text('Дакладны пошук');
				$(word.querySelector('.word-select-variants')).hide();
				$(word.querySelector('.word-select-grammar')).hide();
				$(word.querySelector('.word-select-word')).show();
				$(word.querySelector('.word-select-wordprompt')).attr('placeholder', "можна з '*' і '?'");
				$(word.querySelector('.word-select-regexp')).show();
				break;
			case WordMode.GRAMMAR:
				$(word.querySelector('.word-select-dropdown button')).text('Толькі граматыка');
				$(word.querySelector('.word-select-variants')).show();
				$(word.querySelector('.word-select-grammar')).show();
				$(word.querySelector('.word-select-word')).hide();
				$(word.querySelector('.word-select-regexp')).hide();
				break;
		}
	}
	setSepMode(from: HTMLElement, mode: string) {
		const sep: HTMLElement = from.closest(".word-select");
		switch(mode) {
			case 'NONE':
				$(sep).find('input').prop('checked', false);
				$(sep).find("button.dropdown-toggle").text('не');
				$(sep).find('.sep-store-mode').text('NONE');
				return;
			case '':
				$(sep).find("button.dropdown-toggle").text('');
				$(sep).find('input').prop('checked', false);
				$(sep).find('.sep-store-mode').text('');
				break;
			default:
				var v = '';
				$(sep).find('input').each(function(index,data) {
					if (this.checked) {
						v += this.getAttribute('v');
					}
				});
				$(sep).find("button.dropdown-toggle").text(v);
				$(sep).find('.sep-store-mode').text(v);
				break;
		}
	}
	addWord(fullrow: HTMLElement): HTMLElement {
		const templateWord: HTMLElement = document.getElementById("template-inputword");
		const newWord: HTMLElement = <HTMLElement>templateWord.cloneNode(true);
		newWord.removeAttribute('id');
		newWord.style.display = 'block';
		let row = fullrow.querySelector('.korpus-words-row');
		let r = row.insertBefore(newWord, row.lastElementChild);
		this.setWordMode(newWord.querySelector('.word-store-mode'), WordMode.ALL_FORMS);
		this.repaintWordButtons();
		return r;
	}
	addSep(fullrow: HTMLElement): HTMLElement {
		const templateSep: HTMLElement = document.getElementById("template-inputsep");
		const newSep: HTMLElement = <HTMLElement>templateSep.cloneNode(true);
		newSep.removeAttribute('id');
		newSep.style.display = 'block';
		let row = fullrow.querySelector('.korpus-words-row');
		let r = row.insertBefore(newSep, row.lastElementChild);
		this.repaintSeps();
		return r;
	}
	addRow(initializeWords: boolean): HTMLElement {
		const templateRow: HTMLElement = document.getElementById("template-inputrow");
		const newRow: HTMLElement = <HTMLElement>templateRow.cloneNode(true);
		newRow.removeAttribute('id');
		newRow.style.display = '';
		let r = templateRow.parentElement.insertBefore(newRow, templateRow.parentElement.lastElementChild);
		if (initializeWords) {
			this.addSep(r);
			this.addWord(r);
			this.addSep(r);
		}
		return r;
	}
	removeWord(button: HTMLElement) {
		const block: HTMLElement = button.closest(".inputword");
		block.nextElementSibling.remove(); // remove separator after
		block.remove();
		this.repaintWordButtons();
		this.repaintSeps();
	}
	removeRow(button: HTMLElement) {
		const block: HTMLElement = button.closest(".korpus-words-row-full");
		block.remove();
		this.repaintWordButtons();
	}
	getRows(): NodeListOf<HTMLElement> {
		return document.querySelectorAll("#divSearch .korpus-words-row-full:not(#template-inputrow)");
	}
	repaintWordButtons() {
		var what = $('.korpus-words-row-removebutton');
		let rows: NodeListOf<HTMLElement> = this.getRows();
		if (rows.length > 1) {
			what.show();
			if (this.chainsInParagraph) {
				$('#btnChainAddSentence').hide();
				$('#btnChainAddParagraph').show();
			} else {
				$('#btnChainAddSentence').show();
				$('#btnChainAddParagraph').hide();
			}
		} else {
			what.hide();
			$('#btnChainAddSentence').show();
			$('#btnChainAddParagraph').show();
		}
		rows.forEach(e => {
			var collectionWords: NodeListOf<HTMLElement> = e.querySelectorAll(".inputword:not(#template-inputword)");
			var what = $(e).find('.close');
			if (collectionWords.length > 1) {
				what.show();
			} else {
				what.hide();
			}
		});
	}
	repaintSeps() {
		let rows: NodeListOf<HTMLElement> = this.getRows();
		rows.forEach(e => {
			var collectionSeps: NodeListOf<HTMLElement> = e.querySelectorAll(".inputsep:not(#template-inputsep)");
			collectionSeps.forEach(sep => {
				sep.querySelectorAll("input[type=checkbox]").forEach(cb => $(cb.parentNode).hide());
			});
			// first separator
			collectionSeps.item(0).querySelectorAll("input[type=checkbox]").forEach(cb => {
				if ("^,;".indexOf(cb.getAttribute('v')) >= 0) {
					$(cb.parentNode).show();
				}
			});
			for(let i=1; i<collectionSeps.length-1; i++) {
				// middle separators
				collectionSeps.item(i).querySelectorAll("input[type=checkbox]").forEach(cb => {
					if (",;".indexOf(cb.getAttribute('v')) >= 0) {
						$(cb.parentNode).show();
					}
				});
			}
			// last separator
			collectionSeps.item(collectionSeps.length-1).querySelectorAll("input[type=checkbox]").forEach(cb => {
				if (",;.!?$".indexOf(cb.getAttribute('v')) >= 0) {
					$(cb.parentNode).show();
				}
			});
		});
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
        $('#desc').html('');
		$('#output').html('');
	}
	showStatus(s: string) {
		$('#error').hide();
		document.getElementById("status").innerText = s;
		$('#status').show();
	}
	private collectFromScreenBase(p: BaseParams) {
		p.lang = this.currentFindLanguage;
		p.textStandard.subcorpuses = KorpusUI.separatedStringToArray(document.getElementById('inputFilterCorpus').innerText);
		p.textStandard.authors = $('#inputFilterAuthorShow').is(":visible") ? KorpusUI.separatedStringToArray(document.getElementById('inputFilterAuthor').innerText) : null;
		p.textStandard.sources = $('#inputFilterSourceShow').is(":visible") ? KorpusUI.separatedStringToArray(document.getElementById('inputFilterSource').innerText) : null;
		p.textStandard.stylegenres = $('#inputFilterStyleShow').is(":visible") ? KorpusUI.separatedStringToArray(document.getElementById('inputFilterStyle').innerText) : null;
		p.textStandard.yearWrittenFrom = $('#inputFilterYearWrittenShow').is(":visible") ? fulltrim((<HTMLInputElement>document.getElementById('inputFilterYearWrittenFrom')).value) : null;
		p.textStandard.yearWrittenTo = $('#inputFilterYearWrittenShow').is(":visible") ? fulltrim((<HTMLInputElement>document.getElementById('inputFilterYearWrittenTo')).value) : null;
		p.textStandard.yearPublishedFrom = $('#inputFilterYearPublishedShow').is(":visible") ? fulltrim((<HTMLInputElement>document.getElementById('inputFilterYearPublishedFrom')).value) : null;
		p.textStandard.yearPublishedTo = $('#inputFilterYearPublishedShow').is(":visible") ? fulltrim((<HTMLInputElement>document.getElementById('inputFilterYearPublishedTo')).value) : null;
	}
	collectFromScreenSearch(): SearchParams {
		let requestedParams: SearchParams = new SearchParams();
		this.collectFromScreenBase(requestedParams);
		let rows: NodeListOf<HTMLElement> = this.getRows();
		if (rows.length > 1) {
			requestedParams.chainsInParagraph = this.chainsInParagraph;
		}
		requestedParams.chains = [];
		rows.forEach(row => {
			var r = new ChainRequest();
			r.words = [];
			row.querySelectorAll('.inputword').forEach(w => r.words.push(this.collectWord(w)));
			r.seps = [];
			row.querySelectorAll('.inputsep').forEach(s => r.seps.push((<HTMLElement>s.querySelector(".sep-store-mode")).innerText));
			requestedParams.chains.push(r);
		});
		return requestedParams;
	}
	collectFromScreenCluster(): ClusterParams {
		let requestedParams: ClusterParams = new ClusterParams();
		this.collectFromScreenBase(requestedParams);
		document.querySelectorAll('#divCluster .inputword').forEach(w => requestedParams.word = this.collectWord(w));
		requestedParams.wordsBefore = parseInt((<HTMLInputElement>document.getElementById("inputClusterBefore")).value);
		requestedParams.wordsAfter = parseInt((<HTMLInputElement>document.getElementById("inputClusterAfter")).value);

		return requestedParams;
	}
	collectWord(w: Element): WordRequest {
		let wrq = new WordRequest();
		wrq.mode = (<HTMLInputElement>w.querySelector(".word-store-mode")).innerText;
		wrq.variants = (<HTMLInputElement>w.querySelector("input[name='variants']")).checked;
		wrq.word = fulltrim((<HTMLInputElement>w.querySelector("input[type='text']")).value);
		wrq.regexp = (<HTMLInputElement>w.querySelector("input[name='regexp']")).checked;
		wrq.grammar = fulltrim((<HTMLElement>w.querySelector(".wordgram-grammar-string")).innerText);
		return wrq;
	}
	restoreWord(w: WordRequest, el: Element) {
		this.setWordMode(<HTMLElement>el.querySelector(".word-store-mode"), w.mode ? w.mode : WordMode.ALL_FORMS);
		//(<HTMLElement>el.querySelector(".word-store-mode")).innerText = w.mode ? w.mode : WordMode.ALL_FORMS;
		(<HTMLInputElement>el.querySelector("input[name='variants']")).checked = w.variants;
		(<HTMLInputElement>el.querySelector("input[type='text']")).value = w.word ? w.word : "";
		(<HTMLInputElement>el.querySelector("input[name='regexp']")).checked = w.regexp;
		(<HTMLElement>el.querySelector(".wordgram-grammar-string")).innerText = w.grammar ? w.grammar : "";
		DialogWordGrammar.wordGrammarToText(w.grammar, el.querySelector(".wordgram-display"));
	}
	restoreSep(s: string, sep: Element) {
		if (s == null) {
			s = '';
		}
		switch(s) {
			case 'NONE':
				$(sep).find('input').prop('checked', false);
				$(sep).find("button.dropdown-toggle").text('не');
				$(sep).find('.sep-store-mode').text('NONE');
				return;
			case '':
				$(sep).find("button.dropdown-toggle").text('');
				$(sep).find('input').prop('checked', false);
				$(sep).find('.sep-store-mode').text('');
				break;
			default:
				$(sep).find('input').each(function(index, data) {
					this.checked = s.indexOf(this.getAttribute('v')) >= 0;
				});
				$(sep).find("button.dropdown-toggle").text(s);
				$(sep).find('.sep-store-mode').text(s);
				break;
		}
	}
	showSubcorpusNames() {
		let subcorpuses: string[] = KorpusUI.separatedStringToArray(document.getElementById('inputFilterCorpus').innerText);
		let cn = {};
		korpusService.initial.subcorpuses.map(kv => cn[kv.key] = kv.value.replace(/\|\|.+/g, ''));
		subcorpuses = subcorpuses.map(id => cn[id]);
		document.getElementById('inputFilterCorpusNames').innerText = subcorpuses.join(', ');
	}
	restoreToScreen(mode: string, data: BaseParams) {
		this.switchMode(mode);
		if (data != null) {
			this.switchLanguage(data.lang);
		}
		(<HTMLInputElement>document.getElementById('inputFilterYearWrittenFrom')).value = data && data.textStandard && data.textStandard.yearWrittenFrom ? data.textStandard.yearWrittenFrom : "";
		(<HTMLInputElement>document.getElementById('inputFilterYearWrittenTo')).value = data && data.textStandard && data.textStandard.yearWrittenTo ? data.textStandard.yearWrittenTo : "";
		(<HTMLInputElement>document.getElementById('inputFilterYearPublishedFrom')).value = data && data.textStandard && data.textStandard.yearPublishedFrom ? data.textStandard.yearPublishedFrom : "";
		(<HTMLInputElement>document.getElementById('inputFilterYearPublishedTo')).value = data && data.textStandard && data.textStandard.yearPublishedTo ? data.textStandard.yearPublishedTo : "";
		document.getElementById('inputFilterCorpus').innerText = data && data.textStandard && data.textStandard.subcorpuses ? data.textStandard.subcorpuses.join(';') : korpusService.initial.preselectedSubcorpuses;
		this.showSubcorpusNames();
		document.getElementById('inputFilterAuthor').innerText = data && data.textStandard && data.textStandard.authors ? data.textStandard.authors.join(';') : "Усе";
		document.getElementById('inputFilterSource').innerText = data && data.textStandard && data.textStandard.sources ? data.textStandard.sources.join(';') : "Усе";
		document.getElementById('inputFilterStyle').innerText = data && data.textStandard && data.textStandard.stylegenres ? data.textStandard.stylegenres.join(';') : "Усе";
		let newWord = this.addWord(document.querySelector("#divCluster .korpus-words-row-full"));
		switch (this.getMode()) {
			case 'search':
			case 'kwic':
				let sps: SearchParams = <SearchParams>data;
				if (sps && sps.chains) {
					sps.chains.forEach(chain => {
						if (chain.seps.length == chain.words.length + 1) {
							let r = this.addRow(false);
							for (let i = 0; i < chain.words.length; i++) {
								this.restoreSep(chain.seps[i], this.addSep(r));
								this.restoreWord(chain.words[i], this.addWord(r));
							}
							this.restoreSep(chain.seps[chain.seps.length - 1], this.addSep(r));
						}
					});
				} else {
					this.addRow(true);
				}
				break;
			case 'cluster':
				let cp: ClusterParams = <ClusterParams>data;
				if (cp && cp.word) {
					let w: HTMLElement = this.addWord(null);
					(<HTMLInputElement>w.querySelector("input[type='text']")).value = cp.word.word ? cp.word.word : "";
					(<HTMLElement>w.querySelector(".wordgram-grammar-string")).innerText = cp.word.grammar ? cp.word.grammar : "";
					DialogWordGrammar.wordGrammarToText(cp.word.grammar, w.querySelector(".wordgram-display"));
				}
				(<HTMLInputElement>document.getElementById("inputClusterBefore")).value = cp.wordsBefore ? cp.wordsBefore.toString():"1";
				(<HTMLInputElement>document.getElementById("inputClusterAfter")).value = cp.wordsAfter ? cp.wordsAfter.toString():"1";
				break;
		}
		DialogSubcorpuses.showInputFilterDependsOnSubcorpus();
	}
	showOutput() {
		$('#output').html($.templates("#template-output").render({
			pages: korpusService.pages,
			currentPage: korpusService.currentPage,
			hasMore: korpusService.hasMore,
			resultSearch: korpusService.resultSearch,
			resultKwic: korpusService.resultKwic,
			resultCluster: korpusService.resultCluster,
			visitedList: this.visitedList,
            totalCount: korpusService.totalCount
        }));
        $('[data-toggle="tooltip"]').tooltip();
	}
	spisy() {
		let subcorpus = $('input[name=kankardansnyjaSpisy]:checked').val();
		let word = $('input[name=wordSpis]').val();
		if (spisy[subcorpus] == null) {
			korpusService.loadSpis(subcorpus, function() {
				if (spisy[subcorpus] != null) {
					korpusui.showSpisy(subcorpus, word);
				}
			});
		} else {
			korpusui.showSpisy(subcorpus, word);
		}
	}
	showSpisy(subcorpus: string, word: string) {
		word = word.trim().replace('.', '').replace('*', '.*');
		let rx = word == '' ? null : new RegExp('^' + word + '$');
		$('#output').html($.templates("#template-output-spisy").render({
			subcorpus: subcorpus,
			data: rx == null ? spisy[subcorpus] : spisy[subcorpus].filter(r => rx.test(r.w))
		}));
	}
	static separatedStringToArray(v: string): string[] {
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
$.views.converters("roundnum", roundnum);
$.views.converters("json", function (val) {
	return JSON.stringify(val);
});
$.views.converters("titleShort", function (str) {
    let s = str.toString();
    if (s.length == 0) {
      return "<...>";
    } else if (s.length > 20) {
      return s.substring(0,17)+"...";
    } else {
      return s;
    }
});
$('body')
	.on('mousedown', '.popover', function (e) {
		e.preventDefault();
	});
$.views.converters("wordtail", function (val) {
	if (val) {
		return val.replace('<', '&lt;').replace('>', '&gt;').replace('\n', '<br/>').replace('{', '<b style="background-color: #CCF">{').replace('}', '}</b>');
	} else {
		return "";
	}
});
$.views.converters("korpusname", function(val) {
	return val.replace(/\|\|.+/g, '');
});
$.views.converters("korpusdesc", function(val) {
	return val.replace(/.+\|\|/g, '');
});
$.views.converters("naciski", function(val) {
    return val != null ? val.replaceAll("+", "\u0301") : val;
});
$(function() {
	$("body").tooltip({ selector: '[data-toggle=tooltip]' });
});

var korpusui: KorpusUI = null;
var korpusService: KorpusService = null;
var localization: { [key:string]: string } = null;
var dialogAuthors: DialogList = null;
var dialogSources: DialogList = null;
var dialogSubcorpuses: DialogSubcorpuses = null;
var dialogText: DialogText = null;
var dialogStyleGenres: DialogStyleGenres = null;
var dialogWordGrammar: DialogWordGrammar = null;
var spisy: { [key: string]: FreqSpisResult[]; } = {};
function initializeKorpusPage(callPrefix: string, defaultLanguage: string) {
	korpusui = new KorpusUI(defaultLanguage);
	korpusService = new KorpusService(callPrefix);
}
