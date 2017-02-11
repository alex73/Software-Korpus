/**
 * Main application module.
 */

import { NgModule } from '@angular/core';
import { BrowserModule } from '@angular/platform-browser';
import { HttpModule }    from '@angular/http';
import { FormsModule }   from '@angular/forms';

import { PageGrammar } from './page.grammar';
import { InputFilterKorpus }   from './input.filter.korpus';
import { InputWord }   from './input.word';
import { InputOrderGrammar }   from './input.order.grammar';
import { OutputGrammar }   from './output.grammar';
import { DirectiveWord }   from './directive.word';

import { AlertModule, ModalModule } from 'ng2-bootstrap';

@NgModule({
  imports:      [ BrowserModule, HttpModule, FormsModule, AlertModule.forRoot(), ModalModule.forRoot() ],
  declarations: [ PageGrammar, InputWord, InputOrderGrammar, OutputGrammar ],
  bootstrap:    [ PageGrammar ]
})

export class ModuleGrammar { }
