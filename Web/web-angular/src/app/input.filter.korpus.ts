import { Component, ViewChild } from '@angular/core';
import { TypeaheadModule } from 'ng2-bootstrap/typeahead';
import { ServiceSearch } from './service.search';
import { ModalModule, ModalDirective } from 'ng2-bootstrap';

@Component({
  selector: 'input-filter-korpus',
  templateUrl: './input.filter.korpus.html'
})

export class InputFilterKorpus {
  @ViewChild('styleModal') styleModal: ModalDirective;
  
  public displayStylegenres: string;
  
  public selectedGroups: { [key:string]:boolean; } = {};
  public selectedItems: { [key:string]:boolean; } = {};

  constructor(public serviceSearch:ServiceSearch) {
    this.updateLabel();
  }

  groupChange(p) {
    for (let k of this.serviceSearch.initial.styleGenres[p]) {
      this.selectedItems[p+'/'+k] = this.selectedGroups[p];
    }
  }

  all(v) {
    for (let p of this.serviceSearch.initial.styleGenresParts) {
      for (let k of this.serviceSearch.initial.styleGenres[p]) {
        this.selectedItems[p+'/'+k] = v;
      }
    }
  }
  
  updateLabel() {
    this.displayStylegenres = this.serviceSearch.inputTextStandard.stylegenres.join(', ');
    if (this.displayStylegenres) {
      if (this.displayStylegenres.length > 40) {
        this.displayStylegenres = this.displayStylegenres.substring(0,40)+'...';
      }
    } else {
      this.displayStylegenres = 'Усе';
    }
  }
  
  styleModalOpen() {
    this.selectedGroups = {};
    this.selectedItems = {};
    if (this.serviceSearch.inputTextStandard.stylegenres.length > 0) {
      for (let v of this.serviceSearch.inputTextStandard.stylegenres) {
        this.selectedItems[v] = true;
      }
    } else {
      this.all(true);
    }
    this.styleModal.show();
  }
  
  styleModalOk() {
    let r: string[] = [];
    let no: boolean = true;
    for (let p of this.serviceSearch.initial.styleGenresParts) {
      for (let k of this.serviceSearch.initial.styleGenres[p]) {
        if (this.selectedItems[p+'/'+k]) {
          r.push(p+'/'+k);
        } else {
          no = false;
        }
      }
    }
    this.serviceSearch.inputTextStandard.stylegenres = no ? []: r;
    this.updateLabel();
    this.styleModal.hide();
  }
  
  styleModalCancel() {
    this.styleModal.hide();
  }
}
