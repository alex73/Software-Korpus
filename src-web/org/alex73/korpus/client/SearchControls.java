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

import java.util.HashMap;
import java.util.Map;

import org.alex73.korpus.client.controls.BaseControlsWrapper;
import org.alex73.korpus.client.controls.StyleGenrePopup;
import org.alex73.korpus.shared.SearchParams;
import org.alex73.korpus.shared.SearchParams.WordsOrder;
import org.alex73.korpus.shared.StyleGenres;

import com.google.gwt.event.dom.client.ChangeEvent;
import com.google.gwt.event.dom.client.ChangeHandler;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.logical.shared.CloseEvent;
import com.google.gwt.event.logical.shared.CloseHandler;
import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Anchor;
import com.google.gwt.user.client.ui.MultiWordSuggestOracle;
import com.google.gwt.user.client.ui.PopupPanel;
import com.google.gwt.user.client.ui.PopupPanel.PositionCallback;
import com.google.gwt.user.client.ui.SimpleRadioButton;
import com.google.gwt.user.client.ui.SuggestBox;
import com.google.gwt.user.client.ui.TextBox;

/**
 * Controls for filter texts, like genre, years, etc.
 */
public class SearchControls extends BaseControlsWrapper {
    public TextControl text = new TextControl();

    public SimpleRadioButton orderPreset, orderAny;

    public SearchControls(String parameters, SearchServiceAsync searchService) {

        final MultiWordSuggestOracle ora = new MultiWordSuggestOracle();
        text.author = SuggestBox.wrap(ora, DOM.getElementById("text.author"));
        text.stylegenre = Anchor.wrap(DOM.getElementById("text.stylegenre"));
        text.yearWrittenFrom =  TextBox.wrap(DOM.getElementById("text.yearWrittenFrom"));
        text.yearWrittenTo =  TextBox.wrap(DOM.getElementById("text.yearWrittenTo"));
        text.yearPublishedFrom = TextBox.wrap(DOM.getElementById("text.yearPublishedFrom"));
        text.yearPublishedTo = TextBox.wrap(DOM.getElementById("text.yearPublishedTo"));

        orderPreset = SimpleRadioButton.wrap(DOM.getElementById("orderPreset"));
        orderAny = SimpleRadioButton.wrap(DOM.getElementById("orderAny"));

        text.stylegenre.addClickHandler(new ClickHandler() {
            @Override
            public void onClick(ClickEvent event) {
            //final StyleGenrePopup
                final StyleGenrePopup popup = new StyleGenrePopup(text.styleGenres);
                popup.addCloseHandler(new CloseHandler<PopupPanel>() {
                    @Override
                    public void onClose(CloseEvent<PopupPanel> event) {
                        text.styleGenres.clear();
                        text.styleGenres.addAll(popup.getSelected());
                        text.stylegenre.setText(StyleGenres.getSelectedName(text.styleGenres));
                    }
                });
                popup.showRelativeTo(text.stylegenre);
                popup.setPopupPositionAndShow(new PositionCallback() {
                    public void setPosition(int offsetWidth, int offsetHeight) {
                        popup.setPopupPosition(0, text.stylegenre.getAbsoluteTop()
                                + text.stylegenre.getOffsetHeight());
                    }
                });
            }
        });

        validator = new ChangeHandler() {
            @Override
            public void onChange(ChangeEvent event) {
                for (WordControl wc : words) {
                    if (!isValidWord(wc.word)) {
                        errorMessage.setText("Няправільнае слова: " + wc.word.getValue());
                        return;
                    }
                }
                errorMessage.setText("");
            }
        };

        final Map<String, String> ps = parseParameters(parameters);
        try {
            searchService.getInitialData(new AsyncCallback<SearchService.InitialData>() {
                @Override
                public void onSuccess(SearchService.InitialData result) {
                    ora.addAll(result.authors);

                    importParameters(ps);
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

    public SearchParams createRequest() {
        SearchParams req = new SearchParams();

        req.text.author = text.author.getValue();
        req.text.stylegenres = text.styleGenres;
        req.text.yearPublishedFrom = txt2int(text.yearPublishedFrom);
        req.text.yearPublishedTo = txt2int(text.yearPublishedTo);
        req.text.yearWrittenFrom = txt2int(text.yearWrittenFrom);
        req.text.yearWrittenTo = txt2int(text.yearWrittenTo);

        for (WordControl wc : words) {
            SearchParams.Word w = new SearchParams.Word();
            w.word = wc.word.getValue();
            w.allForms = Boolean.TRUE.equals(wc.allForms.getValue());
            w.grammar = wc.wordGrammar;

            if ((w.word != null && !w.word.trim().isEmpty()) || w.grammar != null) {
                req.words.add(w);
            }
        }

        req.wordsOrder = Boolean.TRUE.equals(orderPreset.getValue()) ? WordsOrder.PRESET : WordsOrder.ANY;

        return req;
    }

    public String exportParameters() {
        StringBuilder out = new StringBuilder();

        // text
        outTextBox(out, "yearPublishedFrom", text.yearPublishedFrom);
        outTextBox(out, "yearPublishedFrom", text.yearPublishedTo);
        outText(out, "stylegenres", StyleGenres.produceCode(text.styleGenres));

        // words
        for (int i = 0; i < words.size(); i++) {
            WordControl w = words.get(i);
            outTextBox(out, "w" + i + ".word", w.word);
            outCheckBox(out, "w" + i + ".allForms", w.allForms);
            outText(out, "w" + i + ".grammar", w.wordGrammar);
        }

        outRadioButton(out, "orderPreset", orderPreset);
        outRadioButton(out, "orderAny", orderAny);

        return out.substring(1).toString();
    }
    
    Map<String, String> parseParameters(String params) {
        Map<String, String> ps = new HashMap<String, String>();
        for (String p : params.split("&")) {
            int pos = p.indexOf('=');
            String key, value;
            if (pos > 0) {
                key = p.substring(0, pos);
                value = p.substring(pos + 1);
            } else {
                key = p;
                value = null;
            }
            ps.put(key, value);
        }
        return ps;
    }

    public void importParameters(Map<String, String> ps) {
        // text
        inSuggestBox(ps, "author", text.author);
        inTextBox(ps, "yearPublishedFrom", text.yearPublishedFrom);
        inTextBox(ps, "yearPublishedTo", text.yearPublishedTo);
        inTextBox(ps, "yearWrittenFrom", text.yearWrittenFrom);
        inTextBox(ps, "yearWrittenTo", text.yearWrittenTo);
        StyleGenres.restoreCode(ps.get("stylegenres"), text.styleGenres);
        text.stylegenre.setText(StyleGenres.getSelectedName(text.styleGenres));

        // words
        for (int i = 0;; i++) {
            boolean found = false;
            String prefix = "w" + i + ".";
            for (String key : ps.keySet()) {
                if (key.startsWith(prefix)) {
                    found = true;
                    break;
                }
            }
            if (!found) {
                break;
            }
            addWord();
            WordControl w = words.get(i);
            inTextBox(ps, prefix + "word", w.word);
            inCheckBox(ps, prefix + "allForms", w.allForms);
            inListBox(ps, prefix + "grammar", w.wordType);
            w.wordGrammar = inText(ps, prefix + "grammar");

            if (w.wordGrammar == null) {
                w.wordTypeDetails.setVisible(false);
            } else {
                w.wordTypeDetails.setVisible(true);
            }
        }

        inRadioButton(ps, "orderPreset", orderPreset);
        inRadioButton(ps, "orderAny", orderAny);
    }
}
