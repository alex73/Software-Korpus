import { Component, Input } from '@angular/core';
import { ServiceSearch } from './service.search';

@Component({
  selector: 'input-word-around',
  templateUrl: './input.word.around.html'
})

export class InputWordAround {
  constructor(private serviceSearch: ServiceSearch) {}
}
