import { Injectable } from '@angular/core';
import { Http, Response, Headers } from '@angular/http';
import { Observable }     from 'rxjs/Observable';
import { GrammarInitial } from './dto/GrammarInitial';
import { WordRequest } from './dto/WordRequest';
import { LemmaInfo, LemmaParadigm } from './dto/LemmaInfo';
import { WordResult } from './dto/WordResult';
import { StandardTextRequest } from './dto/StandardTextRequest';
import { UnprocessedTextRequest } from './dto/UnprocessedTextRequest';
import { ClusterParams, SearchParams } from './dto/RequestTypes';
import { ResultSearchOut, ResultKwicOut, SearchResults } from './dto/ResultTypes';
import { LatestMark, ClusterResult } from './service.search.dto';

@Injectable()
export class ServiceGrammar {
  public error: string;
  public status: string;

  public initial: GrammarInitial = new GrammarInitial();

  public inputWord: WordRequest = new WordRequest();
  public inputOrder: string = "STANDARD"; // "STANDARD" | "REVERSE";
  
  public results: LemmaInfo[] = [];
  public details: LemmaParadigm;

  constructor (private http:Http) {
    this.error = null;
    this.status = "Чытаецца...";
    this.http.get("/rest/grammar/initial").subscribe((res: Response) => {
      this.initial = res.json();
      this.status = null;
      this.afterInit();
    }, err => {
      this.error = "Памылка: "+err.statusText;
      this.status = null;
    });
  }
  afterInit() {
    let ps = window.location.hash;
    if (ps.charAt(0) == '#') {
      ps = ps.substring(1);
    }
    let rq: GrammarRequest = new GrammarRequest();
    rq.importParameters(ps);
    this.inputWord = rq.word.clone();
    this.inputOrder = rq.orderReverse ? "REVERSE" : "STANDARD";
    this.inputWord.updateDisplay(this.initial.grammarTree);
  }

  search() {
    let rq: GrammarRequest = new GrammarRequest();
    rq.word = this.inputWord.clone();
    rq.word.display = undefined;
    rq.orderReverse = this.inputOrder != 'STANDARD';

    window.location.hash = '#' + rq.exportParameters();
    
    this.error = null;
    this.status = "Шукаем...";
    let headers = new Headers({ 'Content-Type': 'application/json' });
    this.http.post("/rest/grammar/search", rq, headers).subscribe((res: Response) => {
      this.results = res.json();
      if (this.results.length == 0) {
        this.error = "Нічога не знойдзена";
      }
      this.status = null;
    }, err => {
      this.error = "Памылка: "+err.statusText;
      this.status = null;
    });
  }

  loadDetails(lemma: LemmaInfo, callback: Function) {
    this.error = null;
    this.status = "Шукаем...";
    this.http.get("/rest/grammar/details/"+lemma.pdgId).subscribe((res: Response) => {
      this.details = res.json();
      this.status = null;
      callback();
    }, err => {
      this.error = "Памылка: "+err.statusText;
      this.status = null;
    });
  }
}

export class GrammarRequest {
  public word: WordRequest = new WordRequest();
  public orderReverse: boolean;
  
  exportParameters() {
    let out: string = "";

    out += this.out("w.word", this.word.word);
    out += this.out("w.allForms", this.word.allForms);
    out += this.out("w.grammar", this.word.grammar);
    out += this.out("orderReverse", this.orderReverse);

    return out.substring(1);
  }

    importParameters(ps: string) {
      if (ps) {
          for (let p of ps.split("&")) {
            let pos: number = p.indexOf('=');
            if (pos > 0) {
                let key: string = p.substring(0, pos);
                let value: string = p.substring(pos + 1);
                this.importParameter(key, value);
            }
          }
      }
    }
    importParameter(key: string, value: string) {
      switch(key) {
        case "w.word":
          this.word.word = value;
          break;
        case "w.allForms":
          this.word.allForms = value === 'true';
          break;
        case "w.grammar":
          this.word.grammar = value;
          break;
        case "orderReverse":
          this.orderReverse = value === 'true';
          break;
      }
    }
  out(name: string, txt: any): string {
    return txt ? '&'+name+'='+txt : "";
  }
}
