/**
 * Main application module.
 */

import { NgModule } from '@angular/core';
import { BrowserModule } from '@angular/platform-browser';
import { HttpModule }    from '@angular/http';
import { FormsModule }   from '@angular/forms';

import { PageKorpus } from './page.korpus';
import { InputFilterKorpus }   from './input.filter.korpus';
import { InputWord }   from './input.word';
import { InputWordAdd }   from './input.word.add';
import { InputWordAround }   from './input.word.around';
import { InputOrder }   from './input.order';
import { OutputSearch }   from './output.search';
import { OutputCluster }   from './output.cluster';
import { DirectiveWord }   from './directive.word';

import { TypeaheadModule, TabsModule, AlertModule, PopoverModule, ModalModule } from 'ng2-bootstrap';

@NgModule({
  imports:      [ BrowserModule, HttpModule, FormsModule, TypeaheadModule.forRoot(), TabsModule.forRoot(), AlertModule.forRoot(), PopoverModule.forRoot(), ModalModule.forRoot() ],
  declarations: [ PageKorpus, InputFilterKorpus, InputWord, InputWordAdd, InputWordAround, InputOrder, OutputSearch, OutputCluster, DirectiveWord ],
  bootstrap:    [ PageKorpus ]
})

export class ModuleSearch { }
