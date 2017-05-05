import { Component, ElementRef, ViewChild, Input, ChangeDetectorRef } from '@angular/core';
import { ServiceSearch } from './service.search';
import { WordResult } from './dto/WordResult';
import { DirectiveWord }   from './directive.word';
import { GrammarInitial }   from './dto/GrammarInitial';
import { ModalDirective, PopoverDirective } from 'ng2-bootstrap';
import { SearchResultsText } from './dto/ResultTypes';
import { TextInfo, OtherInfo } from './service.search.dto';

@Component({
  selector: 'output-search',
  templateUrl: "./output.search.html"
})

export class OutputSearch {
  constructor (public serviceSearch:ServiceSearch, private ref: ChangeDetectorRef) { }
  
  @Input() kwic: boolean;
  @ViewChild('popoverWord') popoverWordRef:ElementRef;
  @ViewChild('popoverWordContent') popoverWordContentRef:ElementRef;
  @ViewChild('popoverBiblio') popoverBiblioRef: ElementRef;
  
  @ViewChild('textDetailsModal') public textDetailsModal: ModalDirective;
  public detailsText: WordResult[][] = [];
  public biblio: TextInfo;
  public biblioOther: OtherInfo;
  
  over(event) {
    if (event.target) {
      let wlemma = event.target.getAttribute("korpusWord.lemma");
      let wcat = event.target.getAttribute("korpusWord.cat");
      if (wlemma && wcat) {
        this.popoverWordContentRef.nativeElement.innerHTML = this.html(wlemma, wcat);
        let spanPlace = this.getOffsetRect(event.target);
        let popoverSize = this.popoverWordRef.nativeElement.getBoundingClientRect();
        this.popoverWordRef.nativeElement.style.top = (spanPlace.top-popoverSize.height)+'px';
        this.popoverWordRef.nativeElement.style.left = (spanPlace.left+spanPlace.width/2-popoverSize.width/2)+'px';
        this.popoverWordRef.nativeElement.style.visibility = 'visible';
        this.popoverWordRef.nativeElement.focus();
      } else {
        this.popoverWordRef.nativeElement.style.visibility = 'hidden';
      }
    }
  }
  showBiblio(event, doc: TextInfo, docOther: OtherInfo) {
    this.biblio = doc;
    this.biblioOther = docOther;
    this.ref.detectChanges();
    let spanPlace = this.getOffsetRect(event.target);
    let popoverSize = this.popoverBiblioRef.nativeElement.getBoundingClientRect();
    this.popoverBiblioRef.nativeElement.style.top = (spanPlace.top-popoverSize.height)+'px';
    this.popoverBiblioRef.nativeElement.style.left = (spanPlace.left+spanPlace.width/2-popoverSize.width/2)+'px';
    this.popoverBiblioRef.nativeElement.style.visibility = 'visible';
    this.popoverBiblioRef.nativeElement.focus();
    return false;
  }
  leave() {
    this.popoverWordRef.nativeElement.style.visibility = 'hidden';
    this.popoverBiblioRef.nativeElement.style.visibility = 'hidden';
  }
  
  fullText( origText: SearchResultsText, doc: TextInfo, docOther: OtherInfo ) {
    this.biblio = doc;
    this.biblioOther = docOther;
    this.detailsText = origText.words;
    this.textDetailsModal.show();
    return false;
  }
  
  html(lemma: string, cat: string): string {
    let o: string = "";
    if (lemma) {
      let lemmas: string[] = lemma.split('_');
      o += 'Лема:&nbsp;' + lemmas.join(', ');
    }
    if (cat) {
      o += '<br/>Граматыка:';
      for (let c of cat.split('_')) {
        o += '<br/>' + c + ':&nbsp;';
        let oo: string = GrammarInitial.text(c, this.serviceSearch.initial.grammar.grammarTree);
        if (oo) {
          o += oo.replace(' ', '&nbsp;');
        }
      }
    }
    return o;
  }

  getOffsetRect(elem) {
    var box = elem.getBoundingClientRect();
    var body = document.body;
    var docElem = document.documentElement;
    
    var scrollTop = window.pageYOffset || docElem.scrollTop || body.scrollTop;
    var scrollLeft = window.pageXOffset || docElem.scrollLeft || body.scrollLeft;
    
    var clientTop = docElem.clientTop || body.clientTop || 0;
    var clientLeft = docElem.clientLeft || body.clientLeft || 0;
    
    var top  = box.top +  scrollTop - clientTop;
    var left = box.left + scrollLeft - clientLeft;
    
    return { top: Math.round(top), left: Math.round(left), height: box.height, width: box.width };
  }
}
