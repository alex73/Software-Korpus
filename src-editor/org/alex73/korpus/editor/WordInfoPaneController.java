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

import javax.swing.ButtonGroup;
import javax.swing.JPanel;
import javax.swing.JRadioButton;

import org.alex73.korpus.base.BelarusianTags;
import org.alex73.korpus.editor.parser.Splitter;
import org.alex73.korpus.editor.ui.WordInfoPane;

import alex73.corpus.paradigm.W;

public class WordInfoPaneController {
    public static void init() {
        UI.wordInfoPane.btnSave.addActionListener(btnSave);
    }

    public static void show(W word) {
        WordInfoPane p = UI.wordInfoPane;

        p.pLemma.removeAll();
        p.pGrammar.removeAll();
        if (word == null) {
            p.txtWord.setText("");
        } else {
            p.txtWord.setText(word.getValue());

            ButtonGroup rbGroupLemma = new ButtonGroup();
            if (word.getLemma() != null) {
                for (String t : word.getLemma().split("_")) {
                    JRadioButton rb = new JRadioButton(t);
                    rb.setToolTipText(t);
                    rbGroupLemma.add(rb);
                    p.pLemma.add(rb);
                    rb.addActionListener(lemmaClick);
                }
                if (p.pLemma.getComponentCount() == 1) {
                    ((JRadioButton) p.pLemma.getComponent(0)).setSelected(true);
                }
            }

            fillLemmas(word);
        }
        setSaveEnabled();
        p.revalidate();
        p.repaint();
    }

    static void fillLemmas(W word) {
        WordInfoPane p = UI.wordInfoPane;
        p.pGrammar.removeAll();
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.anchor = GridBagConstraints.WEST;
        if (word.getCat() != null) {
            ButtonGroup rbGroupGrammar = new ButtonGroup();
            for (String c : word.getCat().split("_")) {
                String outText;
                try {
                    outText = c + ":";
                    for (String d : BelarusianTags.getInstance().describe(c)) {
                        outText += d + ",";
                    }
                    outText = outText.substring(0, outText.length() - 1);
                } catch (Exception ex) {
                    // unknown code
                    outText = c + ":няправільны код";
                }
                JRadioButton rb = new JRadioButton(outText);
                rb.setToolTipText(c);
                rbGroupGrammar.add(rb);
                gbc.gridy = p.pGrammar.getComponentCount();
                p.pGrammar.add(rb, gbc);
                rb.addActionListener(grammarClick);
            }
            if (p.pGrammar.getComponentCount() == 1) {
                ((JRadioButton) p.pGrammar.getComponent(0)).setSelected(true);
            }
        }
    }

    static void setSaveEnabled() {
        WordInfoPane p = UI.wordInfoPane;
        p.btnSave.setEnabled(getSelected(p.pLemma) != null && getSelected(p.pGrammar) != null);
    }

    static String getSelected(JPanel panel) {
        for (int i = 0; i < panel.getComponentCount(); i++) {
            JRadioButton rb = (JRadioButton) panel.getComponent(i);
            if (rb.isSelected()) {
                return rb.getToolTipText();
            }
        }
        return null;
    }

    static ActionListener lemmaClick = new ActionListener() {
        public void actionPerformed(ActionEvent e) {
            W w = new W();
            w.setValue(UI.wordInfoPane.txtWord.getText());
            String lemma = ((JRadioButton) e.getSource()).getText();
            Splitter.fillWordInfo(w, lemma);
            fillLemmas(w);

            setSaveEnabled();
            UI.wordInfoPane.revalidate();
            UI.wordInfoPane.repaint();
        }
    };
    static ActionListener grammarClick = new ActionListener() {
        public void actionPerformed(ActionEvent e) {
            setSaveEnabled();
        }
    };
    static ActionListener btnSave = new ActionListener() {
        public void actionPerformed(ActionEvent e) {
            WordInfoPane p = UI.wordInfoPane;
            W w = new W();
            w.setValue(p.txtWord.getText());
            w.setLemma(getSelected(p.pLemma));
            w.setCat(getSelected(p.pGrammar));
            w.setManual(true);
            MainController.setWordInfo(w);
        }
    };
}
