import { Component, ElementRef, ViewChild, Input, ChangeDetectorRef } from '@angular/core';
import { ServiceGrammar } from './service.grammar';
import { WordResult } from './dto/WordResult';
import { GrammarInitial, GrammarLetter } from './dto/GrammarInitial';
import { DirectiveWord }   from './directive.word';
import { ModalDirective, PopoverDirective } from 'ng2-bootstrap';
import { SearchResultsText } from './dto/ResultTypes';
import { LemmaInfo, LemmaParadigm, LemmaVariant, LemmaForm } from './dto/LemmaInfo';

@Component({
  selector: 'output-grammar',
  templateUrl: "./output.grammar.html"
})

export class OutputGrammar {
  @ViewChild('lemmaModal') public lemmaModal: ModalDirective;

  public details: OutGrammarParadigm;

  constructor (public serviceGrammar:ServiceGrammar) { }
  
  showDetails(lemma: LemmaInfo) {
    this.serviceGrammar.loadDetails(lemma, () => {
      this.details = this.convert(this.serviceGrammar.details);
      this.lemmaModal.show();
    });
    return false;
  }

  convert(p: LemmaParadigm): OutGrammarParadigm {
    let r: OutGrammarParadigm = new OutGrammarParadigm();
    r.lemma = p.lemma;
    r.catText = GrammarInitial.text(p.tag, this.serviceGrammar.initial.grammarTree);
    r.subtree = GrammarInitial.subtree(p.tag, this.serviceGrammar.initial.grammarTree);
    r.variants = [];
    for(let v of p.variants) {
      let rv: OutGrammarVariant = new OutGrammarVariant();
      rv.forms = [];
      rv.catnames = [];
      
      for(let f of v.forms) {
        let gr = r.subtree;
        for(let c of f.tag.split('')) {
          if (gr) {
            let g = gr[c];
            if (g) {
              if (rv.catnames.indexOf(g.name)<0) {
                rv.catnames.push(g.name);
              }
              gr = g.ch;
            }
          } else {
            break;
          }
        }
      }

      for(let f of v.forms) {
        let rf: OutGrammarForm = new OutGrammarForm();
        rf.value = f.value;
        rf.data = [];
        rf.colspan = [];

        let gr = r.subtree;
        for(let c of f.tag.split('')) {
          if (gr) {
            let g = gr[c];
            if (g) {
              let idx: number = rv.catnames.indexOf(g.name);
              rf.data[idx] = g.desc;
              gr = g.ch;
            }
          } else {
            break;
          }
        }
        for(let i = 0; i < rv.catnames.length; i++) {
          if (rf.data[i]) {
          } else {
            rf.data[i] = "";
          }
        }
        rv.forms.push(rf);
      }
      for(let i = 0; i < rv.catnames.length; i++) {
        for(let fi: number = 0; fi < rv.forms.length; fi++) {
          rv.forms[fi].colspan[i] = 1;
        }
        for(let fi: number = rv.forms.length-1; fi > 0; fi--) {
          if (rv.forms[fi].data[i] == rv.forms[fi-1].data[i]) {
            rv.forms[fi-1].colspan[i] += rv.forms[fi].colspan[i];
            rv.forms[fi].colspan[i] = 0;
          }
        }
      }
      r.variants.push(rv);
    }
    return r;
  }
}

export class OutGrammarParadigm {
    public lemma: string;
    public variants: OutGrammarVariant[];

    public catText: string;
    public subtree: { [key:string]:GrammarLetter; };
}

export class OutGrammarVariant {
    public forms: OutGrammarForm[];
    public catnames: string[];
}

export class OutGrammarForm {
    public tag: string;
    public value: string;
    public data: string[];
    public colspan: number[];
}
