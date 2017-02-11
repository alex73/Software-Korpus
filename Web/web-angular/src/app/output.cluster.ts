import { Component } from '@angular/core';
import { ServiceSearch } from './service.search';

@Component({
  selector: 'output-cluster',
  templateUrl: "./output.cluster.html"
})

export class OutputCluster {
  constructor (public serviceSearch:ServiceSearch) { }
}
