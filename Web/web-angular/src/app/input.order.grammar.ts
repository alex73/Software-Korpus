import { Component } from '@angular/core';
import { ServiceGrammar } from './service.grammar';

@Component({
  selector: 'input-order-grammar',
  templateUrl: "./input.order.grammar.html"
})

export class InputOrderGrammar {
  constructor(private serviceGrammar: ServiceGrammar) {}
}
