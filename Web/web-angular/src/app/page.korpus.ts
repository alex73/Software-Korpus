import { Component, ElementRef, ViewChild } from '@angular/core';
import { TabsModule, AlertModule } from 'ng2-bootstrap';
import { InputWord }   from './input.word';
import { InputWordAround }   from './input.word.around';
import { ServiceSearch } from './service.search';

@Component({
  selector: 'page-korpus',
  providers: [ ServiceSearch ],
  templateUrl: './page.korpus.html'
})

export class PageKorpus {
  @ViewChild('btnShowSearch')   btnShowSearchRef:ElementRef;
  @ViewChild('btnShowKwic')     btnShowKwicRef:ElementRef;
  @ViewChild('btnShowCluster')  btnShowClusterRef:ElementRef;
  @ViewChild('divInputSearch')  divInputSearchRef:ElementRef;
  @ViewChild('divInputKwic')    divInputKwicRef:ElementRef;
  @ViewChild('divInputCluster') divInputClusterRef:ElementRef;
  
    public isCollapsed: boolean = true;
  public c() {
    console.log('zzzz');
  }
  

  constructor (private serviceSearch: ServiceSearch) {
    this.serviceSearch.minimizeSearch = () => {
      this.btnShowSearchRef.nativeElement.style.display = 'block';
      this.divInputSearchRef.nativeElement.style.display = 'none';
    };
    this.serviceSearch.maximizeSearch = () => {
      this.btnShowSearchRef.nativeElement.style.display = 'none';
      this.divInputSearchRef.nativeElement.style.display = 'block';
    };
    this.serviceSearch.minimizeKwic = () => {
      this.btnShowKwicRef.nativeElement.style.display = 'block';
      this.divInputKwicRef.nativeElement.style.display = 'none';
    };
    this.serviceSearch.maximizeKwic = () => {
      this.btnShowKwicRef.nativeElement.style.display = 'none';
      this.divInputKwicRef.nativeElement.style.display = 'block';
    };
    this.serviceSearch.minimizeCluster = () => {
      this.btnShowClusterRef.nativeElement.style.display = 'block';
      this.divInputClusterRef.nativeElement.style.display = 'none';
    };
    this.serviceSearch.maximizeCluster = () => {
      this.btnShowClusterRef.nativeElement.style.display = 'none';
      this.divInputClusterRef.nativeElement.style.display = 'block';
    };
  }

  showInputSearch() {
    this.btnShowSearchRef.nativeElement.style.display = 'none';
    this.divInputSearchRef.nativeElement.style.display = 'block';
  }
  showInputKwic() {
    this.btnShowKwicRef.nativeElement.style.display = 'none';
    this.divInputKwicRef.nativeElement.style.display = 'block';
  }
  showInputCluster() {
    this.btnShowClusterRef.nativeElement.style.display = 'none';
    this.divInputClusterRef.nativeElement.style.display = 'block';
  }
  checkRemove() :boolean {
    return this.serviceSearch.inputWords.length > 1;
  }
}
