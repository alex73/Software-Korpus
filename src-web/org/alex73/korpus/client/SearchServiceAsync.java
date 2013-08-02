/**************************************************************************
 Korpus - Corpus Linguistics Software.

 Copyright (C) 2013 Aleś Bułojčyk (alex73mail@gmail.com)
               Home page: https://sourceforge.net/projects/korpus/

 This file is part of Korpus.

 Korpus is free software: you can redistribute it and/or modify
 it under the terms of the GNU General Public License as published by
 the Free Software Foundation, either version 3 of the License, or
 (at your option) any later version.

 Korpus is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU General Public License for more details.

 You should have received a copy of the GNU General Public License
 along with this program.  If not, see <http://www.gnu.org/licenses/>.
 **************************************************************************/

package org.alex73.korpus.client;

import org.alex73.korpus.shared.ResultSentence;
import org.alex73.korpus.shared.SearchParams;

import com.google.gwt.user.client.rpc.AsyncCallback;

public interface SearchServiceAsync {
    void search(SearchParams params, SearchService.LatestMark latest, AsyncCallback<SearchService.SearchResult> callback)
            throws Exception;

    void getSentences(int[] list, AsyncCallback<ResultSentence[]> callback) throws Exception;

    void getInitialData(AsyncCallback<SearchService.InitialData> callback) throws Exception;
}
