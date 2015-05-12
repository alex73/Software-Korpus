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

package org.alex73.korpus.client.controls;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.alex73.korpus.base.DBTagsGroups;

import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.Node;
import com.google.gwt.event.dom.client.ChangeEvent;
import com.google.gwt.event.dom.client.ChangeHandler;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.dom.client.KeyCodes;
import com.google.gwt.event.dom.client.KeyDownEvent;
import com.google.gwt.event.dom.client.KeyDownHandler;
import com.google.gwt.event.dom.client.KeyUpEvent;
import com.google.gwt.event.dom.client.KeyUpHandler;
import com.google.gwt.event.logical.shared.CloseEvent;
import com.google.gwt.event.logical.shared.CloseHandler;
import com.google.gwt.regexp.shared.RegExp;
import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.ui.Anchor;
import com.google.gwt.user.client.ui.HTMLPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.ListBox;
import com.google.gwt.user.client.ui.PopupPanel;
import com.google.gwt.user.client.ui.PopupPanel.PositionCallback;
import com.google.gwt.user.client.ui.SimpleCheckBox;
import com.google.gwt.user.client.ui.SimpleRadioButton;
import com.google.gwt.user.client.ui.SuggestBox;
import com.google.gwt.user.client.ui.TextBox;

public abstract class BaseControlsWrapper {

    private ClickHandler processHandler;

    public List<WordControl> words = new ArrayList<WordControl>();

    public Label errorMessage;
    protected Element templateWord;
    protected Element wordsBlock;
    protected List<HTMLPanel> wordPanels = new ArrayList<HTMLPanel>();

    public ChangeHandler validator;

    public static class TextControl {
        public SuggestBox author;
        public Anchor stylegenre;
        public final List<String> styleGenres = new ArrayList<>();
        public TextBox yearWrittenFrom, yearWrittenTo, yearPublishedFrom, yearPublishedTo;
        public SuggestBox volume;
        public TextBox wordsBefore, wordsAfter;
    }

    public static class WordControl {
        public TextBox word;
        public SimpleCheckBox allForms;
        public ListBox wordType;
        public Anchor wordTypeDetails;
        public String wordGrammar;
        public Anchor wordClose;
    }

    public BaseControlsWrapper() {
        errorMessage = Label.wrap(DOM.getElementById("errorMessage"));
        templateWord = DOM.getElementById("templateWord");
        wordsBlock = DOM.getElementById("wordsBlock");
    }

    public void setProcessHandler(ClickHandler processHandler) {
        this.processHandler = processHandler;
    }

    public void addWord() {
        final Node newWord = templateWord.cloneNode(true);
        wordsBlock.appendChild(newWord);
        Element newWordElement = Element.as(newWord);
        final HTMLPanel newWordPanel = HTMLPanel.wrap(newWordElement);
        int wordIndex = words.size();
        templateToWord(newWordElement, wordIndex);
        newWordPanel.setVisible(true);
        wordPanels.add(newWordPanel);

        final WordControl controls = new WordControl();
        words.add(controls);
        controls.word = new TextBox();
        controls.allForms = new SimpleCheckBox();
        controls.allForms.setValue(false);
        controls.wordType = new ListBox();
        controls.wordType.addItem("-----", "");
        for (DBTagsGroups.KeyValue en : DBTagsGroups.getWordTypes()) {
            controls.wordType.addItem(en.value, en.key);
        }

        controls.wordTypeDetails = new Anchor("падрабязнасці");
        controls.wordTypeDetails.setVisible(false);

        controls.wordType.addChangeHandler(new ChangeHandler() {
            @Override
            public void onChange(ChangeEvent event) {
                int vi = controls.wordType.getSelectedIndex();
                String c = controls.wordType.getValue(vi);
                if (c.isEmpty()) {
                    controls.wordGrammar = null;
                    controls.wordTypeDetails.setVisible(false);
                } else {
                    controls.wordGrammar = c + ".*";
                    DBTagsGroups tagsList = DBTagsGroups.getTagGroupsByWordType().get(c.charAt(0));
                    controls.wordTypeDetails.setVisible(!tagsList.groups.isEmpty());
                }
            }
        });

        controls.wordTypeDetails.addClickHandler(new ClickHandler() {
            @Override
            public void onClick(ClickEvent event) {
                int vi = controls.wordType.getSelectedIndex();
                String v = controls.wordType.getValue(vi);
                if (v.length() > 0) {
                    DBTagsGroups tagsList = DBTagsGroups.getTagGroupsByWordType().get(v.charAt(0));
                    final WordGrammarPopup popup = new WordGrammarPopup(tagsList);
                    if (!".*".equals(controls.wordGrammar.substring(1))) {
                        popup.setSelected(controls.wordGrammar.substring(1));
                    }
                    popup.addCloseHandler(new CloseHandler<PopupPanel>() {
                        @Override
                        public void onClose(CloseEvent<PopupPanel> event) {
                            String c = controls.wordType.getValue(controls.wordType.getSelectedIndex());
                            controls.wordGrammar = c + popup.getSelected();
                        }
                    });
                    popup.showRelativeTo(controls.wordTypeDetails);
                    popup.setPopupPositionAndShow(new PositionCallback() {
                        public void setPosition(int offsetWidth, int offsetHeight) {
                            popup.setPopupPosition(0, controls.wordTypeDetails.getAbsoluteTop()
                                    + controls.wordTypeDetails.getOffsetHeight());
                        }
                    });
                }
            }
        });
        controls.wordClose = new Anchor("[X]");
        controls.wordClose.setTitle("прыбраць");
        controls.wordClose.addClickHandler(new ClickHandler() {
            @Override
            public void onClick(ClickEvent event) {
                words.remove(controls);
                wordPanels.remove(newWordPanel);
                wordsBlock.removeChild(newWord);
            }
        });
        newWordPanel.addAndReplaceElement(controls.word, "w" + wordIndex + ".Word");
        newWordPanel.addAndReplaceElement(controls.allForms, "w" + wordIndex + ".cbAllForms");
        newWordPanel.addAndReplaceElement(controls.wordType, "w" + wordIndex + ".lbTypes");
        newWordPanel.addAndReplaceElement(controls.wordTypeDetails, "w" + wordIndex + ".lnDetails");
        newWordPanel.addAndReplaceElement(controls.wordClose, "w" + wordIndex + ".lnClose");

        controls.word.addKeyDownHandler(new KeyDownHandler() {
            @Override
            public void onKeyDown(KeyDownEvent event) {
                if (event.getNativeKeyCode() == KeyCodes.KEY_ENTER) {
                    processHandler.onClick(null);
                }
            }
        });
        controls.word.addKeyUpHandler(new KeyUpHandler() {
            @Override
            public void onKeyUp(KeyUpEvent event) {
                validator.onChange(null);
            }
        });
    }

    RegExp RE_WORD = RegExp
            .compile("^[йцукенгшўзх'фывапролджэячсмітьбюёЙЦУКЕНГШЎЗХФЫВАПРОЛДЖЭЯЧСМІТЬБЮЁ\\?\\*]+$");

    protected boolean isValidWord(TextBox txt) {
        if (txt.getValue().trim().isEmpty()) {
            return true;
        }
        return RE_WORD.exec(txt.getValue()) != null;
    }

    protected void templateToWord(Element el, int wordIndex) {
        String c = el.getClassName();
        if (c.startsWith("templateReplace")) {
            String id = "w" + wordIndex + "." + c.substring("templateReplace".length());
            el.setId(id);
        }
        Element next = el.getNextSiblingElement();
        if (next != null) {
            templateToWord(next, wordIndex);
        }
        Element child = el.getFirstChildElement();
        if (child != null) {
            templateToWord(child, wordIndex);
        }
    }

    protected void outText(StringBuilder out, String name, String txt) {
        if (txt != null && !txt.isEmpty()) {
            out.append('&').append(name).append('=').append(txt);
        }
    }

    protected String inText(Map<String, String> params, String name) {
        if (params.containsKey(name)) {
            return params.get(name);
        } else {
            return null;
        }
    }

    protected void inListBox(Map<String, String> params, String name, ListBox cb) {
        String v = params.get(name);
        if (v == null || v.isEmpty()) {
            return;
        }
        for (int i = 0; i < cb.getItemCount(); i++) {
            if (v.equals(cb.getValue(i))) {
                cb.setSelectedIndex(i);
                return;
            }
        }
        for (int i = 0; i < cb.getItemCount(); i++) {
            if (v.substring(0, 1).equals(cb.getValue(i))) {
                cb.setSelectedIndex(i);
                return;
            }
        }
    }

    protected void inCheckBox(Map<String, String> params, String name, SimpleCheckBox cb) {
        if (params.containsKey(name)) {
            cb.setValue(true);
        }
    }

    protected void outCheckBox(StringBuilder out, String name, SimpleCheckBox cb) {
        if (Boolean.TRUE.equals(cb.getValue())) {
            out.append('&').append(name);
        }
    }

    protected void outTextBox(StringBuilder out, String name, TextBox txt) {
        if (txt != null && !txt.getValue().trim().isEmpty()) {
            out.append('&').append(name).append('=').append(txt.getValue().trim());
        }
    }

    protected void inSuggestBox(Map<String, String> params, String name, SuggestBox txt) {
        if (txt != null) {
            if (params.containsKey(name)) {
                txt.setValue(params.get(name));
            }
        }
    }

    protected void inTextBox(Map<String, String> params, String name, TextBox txt) {
        if (txt != null) {
            if (params.containsKey(name)) {
                txt.setValue(params.get(name));
            }
        }
    }

    protected void outRadioButton(StringBuilder out, String name, SimpleRadioButton radio) {
        if (radio != null) {
            if (Boolean.TRUE.equals(radio.getValue())) {
                out.append('&').append(name);
            }
        }
    }

    protected void inRadioButton(Map<String, String> params, String name, SimpleRadioButton radio) {
        if (params.containsKey(name)) {
            radio.setValue(true);
        }
    }

    protected Integer txt2int(TextBox txt) {
        try {
            return Integer.parseInt(txt.getValue());
        } catch (Exception ex) {
            return null;
        }
    }
}
