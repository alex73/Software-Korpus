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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.alex73.korpus.base.BelarusianTags;
import org.alex73.korpus.client.controls.VisibleElement;
import org.alex73.korpus.shared.ResultSentence;
import org.alex73.korpus.shared.SearchChecks;
import org.alex73.korpus.shared.SearchParams;

import com.google.gwt.core.client.EntryPoint;
import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.dom.client.MouseDownEvent;
import com.google.gwt.event.dom.client.MouseDownHandler;
import com.google.gwt.event.dom.client.MouseOutEvent;
import com.google.gwt.event.dom.client.MouseOutHandler;
import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.History;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Anchor;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.DecoratedPopupPanel;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.HTMLPanel;
import com.google.gwt.user.client.ui.InlineLabel;
import com.google.gwt.user.client.ui.RootPanel;
import com.google.gwt.user.client.ui.UIObject;
import com.google.gwt.user.client.ui.VerticalPanel;

/**
 * Entry point classes define <code>onModuleLoad()</code>.
 */
public class Korpus implements EntryPoint {

    private final SearchServiceAsync searchService = GWT.create(SearchService.class);

    SearchControls searchControls;

    VerticalPanel resultTable;

    DecoratedPopupPanel docInfoPopup;

    List<int[]> pages = new ArrayList<int[]>();
    boolean hasMore;
    SearchService.LatestMark latestMark;

    public void onModuleLoad() {
        Button btnSearch = Button.wrap(DOM.getElementById("btnSearch"));
        btnSearch.addClickHandler(searchHandler);

        Button btnAddWord = Button.wrap(DOM.getElementById("btnAddWord"));
        btnAddWord.addClickHandler(addWordhandler);

        searchControls = new SearchControls(History.getToken(), searchService);

        if (searchControls.words.isEmpty()) {
            addWordhandler.onClick(null);
        }
        searchControls.validator.onChange(null);

        resultTable = new VerticalPanel();
        RootPanel.get("resultTable").add(resultTable);

        docInfoPopup = new DecoratedPopupPanel(true);
        docInfoPopup.ensureDebugId("cwBasicPopup-simplePopup");
        docInfoPopup.addStyleName("popupDocDetails");
        docInfoPopup.setWidget(new HTML("about word<br/>next"));

        VisibleElement mainPart = new VisibleElement(DOM.getElementById("mainPart"));
        mainPart.setVisible(true);
    }

    SearchParams curentParams;
    Map<Anchor, ResultSentence> widgetsInfoDoc = new HashMap<Anchor, ResultSentence>();
    Map<InlineLabel, ResultSentence.Word> widgetsInfoWord = new HashMap<InlineLabel, ResultSentence.Word>();

    ClickHandler handlerShowInfoDoc = new ClickHandler() {
        public void onClick(ClickEvent event) {
            ResultSentence doc = widgetsInfoDoc.get(event.getSource());
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
            ResultSentence.Word info = widgetsInfoWord.get(event.getSource());
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

    void requestPageDetails(final int pageIndex) {
        searchControls.errorMessage.setText("Дэталі...");
        try {
            int[] req = pages.get(pageIndex);
            searchService.getSentences(req, new AsyncCallback<ResultSentence[]>() {
                @Override
                public void onSuccess(ResultSentence[] result) {
                    showResults(pageIndex, result);
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
        resultTable.clear();
    }

    void showResults(int pageIndex, ResultSentence[] sentences) {
        int c = 0;
        for (int[] p : pages) {
            c += p.length;
        }
        if (hasMore) {
            searchControls.errorMessage.setText("Знойдзена больш за " + c);
        } else {
            searchControls.errorMessage.setText("Знойдзена: " + c);
        }

        widgetsInfoDoc.clear();
        widgetsInfoWord.clear();
        resultTable.clear();

        resultTable.add(createPagesIndexPanel(pageIndex));

        for (ResultSentence s : sentences) {
            HTMLPanel p = new HTMLPanel("");
            Anchor doclabel = new Anchor("падрабязней... ");
            doclabel.addClickHandler(handlerShowInfoDoc);
            // doclabel.addMouseOutHandler(handlerHideInfoDoc);

            p.add(doclabel);

            int firstFoundWord = -1;
            for (int i = 0; i < s.words.length && firstFoundWord < 0; i++) {
                if (SearchChecks.isFoundWord(curentParams, s, i)) {
                    firstFoundWord = i;
                    break;
                }
            }
            int begWord = Math.max(firstFoundWord - 6, 0);
            int endWord = Math.min(s.words.length - 1, firstFoundWord + 6);
            for (int i = begWord; i <= endWord; i++) {
                ResultSentence.Word w = s.words[i];
                String text;
                if (w.value.equals(",") || w.value.equals(".")) {
                    text = w.value;
                } else {
                    text = " " + w.value;
                }
                InlineLabel wlabel = new InlineLabel(text);
                if (SearchChecks.isFoundWord(curentParams, s, i)) {
                    wlabel.setStyleName("wordFound");
                }
                wlabel.addMouseDownHandler(handlerShowInfoWord);
                p.add(wlabel);
                widgetsInfoWord.put(wlabel, w);
            }

            widgetsInfoDoc.put(doclabel, s);

            resultTable.add(p);
        }

        resultTable.add(createPagesIndexPanel(pageIndex));
    }

    HTMLPanel createPagesIndexPanel(int currentPageIndex) {
        HTMLPanel panelPages = new HTMLPanel("<br/><br/>");
        for (int i = 0; i < pages.size(); i++) {
            if (i == currentPageIndex) {
                InlineLabel next = new InlineLabel(Integer.toString(i + 1));
                next.setStyleName("pageIndex");
                panelPages.add(next);
            } else {
                Anchor next = new Anchor(Integer.toString(i + 1));
                next.setStyleName("pageIndex");
                final int pi = i;
                next.addClickHandler(new ClickHandler() {
                    public void onClick(ClickEvent event) {
                        requestPageDetails(pi);
                    }
                });
                panelPages.add(next);
            }
        }
        if (hasMore) {
            Anchor next = new Anchor("Наступная старонка...");
            next.setStyleName("pageIndex");
            next.addClickHandler(nextPageHandler);
            panelPages.add(next);
        }
        panelPages.add(new HTMLPanel("<br/><br/>"));

        return panelPages;
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
                final SearchParams params = searchControls.createRequest();
                curentParams = params;
                searchService.search(params, null, new AsyncCallback<SearchService.SearchResult>() {
                    @Override
                    public void onSuccess(SearchService.SearchResult result) {
                        curentParams = result.params;
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
                        searchControls.errorMessage.setText("Памылка сэрвера: " + caught.getMessage());
                    }
                });
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
                searchService.search(curentParams, latestMark,
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
