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

import java.io.Serializable;
import java.util.List;
import java.util.Map;

import org.alex73.korpus.shared.dto.ClusterParams;
import org.alex73.korpus.shared.dto.ClusterResults;
import org.alex73.korpus.shared.dto.LatestMark;
import org.alex73.korpus.shared.dto.SearchParams;
import org.alex73.korpus.shared.dto.SearchResults;

import com.google.gwt.user.client.rpc.RemoteService;
import com.google.gwt.user.client.rpc.RemoteServiceRelativePath;

@RemoteServiceRelativePath("search")
public interface SearchService extends RemoteService {
    SearchResult search(SearchParams params, LatestMark latest) throws Exception;

    SearchResults[] getSentences(SearchParams params, int[] list) throws Exception;

    ClusterResults calculateClusters(ClusterParams params) throws Exception;

    /**
     * Get initial values for display.
     */
    InitialData getInitialData() throws Exception;

    public static class SearchResult implements Serializable {
        public int[] foundIDs;
        public LatestMark latest;
        public boolean hasMore;
    }

    public static class InitialData implements Serializable {
        public List<String> authors;
        public List<String> volumes;
        public Map<String,Integer> statKorpus, statOther;
    }
}