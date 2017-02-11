import { Component } from '@angular/core';
import { PageKorpus } from './page.korpus';
import { InputWord }   from './input.word';
import { WordRequest } from './dto/WordRequest';
import { ServiceSearch } from './service.search';

@Component({
  selector: 'input-word-add',
  templateUrl: './input.word.add.html'
})

export class InputWordAdd {
  constructor(private serviceSearch: ServiceSearch) { }
}
