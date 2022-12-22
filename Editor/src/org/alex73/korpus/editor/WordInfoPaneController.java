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

package org.alex73.korpus.editor;

import java.awt.GridBagConstraints;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import javax.swing.ButtonGroup;
import javax.swing.JLabel;
import javax.swing.JRadioButton;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import org.alex73.corpus.paradigm.Paradigm;
import org.alex73.korpus.editor.ui.WordInfoPane;
import org.alex73.korpus.languages.belarusian.BelarusianTags;
import org.alex73.korpus.text.structure.files.WordItem;

public class WordInfoPaneController {
    public static void init() {
        UI.wordInfoPane.btnSave.addActionListener(btnSave);
        UI.wordInfoPane.btnReset.addActionListener(btnReset);
    }

    static Map<JRadioButton, Paradigm> paradigmsOnLemmas = new HashMap<>();

    static WordItem currentWord;
    static boolean changed;

    public static void show(WordItem word) {
        WordInfoPane p = UI.wordInfoPane;

        p.pLemma.removeAll();
        p.pGrammar.removeAll();
        paradigmsOnLemmas.clear();
        if (word == null) {
            UI.dockWordInfo.getDockKey().setName("");
            p.txtNormal.setText("");
            currentWord = null;
        } else {
            UI.dockWordInfo.getDockKey().setName("Звесткі пра слова: "+word.lightNormalized);
            p.txtNormal.setText(word.manualNormalized != null ? word.manualNormalized : "");
            currentWord = new WordItem();
            currentWord.lightNormalized = word.lightNormalized;
            currentWord.manualNormalized = word.manualNormalized;
            currentWord.manualLemma = word.manualLemma;
            currentWord.manualTag = word.manualTag;
            currentWord.type = word.type;
            showLemmasAndTags();
            p.txtNormal.getDocument().addDocumentListener(docListener);

            GrammarPaneController2.show(word.manualNormalized != null ? word.manualNormalized : word.lightNormalized);
        }
        changed = false;
        setButtonsEnabled();
        p.revalidate();
        p.repaint();
    }

    static void showLemmasAndTags() {
        WordInfoPane p = UI.wordInfoPane;

        p.pLemma.removeAll();
        p.pGrammar.removeAll();

        if (currentWord.type != null) {
            p.pLemma.add(new JLabel(currentWord.type.name()));
            p.revalidate();
            return;
        }

        WordItem nonManual = currentWord.clone();
        nonManual.manualLemma = null;
        nonManual.manualTag = null;

        MainController.gr.filler.fill(nonManual);
        MainController.gr.filler.fill(currentWord);

        // show lemmas
        if (nonManual.lemmas != null) {
            ButtonGroup rbGroupLemma = new ButtonGroup();
            Set<String> manualSet = listToSet(currentWord.lemmas);
            for (String c : nonManual.lemmas.split(";")) {
                if (c.isEmpty()) {
                    continue;
                }
                JRadioButton rb = new JRadioButton(c);
                rb.setName(c);
                rb.setFont(p.pLemma.getFont());
                if (c.equals(currentWord.manualLemma)) {
                    rb.setSelected(true);
                }
                rb.setEnabled(manualSet.contains(c));
                rbGroupLemma.add(rb);
                p.pLemma.add(rb);
                rb.addActionListener(lemmaClick);
            }
        }

        // show tags
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1;
        if (nonManual.tags != null) {
            ButtonGroup rbGroupGrammar = new ButtonGroup();
            Set<String> manualSet = listToSet(currentWord.tags);
            for (String c : nonManual.tags.split(";")) {
                if (c.isEmpty()) {
                    continue;
                }
                JRadioButton rb = new JRadioButton("<html>" + createTagDesc(c) + "</html>");
                rb.setToolTipText(rb.getText());
                rb.setName(c);
                rb.setFont(p.pGrammar.getFont());
                if (c.equals(currentWord.manualTag)) {
                    rb.setSelected(true);
                }
                rb.setEnabled(manualSet.contains(c));
                rbGroupGrammar.add(rb);
                gbc.gridy = p.pGrammar.getComponentCount();
                p.pGrammar.add(rb, gbc);
                rb.addActionListener(grammarClick);
            }
        }

        UI.wordInfoPane.revalidate();
        UI.wordInfoPane.repaint();
    }

    private static Set<String> listToSet(String str) {
        Set<String> set = new TreeSet<>();
        for (String s : str.split(";")) {
            set.add(s);
        }
        return set;
    }

    private static String createTagDesc(String tag) {
        String outText;
        try {
            outText = tag + ": ";
            for (String d : BelarusianTags.getInstance().describe(tag, null)) {
                outText += d + ", ";
            }
            outText = outText.substring(0, outText.length() - 2);
        } catch (Exception ex) {
            // unknown code
            outText = tag + ":няправільны код";
        }
        return outText;
    }

    static void setButtonsEnabled() {
        WordInfoPane p = UI.wordInfoPane;
        if (currentWord != null) {
            p.btnSave.setEnabled(changed);
            p.btnReset.setEnabled(currentWord.manualLemma != null || currentWord.manualTag != null
                    || !p.txtNormal.getText().trim().isEmpty());
        } else {
            p.btnSave.setEnabled(false);
            p.btnReset.setEnabled(false);
        }
    }

    static ActionListener lemmaClick = new ActionListener() {
        public void actionPerformed(ActionEvent e) {
            changed = true;
            JRadioButton button = (JRadioButton) e.getSource();
            currentWord.manualLemma = button.getName();
            showLemmasAndTags();
            setButtonsEnabled();
        }
    };

    static ActionListener grammarClick = new ActionListener() {
        public void actionPerformed(ActionEvent e) {
            changed = true;
            JRadioButton button = (JRadioButton) e.getSource();
            currentWord.manualTag = button.getName();
            showLemmasAndTags();
            setButtonsEnabled();
        }
    };

    static ActionListener btnSave = new ActionListener() {
        public void actionPerformed(ActionEvent e) {
            MainController.setWordInfo(currentWord.manualLemma, currentWord.manualTag, UI.wordInfoPane.txtNormal.getText());
        }
    };
    static ActionListener btnReset = new ActionListener() {
        public void actionPerformed(ActionEvent e) {
            currentWord.manualLemma = null;
            currentWord.manualTag = null;
            UI.wordInfoPane.txtNormal.setText("");
            changed = true;
            showLemmasAndTags();
            setButtonsEnabled();
        }
    };
    static DocumentListener docListener = new DocumentListener() {
        public void changedUpdate(DocumentEvent e) {
            changed = true;
            setButtonsEnabled();
        }

        public void insertUpdate(DocumentEvent e) {
            changed = true;
            setButtonsEnabled();
        }

        public void removeUpdate(DocumentEvent e) {
            changed = true;
            setButtonsEnabled();
        }
    };
}
