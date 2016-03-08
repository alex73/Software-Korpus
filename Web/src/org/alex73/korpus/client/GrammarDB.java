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

import org.alex73.korpus.base.DBTagsGroups;
import org.alex73.korpus.client.controls.VisibleElement;
import org.alex73.korpus.client.controls.WordGrammarPopup;
import org.alex73.korpus.shared.LemmaInfo;

import com.google.gwt.core.client.EntryPoint;
import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ChangeEvent;
import com.google.gwt.event.dom.client.ChangeHandler;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.logical.shared.CloseEvent;
import com.google.gwt.event.logical.shared.CloseHandler;
import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.History;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Anchor;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.DecoratedPopupPanel;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.InlineLabel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.ListBox;
import com.google.gwt.user.client.ui.PopupPanel;
import com.google.gwt.user.client.ui.PopupPanel.PositionCallback;
import com.google.gwt.user.client.ui.RootPanel;
import com.google.gwt.user.client.ui.SimpleRadioButton;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.VerticalPanel;

/**
 * UI for find by grammar database.
 */
public class GrammarDB implements EntryPoint {

    private final GrammarServiceAsync grammarService = GWT.create(GrammarService.class);

    Label errorMessage;
    SimpleRadioButton orderNormal, orderReverse;
    TextBox wordPart, lemma;
    Anchor details;
    String wordGrammar;
    ListBox list;
    VerticalPanel resultTable;
    DecoratedPopupPanel lemmaInfoPopup;

    public void onModuleLoad() {
        Button btnSearch = Button.wrap(DOM.getElementById("btnSearch"));
        btnSearch.addClickHandler(submitHandler);

        errorMessage = Label.wrap(DOM.getElementById("errorMessage"));
        wordPart = TextBox.wrap(DOM.getElementById("wordPart"));
        lemma = TextBox.wrap(DOM.getElementById("lemma"));
        list = ListBox.wrap(DOM.getElementById("list"));
        details = Anchor.wrap(DOM.getElementById("details"));
        orderNormal = SimpleRadioButton.wrap(DOM.getElementById("orderNormal"));
        orderReverse = SimpleRadioButton.wrap(DOM.getElementById("orderReverse"));

        list.addItem("-----", "");
        for (DBTagsGroups.KeyValue en : DBTagsGroups.getWordTypes()) {
            list.addItem(en.value, en.key);
        }

        list.addChangeHandler(listHandler);
        details.setVisible(false);
        details.addClickHandler(detailsHandler);

        importParameters(History.getToken());

        resultTable = new VerticalPanel();
        RootPanel.get("resultTable").add(resultTable);

        lemmaInfoPopup = new DecoratedPopupPanel(true);
        lemmaInfoPopup.ensureDebugId("cwBasicPopup-simplePopup");
        lemmaInfoPopup.addStyleName("popupDocDetails");
        lemmaInfoPopup.setWidget(new HTML("about lemma<br/>next"));

        VisibleElement mainPart = new VisibleElement(DOM.getElementById("mainPart"));
        mainPart.setVisible(true);
    }

    ChangeHandler listHandler = new ChangeHandler() {
        @Override
        public void onChange(ChangeEvent event) {
            String c = list.getValue(list.getSelectedIndex());
            if (c.isEmpty()) {
                wordGrammar = null;
                details.setVisible(false);
            } else {
                wordGrammar = c + ".*";
                DBTagsGroups tagsList = DBTagsGroups.getTagGroupsByWordType().get(c.charAt(0));
                details.setVisible(!tagsList.groups.isEmpty());
            }
        }
    };

    ClickHandler detailsHandler = new ClickHandler() {
        @Override
        public void onClick(ClickEvent event) {
            int vi = list.getSelectedIndex();
            String v = list.getValue(vi);
            if (v.length() > 0) {
                DBTagsGroups tagsList = DBTagsGroups.getTagGroupsByWordType().get(v.charAt(0));
                final WordGrammarPopup popup = new WordGrammarPopup(tagsList);
                if (!".*".equals(wordGrammar.substring(1))) {
                    popup.setSelected(wordGrammar.substring(1));
                }
                popup.addCloseHandler(new CloseHandler<PopupPanel>() {
                    @Override
                    public void onClose(CloseEvent<PopupPanel> event) {
                        String c = list.getValue(list.getSelectedIndex());
                        wordGrammar = c + popup.getSelected();
                    }
                });
                popup.showRelativeTo(details);
                popup.setPopupPositionAndShow(new PositionCallback() {
                    public void setPosition(int offsetWidth, int offsetHeight) {
                        popup.setPopupPosition(0, details.getAbsoluteTop() + details.getOffsetHeight());
                    }
                });
            }
        }
    };

    ClickHandler submitHandler = new ClickHandler() {
        @Override
        public void onClick(ClickEvent event) {
            errorMessage.setText("Пошук...");

            History.newItem(exportParameters());
            try {
                String wordMask = wordPart.getValue().isEmpty() ? null : "*" + wordPart.getValue() + "*";
                String lemmaMask = lemma.getValue();
                grammarService.search(lemmaMask, wordMask, wordGrammar, Boolean.TRUE.equals(orderReverse.getValue()),
                        new AsyncCallback<LemmaInfo[]>() {
                            @Override
                            public void onSuccess(LemmaInfo[] result) {
                                resultTable.clear();
                                for (LemmaInfo w : result) {
                                    HorizontalPanel p = new HorizontalPanel();

                                    p.add(new InlineLabel("Лема: "));
                                    AnchorLemma a = new AnchorLemma(w);
                                    a.addClickHandler(handlerShowLemmaInfo);
                                    p.add(a);

                                    resultTable.add(p);
                                }
                                errorMessage.setText("");
                                if (result.length==0) {
                                    errorMessage.setText("Нічога не знойдзена");
                                }
                            }

                            @Override
                            public void onFailure(Throwable caught) {
                                errorMessage.setText("Памылка сэрвера: " + caught.getMessage());
                            }
                        });
            } catch (Exception ex) {
                errorMessage.setText("Памылка сэрвера: " + ex.getMessage());
            }
        }
    };

    String exportParameters() {
        String s = "";
        if (lemma.getValue() != null && !lemma.getValue().isEmpty()) {
            s += "&lemma=" + lemma.getValue();
        }
        if (wordPart.getValue() != null && !wordPart.getValue().isEmpty()) {
            s += "&wordPart=" + wordPart.getValue();
        }
        if (wordGrammar != null && !wordGrammar.isEmpty()) {
            s += "&grammar=" + wordGrammar;
        }
        if (Boolean.TRUE.equals(orderReverse.getValue())) {
            s += "&order=reverse";
        }
        return s.isEmpty() ? "" : s.substring(1);
    }

    public void importParameters(String params) {
        for (String p : params.split("&")) {
            int pos = p.indexOf('=');
            if (pos > 0) {
                String key = p.substring(0, pos);
                String value = p.substring(pos + 1);
                if ("wordPart".equals(key)) {
                    wordPart.setValue(value);
                } else if ("lemma".equals(key)) {
                    lemma.setValue(value);
                } else if ("grammar".equals(key)) {
                    wordGrammar = value;
                    for (int i = 0; i < list.getItemCount(); i++) {
                        if (wordGrammar.substring(0, 1).equals(list.getValue(i))) {
                            list.setSelectedIndex(i);
                            break;
                        }
                    }
                    listHandler.onChange(null);
                } else if ("order".equals(key)) {
                    orderReverse.setValue("reverse".equals(value));
                }
            }
        }
    }

    public static class AnchorLemma extends Anchor {
        public final LemmaInfo lemma;

        public AnchorLemma(LemmaInfo lemma) {
            this.lemma = lemma;
            setText(lemma.lemma);
        }
    }

    ClickHandler handlerShowLemmaInfo = new ClickHandler() {
        public void onClick(ClickEvent event) {
            errorMessage.setText("Дэталі...");
            final AnchorLemma info = (AnchorLemma) event.getSource();
            try {
                grammarService.getLemmaDetails(info.lemma, new AsyncCallback<LemmaInfo[]>() {
                    @Override
                    public void onSuccess(LemmaInfo[] result) {
                        errorMessage.setText("");

                        lemmaInfoPopup.setWidget(new GrammarDBLemmaInfo(result, GrammarDB.this));
                        lemmaInfoPopup.showRelativeTo(info);
                    }

                    @Override
                    public void onFailure(Throwable caught) {
                        errorMessage.setText("Памылка сэрвера: " + caught.getMessage());
                    }
                });
            } catch (Exception ex) {
                errorMessage.setText("Памылка сэрвера: " + ex.getMessage());
            }
        }
    };
}
