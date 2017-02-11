import { Directive, ElementRef, HostListener, Input } from '@angular/core';
import { WordResult } from './dto/WordResult';

@Directive({ selector: '[korpusWord]' })
export class DirectiveWord {

    constructor(private el: ElementRef) { }

    @Input()
    set korpusWordValue(v:WordResult) {
      if (v.isWord) {
        this.el.nativeElement.setAttribute("korpusWord.lemma", v.lemma);
        this.el.nativeElement.setAttribute("korpusWord.cat", v.cat);
        if (v.requestedWord) {
          this.el.nativeElement.style.color = "#F00";
        }
      } else {
        this.el.nativeElement.setAttribute("korpusWord.lemma", null);
        this.el.nativeElement.setAttribute("korpusWord.cat", null);
      }
    }
}
