
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
	//public errorSearch: string;
	//public statusSearch: string;

	public initial: InitialData;

	/*public typeSearch: string = "search";

	public inputTextStandard: StandardTextRequest;
	public inputTextUnprocessed: UnprocessedTextRequest;
	public inputWords: WordRequest[];
	public inputWordsBefore: number = 1;
	public inputWordsAfter: number = 1;
	public inputWordsOrder: string = "PRESET"; // "PRESET" | "ANY_IN_SENTENCE" | "ANY_IN_PARAGRAPH";
*/
	private requestedTypeSearch: string = null;
	public requestedParams: SearchParams = null;
	public requestedParamsCluster: ClusterParams = null;
	public latestMark: LatestMark = null;
	public hasMore: boolean = false;

	public pages: number[][];
	public currentPage: number;

	// current page data
	public resultSearch: ResultSearchOut;
	public resultKwic: ResultKwicOut;
	public resultCluster: ClusterResult;

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
		let mode = 'search';
		if (ps.charAt(0) == '#') {
			ps = ps.substring(1);
			let p = ps.indexOf(':');
			if (p >= 0) {
				mode = ps.substring(0, p);
				ps = ps.substring(p + 1);
			}
		} else {
			ps = null;
		}
		let data;
		try {
			data = JSON.parse(decodeURI(ps));
		} catch (error) {
			data = null;
		}
		korpusui.restoreToScreen(mode, data);
	}

	search() {
		this.requestedTypeSearch = korpusui.getMode();
		if (this.requestedTypeSearch == 'cluster') {
			this.requestedParamsCluster = korpusui.collectFromScreenCluster();
			window.location.hash = '#' + this.requestedTypeSearch + ":" + stringify(this.requestedParamsCluster);
		} else {
			this.requestedParams = korpusui.collectFromScreenSearch();
			window.location.hash = '#' + this.requestedTypeSearch + ":" + stringify(this.requestedParams);
		}

		this.pages = [];
		korpusui.visitedList = [];
		this.resultSearch = null;
		this.resultKwic = null;
		this.resultCluster = null;
		this.latestMark = null;
		this.hasMore = false;

		if (this.requestedTypeSearch == 'cluster') {
			this.searchCluster();
		} else {
			this.searchMore(korpusui.minimizeHeader);
		}
	}
	searchCluster() {
		korpusui.showStatus("Шукаем...");

		fetch('rest/korpus/cluster', {
			method: 'POST',
			headers: {
				'Content-Type': 'application/json'
			},
			body: JSON.stringify(this.requestedParamsCluster)
		})
			.then(r => {
				if (!r.ok) {
					korpusui.showError("Памылка пошуку: " + r.status + " " + r.statusText);
				} else {
					r.json().then(json => {
						korpusui.hideStatusError();
						let rs: ClusterResult = json;
						if (rs.rows.length > 0) {
							this.resultCluster = new ClusterResult(rs.rows);
							korpusui.showOutput();
						} else {
							korpusui.showError("Нічога не знойдзена");
						}
					});
				}
			})
			.catch(err => korpusui.showError("Памылка: " + err));
	}

	searchMore(after: Function) {
		korpusui.showStatus("Шукаем...");
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
							korpusui.showError("Памылка: " + rs.error);
							return;
						}
						this.latestMark = rs.latest;
						this.hasMore = rs.hasMore;
						if (rs.foundIDs.length > 0) {
							this.pages.push(rs.foundIDs);
							korpusui.showOutput();
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
		korpusui.showStatus("Падрабязнасці...");
		let rq: SentencesRequest = new SentencesRequest();
		rq.params = this.requestedParams;
		rq.list = list;
		fetch('rest/korpus/sentences', {
			method: 'POST',
			headers: {
				'Content-Type': 'application/json'
			},
			body: JSON.stringify(rq)
		})
			.then(r => {
				if (!r.ok) {
					korpusui.showError("Памылка: " + r.status + " " + r.statusText);
				} else {
					r.json().then(json => {
						switch (this.requestedTypeSearch) {
							case 'search':
								this.resultKwic = null;
								this.resultSearch = new ResultSearchOut(json, this.requestedParams.words.length);
								break;
							case 'kwic':
								this.resultSearch = null;
								this.resultKwic = new ResultKwicOut(json, this.requestedParams.words.length);
								break;
						}
						this.currentPage = page;
						korpusui.hideStatusError();
						korpusui.showOutput();
					});
				}
			})
			.catch(err => korpusui.showError("Памылка: " + err));
	}
}
