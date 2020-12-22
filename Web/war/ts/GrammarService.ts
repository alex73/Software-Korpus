class GrammarService {
  public error: string;
  public status: string;

  public initial: GrammarInitial;

  public inputWord: WordRequest = new WordRequest();
  public inputOrder: string = "STANDARD"; // "STANDARD" | "REVERSE";

  public results: LemmaInfo[] = [];
  public details: LemmaParadigm;

  constructor() {
    grammarui.showStatus("Чытаюцца пачатковыя звесткі...");
    fetch('rest/grammar/initial')
      .then(r => {
        if (!r.ok) {
          grammarui.showError("Памылка запыту пачатковых звестак: " + r.status + " " + r.statusText);
        } else {
          r.json().then(json => {
            this.initial = json;
            grammarui.hideStatusError();
            this.afterInit();
          });
        }
      })
      .catch(err => grammarui.showError("Памылка: " + err));
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
    grammarui.restoreToScreen(data);
  }

  search() {
    let rq: GrammarRequest = grammarui.collectFromScreen();
    window.location.hash = '#' + stringify(rq);

    grammarui.showStatus("Шукаем...");
    fetch('rest/grammar/search', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json'
      },
      body: JSON.stringify(rq)
    })
      .then(r => {
        if (!r.ok) {
          grammarui.showError("Памылка пошуку: " + r.status + " " + r.statusText);
        } else {
          r.json().then(json => {
            this.results = json;
            grammarui.showOutput(rq.grammar);
            if (this.results.length > 0) {
              grammarui.hideStatusError();
            } else {
              grammarui.showError("Нічога не знойдзена");
            }
          });
        }
      })
      .catch(err => grammarui.showError("Памылка: " + err));
  }
  loadDetails(pdgId: number) {
    grammarui.showStatus("Шукаем...");

    fetch("rest/grammar/details/" + pdgId)
      .then(r => {
        if (!r.ok) {
          grammarui.showError("Памылка пошуку: " + r.status + " " + r.statusText);
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
  public word: string;
  public multiForm: boolean = false;
  public grammar: string;
  public outputGrammar: string;
  public orderReverse: boolean;
}
