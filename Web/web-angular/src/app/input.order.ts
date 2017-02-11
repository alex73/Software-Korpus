import { Component } from '@angular/core';
import { ServiceSearch } from './service.search';

@Component({
  selector: 'input-order',
  templateUrl: "./input.order.html"
})

export class InputOrder {
  constructor(private serviceSearch: ServiceSearch) {}
}
