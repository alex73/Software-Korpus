
class SearchRequest {
	public params: SearchParams;
	public latest: LatestMark;
}

class SearchResult {
	public error: string;
	public foundIDs: number[];
	public latest: LatestMark;
	public hasMore: boolean;
}

class SentencesRequest {
	public params: SearchParams;
	public list: number[];
}

class KorpusService {
	public errorSearch: string;
	public statusSearch: string;

	public initial: InitialData;

	public typeSearch: string = "search";

	public inputTextStandard: StandardTextRequest;
	public inputTextUnprocessed: UnprocessedTextRequest;
	public inputWords: WordRequest[];
	public inputWordsBefore: number = 1;
	public inputWordsAfter: number = 1;
	public inputWordsOrder: string = "PRESET"; // "PRESET" | "ANY_IN_SENTENCE" | "ANY_IN_PARAGRAPH";

	private requestedTypeSearch: string = null;
	public requestedParams: SearchParams = null;
	public requestedParamsCluster: ClusterParams = null;
	public latestMark: LatestMark = null;
	public hasMore: boolean = false;

	public pages: number[][] = [];
	public currentPage: number = 0;
	public resultSearch: ResultSearchOut;
	public resultKwic: ResultKwicOut;
	public resultCluster: ClusterResult;

	public minimizeSearch: Function;
	public minimizeKwic: Function;
	public minimizeCluster: Function;
	public maximizeSearch: Function;
	public maximizeKwic: Function;
	public maximizeCluster: Function;

	constructor() {
		korpusui.showStatus("Чытаюцца пачатковыя звесткі...");
		fetch('rest/korpus/initial')
			.then(r => {
				if (!r.ok) {
					korpusui.showError("Памылка запыту пачатковых звестак: " + r.status + " " + r.statusText);
				} else {
					r.json().then(json => {
						this.initial = json;
						korpusui.hideStatusError();
						this.afterInit();
					});
				}
			})
			.catch(err => korpusui.showError("Памылка: " + err));
	}

	afterInit() {
		let ps = window.location.hash;
		if (ps.charAt(0) == '#') {
			ps = ps.substring(1);
		}
		let data;
		try {
			data = JSON.parse(decodeURI(ps));
		} catch (error) {
			data = null;
		}
		korpusui.restoreToScreen(data);
		/*if (ps.startsWith("search&")) {
			this.typeSearch = "search";
			let p: SearchParams = new SearchParams();
			p.importParameters(ps);
			this.inputCorpusType = p.corpusType;
			this.inputTextStandard = p.textStandard;
			this.inputTextUnprocessed = p.textUnprocessed;
			this.inputWords = p.words;
			this.inputWordsOrder = p.wordsOrder;
		} else if (ps.startsWith("kwic&")) {
			this.typeSearch = "kwic";
			let p: SearchParams = new SearchParams();
			p.importParameters(ps);
			this.inputCorpusType = p.corpusType;
			this.inputTextStandard = p.textStandard;
			this.inputTextUnprocessed = p.textUnprocessed;
			this.inputWords = p.words;
		} else if (ps.startsWith("cluster&")) {
			this.typeSearch = "cluster";
			let p: ClusterParams = new ClusterParams();
			p.importParameters(ps);
			this.inputCorpusType = p.corpusType;
			this.inputTextStandard = p.textStandard;
			this.inputTextUnprocessed = p.textUnprocessed;
			this.inputWords = [p.word];
			this.inputWordsBefore = p.wordsBefore;
			this.inputWordsAfter = p.wordsAfter;
		}
		for (let w of this.inputWords) {
			w.updateDisplay(this.initial.grammar.grammarTree);
		}*/
	}

	search() {
		this.requestedParams = korpusui.collectFromScreen();

		this.requestedTypeSearch = this.typeSearch;
		window.location.hash = '#' + stringify(this.requestedParams);

		this.pages = [];
		this.resultSearch = null;
		this.resultKwic = null;
		this.resultCluster = null;
		this.latestMark = null;
		this.hasMore = false;

		//this.maximizeSearch();
		//this.maximizeKwic();
		//this.maximizeCluster();
		this.searchMore(this.requestedTypeSearch == 'search' ? this.minimizeSearch : this.minimizeKwic);
	}
	searchCluster() {
		this.requestedParamsCluster = new ClusterParams();
		this.requestedParamsCluster.corpusType = this.inputCorpusType;
		this.requestedParamsCluster.textStandard = this.inputTextStandard.clone();
		this.requestedParamsCluster.textUnprocessed = this.inputTextUnprocessed.clone();
		this.requestedParamsCluster.word = this.inputWords[0].clone();
		this.requestedParamsCluster.word.display = undefined;
		this.requestedParamsCluster.wordsBefore = this.inputWordsBefore;
		this.requestedParamsCluster.wordsAfter = this.inputWordsAfter;

		window.location.hash = '#' + this.requestedParamsCluster.exportParameters();

		this.pages = [];
		this.resultSearch = null;
		this.resultKwic = null;
		this.resultCluster = null;
		this.latestMark = null;
		this.hasMore = false;

		this.maximizeSearch();
		this.maximizeKwic();
		this.maximizeCluster();

		this.errorSearch = null;
		this.statusSearch = "Шукаем...";
		let headers = new Headers({ 'Content-Type': 'application/json' });
		/*this.http.post("/rest/korpus/cluster", this.requestedParamsCluster, headers).subscribe((res: Response) => {
			let rs: ClusterResult = res.json();
			if (rs.rows.length > 0) {
				this.resultCluster = new ClusterResult(res.json().rows);
				this.minimizeCluster();
			} else {
				this.errorSearch = "Нічога не знойдзена";
			}
			this.statusSearch = null;
		}, err => {
			this.errorSearch = "Памылка: " + err.statusText;
			this.statusSearch = null;
		});*/
	}

	searchMore(after: Function) {
		korpusui.showStatus("Шукаем...");
		let headers = new Headers({ 'Content-Type': 'application/json' });
		let rq: SearchRequest = new SearchRequest();
		rq.params = this.requestedParams;
		rq.latest = this.latestMark;

		fetch('rest/korpus/search', {
			method: 'POST',
			headers: {
				'Content-Type': 'application/json'
			},
			body: JSON.stringify(rq)
		})
			.then(r => {
				if (!r.ok) {
					korpusui.showError("Памылка пошуку: " + r.status + " " + r.statusText);
				} else {
					r.json().then(json => {
						let rs: SearchResult = json;
						if (rs.error != null) {
							korpusui.showError("Памылка: "+rs.error);
							return;
						}
						this.latestMark = rs.latest;
						this.hasMore = rs.hasMore;
						if (rs.foundIDs.length > 0) {
							this.pages.push(rs.foundIDs);
							this.requestPage(this.pages.length - 1);
							if (after) {
								after();
							}
						} else {
							korpusui.showError("Нічога не знойдзена");
						}
					});
				}
			})
			.catch(err => korpusui.showError("Памылка: " + err));
	}
	requestPage(page: number) {
		if (page < 0) {
			// new page
			this.searchMore(null);
		} else {
			this.requestPageDetails(page);
		}

		// scroll to beginning of list
		let pos = document.getElementById('searchTop');
		if (pos != null) {
			pos.scrollIntoView();
		}
	}
	requestPageDetails(page: number) {
		let list: number[] = this.pages[page];
		this.errorSearch = null;
		this.statusSearch = "Падрабязнасці...";
		let headers = new Headers({ 'Content-Type': 'application/json' });
		let rq: SentencesRequest = new SentencesRequest();
		rq.params = this.requestedParams;
		rq.list = list;
		/*this.http.post("/rest/korpus/sentences", rq, headers).subscribe((res: Response) => {
			switch (this.requestedTypeSearch) {
				case 'search':
					this.resultKwic = null;
					this.resultSearch = new ResultSearchOut(res.json(), this.requestedParams.words.length);
					break;
				case 'kwic':
					this.resultSearch = null;
					this.resultKwic = new ResultKwicOut(res.json(), this.requestedParams.words.length);
					break;
			}
			this.currentPage = page;
			this.statusSearch = null;
		}, err => {
			this.errorSearch = "Памылка: " + err.statusText;
			this.statusSearch = null;
		});*/
	}

	selectSearch() {
		this.typeSearch = "search";
	}
	selectKWIC() {
		this.typeSearch = "kwic";
	}
}
