import { Injectable } from '@angular/core';
import { Http, Response, Headers } from '@angular/http';
import { Observable }     from 'rxjs/Observable';
import { InitialData } from './dto/InitialData';
import { WordRequest } from './dto/WordRequest';
import { WordResult } from './dto/WordResult';
import { StandardTextRequest } from './dto/StandardTextRequest';
import { UnprocessedTextRequest } from './dto/UnprocessedTextRequest';
import { ClusterParams, SearchParams } from './dto/RequestTypes';
import { ResultSearchOut, ResultKwicOut, SearchResults } from './dto/ResultTypes';
import { LatestMark, ClusterResult } from './service.search.dto';

@Injectable()
export class ServiceSearch {
  public errorSearch: string;
  public statusSearch: string;

  public initial: InitialData = new InitialData();

  public typeSearch: string = "search";
  
  public inputCorpusType: string = "MAIN"; // MAIN, UNSORTED
  public inputTextStandard: StandardTextRequest = new StandardTextRequest();
  public inputTextUnprocessed: UnprocessedTextRequest = new UnprocessedTextRequest();
  public inputWords: WordRequest[] = [new WordRequest()];
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
  
  public removeWord = (w) => {
    this.removeWordImpl(w);
  };

  addWord() {
    this.inputWords.push(new WordRequest());
  }
  
  removeWordImpl( wordToRemove: WordRequest ) {
    var index = this.inputWords.indexOf(wordToRemove);
    if (index > -1) {
       this.inputWords.splice(index, 1);
    }
  }

  constructor (private http:Http) {
    this.errorSearch = null;
    this.statusSearch = "Чытаецца...";
    this.http.get("/rest/korpus/initial").subscribe((res: Response) => {
      this.initial = res.json();
      this.statusSearch = null;
      this.afterInit();
    }, err => {
      this.errorSearch = "Памылка: "+err.statusText;
      this.statusSearch = null;
    });
  }
  
  afterInit() {
    let ps = window.location.hash;
    if (ps.charAt(0) == '#') {
      ps = ps.substring(1);
    }
    if (ps.startsWith("search&")) {
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
    for(let w of this.inputWords) {
      w.updateDisplay(this.initial.grammar.grammarTree);
    }
  }

  search() {
    this.requestedParams = new SearchParams();
    this.requestedParams.corpusType = this.inputCorpusType;
    this.requestedParams.textStandard = this.inputTextStandard.clone();
    this.requestedParams.textUnprocessed = this.inputTextUnprocessed.clone();
    this.requestedParams.words = [];
    for(let w of this.inputWords) {
      let wrq = w.clone();
      wrq.display = undefined;
      this.requestedParams.words.push(wrq);
    }
    this.requestedParams.wordsOrder = this.inputWordsOrder;
    
    this.requestedTypeSearch = this.typeSearch;
    window.location.hash = '#' + this.requestedParams.exportParameters(this.requestedTypeSearch);
  
    this.pages = [];
    this.resultSearch = null;
    this.resultKwic = null;
    this.resultCluster = null;
    this.latestMark = null;
    this.hasMore = false;

    this.maximizeSearch();
    this.maximizeKwic();
    this.maximizeCluster();
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
    this.http.post("/rest/korpus/cluster", this.requestedParamsCluster, headers).subscribe((res: Response) => {
      let rs: ClusterResult = res.json();
      if (rs.rows.length > 0) {
        this.resultCluster = new ClusterResult(res.json().rows);
        this.minimizeCluster();
      } else {
        this.errorSearch = "Нічога не знойдзена";
      }
      this.statusSearch = null;
    }, err => {
      this.errorSearch = "Памылка: "+err.statusText;
      this.statusSearch = null;
    });
  }

  searchMore(after: Function) {
    this.errorSearch = null;
    this.statusSearch = "Шукаем...";
    let headers = new Headers({ 'Content-Type': 'application/json' });
    let rq: SearchRequest = new SearchRequest();
    rq.params = this.requestedParams;
    rq.latest = this.latestMark;
    this.http.post("/rest/korpus/search", rq, headers).subscribe((res: Response) => {
      let rs: SearchResult = res.json();
      this.latestMark = rs.latest;
      this.hasMore = rs.hasMore;
      if (rs.foundIDs.length > 0) {
        this.pages.push(rs.foundIDs);
        this.requestPage(this.pages.length-1);
        if (after) {
          after();
        }
      } else {
        this.errorSearch = "Нічога не знойдзена";
        this.statusSearch = null;
      }
    }, err => {
      this.errorSearch = "Памылка: "+err.statusText;
      this.statusSearch = null;
    });
  }
  requestPage(page:number) {
    if (page < 0) {
      // new page
      this.searchMore(null);
    } else {
      this.requestPageDetails(page);
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
    this.http.post("/rest/korpus/sentences", rq, headers).subscribe((res: Response) => {
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
      this.errorSearch = "Памылка: "+err.statusText;
      this.statusSearch = null;
    });
  }

  selectSearch() {
    this.typeSearch = "search";
  }
  selectKWIC() {
    this.typeSearch = "kwic";
  }
}

export class SearchRequest {
    public params: SearchParams;
    public latest: LatestMark;
}

export class SearchResult {
    public foundIDs: number[];
    public latest: LatestMark;
    public hasMore: boolean;
}

export class SentencesRequest {
    public params: SearchParams;
    public list: number[];
}
