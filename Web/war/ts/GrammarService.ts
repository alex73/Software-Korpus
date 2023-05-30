class GrammarService {
	private callPrefix: string = "korpus";

  public error: string;
  public status: string;

  public initial: GrammarInitial;

  public inputWord: WordRequest = new WordRequest();
  public inputOrder: string = "STANDARD"; // "STANDARD" | "REVERSE";

  public result: GrammarSearchResult;
  public details: LemmaParadigm;

	constructor() {
		grammarui.showStatus("Чытаюцца пачатковыя звесткі...");
		fetch('rest/localization')
			.then(r => {
				if (!r.ok) {
					korpusui.showError("Памылка запыту лакалізацыі звестак. Звярніцеся да распрацоўшчыкаў");
				} else {
					r.json().then(json => {
						localization = json[document.documentElement.lang];
						this.afterInit1();
					});
				}
			})
			.catch(err => korpusui.showError("Памылка: " + err));
	}

	afterInit1() {
      fetch(this.callPrefix + '/grammar/initial')
      .then(r => {
        if (!r.ok) {
          grammarui.showError("Памылка запыту пачатковых звестак. Звярніцеся да распрацоўшчыкаў");
        } else {
          r.json().then(json => {
            this.initial = json;
            grammarui.hideStatusError();
            this.afterInit2();
          });
        }
      })
      .catch(err => grammarui.showError("Памылка: " + err));
	}

  afterInit2() {
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
    grammarui.restoreToScreen(data);
    $('#grammarStat').html($.templates("#template-grammar-stat").render({
        initial: this.initial
    }));
  }

  search() {
    $('#desc').hide();
    $('#grammarStat').hide();
    let rq: GrammarRequest = grammarui.collectFromScreen();
    window.location.hash = '#' + stringify(rq);

    grammarui.showStatus("Шукаем...");
    fetch(this.callPrefix + '/grammar/search', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json'
      },
      body: JSON.stringify(rq)
    })
      .then(r => {
        if (!r.ok) {
          grammarui.showError("Памылка сервера. Звярніцеся да распрацоўшчыкаў");
        } else {
          r.json().then(json => {
            this.result = json;
            if (this.result.error) {
              grammarui.showError(this.result.error);
            } else {
              grammarui.showOutput(rq.grammar, rq.orderReverse && !rq.outputGrouping);
              if (this.result.output.length > 0) {
                grammarui.hideStatusError();
              } else if (this.result.hasMultiformResult) {
                grammarui.showError("Нічога не знойдзена, але ёсць такое слова сярод формаў. Паспрабуйце пашукаць па ўсіх формах.");
              } else {
                grammarui.showError("Нічога не знойдзена");
              }
            }
          });
        }
      })
      .catch(err => grammarui.showError("Памылка: " + err));
  }
  loadDetails(pdgId: number) {
    grammarui.showStatus("Шукаем...");
    
    let url;
    let elemFull: HTMLInputElement = <HTMLInputElement>document.getElementById("grammarword-full");
    if (elemFull != null && elemFull.checked) {
        url = this.callPrefix + "/grammar/detailsFull/" + pdgId;
    } else {
        url = this.callPrefix + "/grammar/details/" + pdgId;
    }

    fetch(url)
      .then(r => {
        if (!r.ok) {
          grammarui.showError("Памылка сервера. Звярніцеся да распрацоўшчыкаў");
        } else {
          r.json().then(json => {
            grammarui.hideStatusError();
            dialogGrammarDB = new DialogGrammarDB(json);
          });
        }
      })
      .catch(err => grammarui.showError("Памылка: " + err));
  }
}

class GrammarRequest {
  public lang: string; // мова
  public word: string; // тое што ўвёў карыстальнік
  public multiForm: boolean = false; // пошук па формах ?
  public grammar: string; // удакладненая граматыка
  public outputGrammar: string; // адмысловая форма
  public orderReverse: boolean; // адваротны парадак
  public outputGrouping: boolean; // групаваць
  public fullDatabase: boolean; // шукаць па ўсёй базе, не звяртаючы ўвагу на фільтры
}
