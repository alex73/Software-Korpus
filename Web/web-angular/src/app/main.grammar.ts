/**
 * Startup code.
 */
import { enableProdMode } from '@angular/core';
import { platformBrowserDynamic } from '@angular/platform-browser-dynamic';
import { ConnectionBackend } from '@angular/http';
if (process.env.ENV === 'production') {
    enableProdMode();
}

import { ModuleGrammar } from './module.grammar';
import { ServiceSearch } from './service.search';

platformBrowserDynamic().bootstrapModule(ModuleGrammar, [ServiceSearch, ConnectionBackend]);
