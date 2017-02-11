import { Component, ElementRef, ViewChild } from '@angular/core';
import { AlertModule } from 'ng2-bootstrap';
import { InputWord }   from './input.word';
import { ServiceGrammar } from './service.grammar';

@Component({
  selector: 'page-grammar',
  providers: [ ServiceGrammar ],
  templateUrl: './page.grammar.html'
})

export class PageGrammar {

  constructor (private serviceGrammar: ServiceGrammar) {
  }

}
