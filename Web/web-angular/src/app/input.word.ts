import { Component, Input, ViewChild, OnInit } from '@angular/core';
import { WordRequest } from './dto/WordRequest';
import { GrammarLetter, KeyValue, DBTagsGroups, Group } from './dto/GrammarInitial';
import { ModalDirective } from 'ng2-bootstrap';

@Component({
  selector: 'input-word',
  templateUrl: './input.word.html'
})

export class InputWord {
  @Input() word: WordRequest;
  @Input() displayRemove: boolean;
  @Input() removeFunction: Function;
  @Input() grammarTree: { [key:string]:GrammarLetter; } = {};
  @Input() grammarWordTypes: KeyValue[] = [];
  @Input() grammarWordTypesGroups: { [key:string]:DBTagsGroups; };
  
  @ViewChild('wordgramModal') public modal:ModalDirective;
  
  public grmains: Gr[] = [];
  public grselected: string = "";
  public grchecked: { [key:string]:any; } = {};
  
  remove() {
    this.removeFunction(this.word);
  }
  
  grammarClick() {
    this.grmains = [];
    for(let g in this.grammarTree) {
      let gr = new Gr();
      gr.letter = g;
      gr.desc = this.grammarTree[g].desc;
      this.grmains.push(gr);
    }
    this.grselected = '';
    this.grchecked = {};
    for(let gr of this.grammarWordTypes) {
      for(let group of this.grammarWordTypesGroups[gr.key].groups) {
        this.grchecked[group.name] = {}
      }
    }
    
    if (this.word.grammar) {
      this.grselected = this.word.grammar.charAt(0);
      let grindex: number = 0;
      for(let i=1; i<this.word.grammar.length; i++) {
        let group = this.grammarWordTypesGroups[this.grselected].groups[grindex];
        let ch: string = this.word.grammar.charAt(i);
        if (ch == '[') {
          for(;i<this.word.grammar.length; i++) {
            ch = this.word.grammar.charAt(i);
            if (ch == ']') {
              break;
            } else {
              this.grchecked[group.name][ch] = true;
            }
          }
        } else if (ch == '.') {
        } else {
          this.grchecked[group.name][ch] = true;
        }
        grindex++;
      }
    }
    
    this.modal.show();
  }
  
  wordgramModalOk() {
    if (this.grselected) {
	    let r: string = this.grselected;
	    
	    let db: DBTagsGroups = this.grammarWordTypesGroups[this.grselected];
	    for(let group of db.groups) {
	      r += this.collect(group);
	    }
	    
	    this.word.grammar = r;
    } else {
        this.word.grammar = null;
    }
    this.word.updateDisplay(this.grammarTree);

    this.modal.hide();
  }
  collect(group: Group) {
    let r: string = '';
    for(let it of group.items) {
      if (this.grchecked[group.name][it.code]) {
        r += it.code;
      }
    }
    if (r.length == 0 || r.length == group.items.length) {
      r = '.';
    } else if (r.length > 1) {
      r = '[' + r + ']';
    }
    return r;
  }
  
  wordgramModalCancel() {
    this.modal.hide();
  }
}

export class Gr {
  public letter: string;
  public desc: string;
}
