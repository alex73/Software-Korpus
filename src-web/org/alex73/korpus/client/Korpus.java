/**************************************************************************
 Korpus - Corpus Linguistics Software.

 Copyright (C) 2013-2015 Aleś Bułojčyk (alex73mail@gmail.com)
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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.alex73.korpus.base.BelarusianTags;
import org.alex73.korpus.client.controls.VisibleElement;
import org.alex73.korpus.shared.dto.ClusterParams;
import org.alex73.korpus.shared.dto.ClusterResults;
import org.alex73.korpus.shared.dto.SearchParams;
import org.alex73.korpus.shared.dto.SearchResults;
import org.alex73.korpus.shared.dto.WordResult;

import com.google.gwt.core.client.EntryPoint;
import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.Element;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.dom.client.MouseDownEvent;
import com.google.gwt.event.dom.client.MouseDownHandler;
import com.google.gwt.event.dom.client.MouseOutEvent;
import com.google.gwt.event.dom.client.MouseOutHandler;
import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.History;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Anchor;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.DecoratedPopupPanel;
import com.google.gwt.user.client.ui.Grid;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.HTMLPanel;
import com.google.gwt.user.client.ui.InlineLabel;
import com.google.gwt.user.client.ui.RootPanel;
import com.google.gwt.user.client.ui.UIObject;

/**
 * Entry point classes define <code>onModuleLoad()</code>.
 */
public class Korpus implements EntryPoint {

    private final SearchServiceAsync searchService = GWT.create(SearchService.class);

    SearchControls searchControls;

    ResultsSearch resultsSearch;
    ResultsConcordance resultsConcordance;
    ResultsCluster resultsCluster;

    DecoratedPopupPanel docInfoPopup;

    List<int[]> pages = new ArrayList<int[]>();
    ClusterResults clusterResults;
    boolean hasMore;
    SearchService.LatestMark latestMark;
    boolean search;
    boolean cluster;

    SearchParams currentSearchParams;
    ClusterParams currentClusterParams;
    Map<Anchor, SearchResults> widgetsInfoDoc = new HashMap<Anchor, SearchResults>();
    Map<InlineLabel, WordResult> widgetsInfoWord = new HashMap<InlineLabel, WordResult>();
    
    public void onModuleLoad() {
        Button btnSearch = Button.wrap(DOM.getElementById("btnSearch"));
        btnSearch.addClickHandler(searchHandler);

        Element elBtnAddWord = DOM.getElementById("btnAddWord");
        if (elBtnAddWord != null) {
            Button btnAddWord = Button.wrap(elBtnAddWord);
            btnAddWord.addClickHandler(addWordhandler);
        }

        searchControls = new SearchControls(History.getToken(), searchService, this);

        search = searchControls.orderPreset != null;
        cluster = elBtnAddWord == null;

        if (searchControls.words.isEmpty()) {
            addWordhandler.onClick(null);
        }
        if (search) {
            resultsSearch = new ResultsSearch(this);
            RootPanel.get("resultTable").add(resultsSearch);
        } else if (cluster) {
            resultsCluster = new ResultsCluster(this);
            RootPanel.get("resultTable").add(resultsCluster);
        } else {
            resultsConcordance = new ResultsConcordance(this);
            RootPanel.get("resultTable").add(resultsConcordance);
        }
        
//        Element statKorpusDiv = DOM.getElementById("statKorpus");
//        if (statKorpusDiv!=null) {
//            HTMLPanel.wrap(statKorpusDiv);
//        }

        docInfoPopup = new DecoratedPopupPanel(true);
        docInfoPopup.ensureDebugId("cwBasicPopup-simplePopup");
        docInfoPopup.addStyleName("popupDocDetails");
        docInfoPopup.setWidget(new HTML("about word<br/>next"));

        VisibleElement mainPart = new VisibleElement(DOM.getElementById("mainPart"));
        mainPart.setVisible(true);
    }

    ClickHandler handlerShowInfoDoc = new ClickHandler() {
        public void onClick(ClickEvent event) {
            SearchResults doc = widgetsInfoDoc.get(event.getSource());
            if (doc == null) {
                return;
            }

            docInfoPopup.setWidget(new DocumentDetailsPanel(doc, Korpus.this));
            docInfoPopup.showRelativeTo((UIObject) event.getSource());
        }
    };

    MouseDownHandler handlerShowInfoWord = new MouseDownHandler() {
        @Override
        public void onMouseDown(MouseDownEvent event) {
            WordResult info = widgetsInfoWord.get(event.getSource());
            if (info == null) {
                return;
            }
            String text = "Lemma:" + info.lemma;
            if (info.cat != null) {
                String[] grammar = info.cat.split("_");
                for (String g : grammar) {
                    if (!BelarusianTags.getInstance().isValid(g, null)) {
                        // ёсьць невядомыя пазыцыі
                        text += "<br/>" + g;
                    } else {
                        text += "<br/>" + g + ": ";
                        try {
                            List<String> descr = BelarusianTags.getInstance().describe(g);
                            for (int i = 0; i < descr.size(); i++) {
                                if (i > 0) {
                                    text += ", ";
                                }
                                text += descr.get(i);
                            }
                        } catch (Exception ex) {
                            ex.printStackTrace();
                        }
                    }
                }
            }
            docInfoPopup.setWidget(new HTML(text));
            docInfoPopup.showRelativeTo((UIObject) event.getSource());
        }
    };

    MouseOutHandler handlerHideInfoDoc = new MouseOutHandler() {
        @Override
        public void onMouseOut(MouseOutEvent event) {
            docInfoPopup.hide();
        }
    };

    void outStat(SearchService.InitialData result) {
//        outStatTable("statKorpus", result.statKorpus);
//        outStatTable("statOther", result.statOther);
    }

    void outStatTable(String id, Map<String, Integer> data) {
        Element div = DOM.getElementById(id);
        if (div != null) {
           
            Grid g = new Grid(2, 3);
            g.setText(0, 1, "Тэкстаў");
            g.setText(0, 2, "Слоў");

            g.setText(1, 0, "Агулам");
            g.setText(1, 1, "" + data.get("texts"));
            g.setText(1, 2, "" + data.get("words"));
            
            Window.alert("show21 "+id);
            HTMLPanel.wrap(div).add(g);
            Window.alert("show31 "+id);
        }else {
            Window.alert("none "+id);
        }
    }

    void requestPageDetails(final int pageIndex) {
        searchControls.errorMessage.setText("Дэталі...");
        try {
            int[] req = pages.get(pageIndex);
            searchService.getSentences(currentSearchParams, req, new AsyncCallback<SearchResults[]>() {
                @Override
                public void onSuccess(SearchResults[] result) {
                    if (search) {
                        resultsSearch.showResults(pageIndex, result);
                    } else {
                        resultsConcordance.showResults(pageIndex, result, currentSearchParams.words.size());
                    }
                }

                @Override
                public void onFailure(Throwable caught) {
                    searchControls.errorMessage.setText("Памылка сэрвера: " + caught.getMessage());
                }
            });
        } catch (Exception ex) {
            searchControls.errorMessage.setText("Памылка сэрвера: " + ex.getMessage());
        }
    }

    void showEmptyResults() {
        searchControls.errorMessage.setText("Нічога не знойдзена");
        widgetsInfoDoc.clear();
        widgetsInfoWord.clear();
        if (resultsSearch != null) {
            resultsSearch.clear();
        }
        if (resultsConcordance != null) {
            resultsConcordance.clear();
        }
    }

    ClickHandler addWordhandler = new ClickHandler() {
        @Override
        public void onClick(ClickEvent event) {
            searchControls.addWord();
        }
    };

    ClickHandler searchHandler = new ClickHandler() {
        @Override
        public void onClick(ClickEvent event) {
            searchControls.errorMessage.setText("Пошук...");

            pages.clear();
            hasMore = false;
            latestMark = null;

            History.newItem(searchControls.exportParameters());
            try {
                if (cluster) {
                    currentClusterParams = searchControls.createClusterRequest();
                    searchService.calculateClusters(currentClusterParams,
                            new AsyncCallback<ClusterResults>() {
                                @Override
                                public void onSuccess(ClusterResults result) {
                                    clusterResults = result;
                                    resultsCluster.showResults(0);
                                }

                                @Override
                                public void onFailure(Throwable caught) {
                                    searchControls.errorMessage.setText("Памылка сэрвера: "
                                            + caught.getMessage());
                                }
                            });
                } else {
                    currentSearchParams = searchControls.createRequest();
                    searchService.search(currentSearchParams, null,
                            new AsyncCallback<SearchService.SearchResult>() {
                                @Override
                                public void onSuccess(SearchService.SearchResult result) {
                                    latestMark = result.latest;
                                    hasMore = result.hasMore;
                                    if (result.foundIDs.length > 0) {
                                        pages.add(result.foundIDs);
                                        requestPageDetails(pages.size() - 1);
                                    } else {
                                        showEmptyResults();
                                    }
                                }

                                @Override
                                public void onFailure(Throwable caught) {
                                    searchControls.errorMessage.setText("Памылка сэрвера: "
                                            + caught.getMessage());
                                }
                            });
                }
            } catch (Exception ex) {
                searchControls.errorMessage.setText("Памылка сэрвера: " + ex.getMessage());
            }
        }
    };

    ClickHandler nextPageHandler = new ClickHandler() {
        public void onClick(ClickEvent event) {
            searchControls.errorMessage.setText("Пошук...");

            hasMore = false;
            try {
                searchService.search(currentSearchParams, latestMark,
                        new AsyncCallback<SearchService.SearchResult>() {
                            @Override
                            public void onSuccess(SearchService.SearchResult result) {
                                latestMark = result.latest;
                                hasMore = result.hasMore;
                                pages.add(result.foundIDs);
                                requestPageDetails(pages.size() - 1);
                            }

                            @Override
                            public void onFailure(Throwable caught) {
                                searchControls.errorMessage.setText("Памылка сэрвера: " + caught.getMessage());
                            }
                        });
            } catch (Exception ex) {
                searchControls.errorMessage.setText("Памылка сэрвера: " + ex.getMessage());
            }
        }
    };
}
