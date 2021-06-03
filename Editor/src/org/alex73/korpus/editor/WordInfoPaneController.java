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
import java.util.List;
import java.util.Map;

import javax.swing.ButtonGroup;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;

import org.alex73.corpus.paradigm.Paradigm;
import org.alex73.korpus.belarusian.BelarusianTags;
import org.alex73.korpus.editor.ui.WordInfoPane;
import org.alex73.korpus.text.structure.corpus.Word;
import org.alex73.korpus.text.structure.files.WordItem;

public class WordInfoPaneController {
    public static void init() {
        UI.wordInfoPane.btnSave.addActionListener(btnSave);
    }

    static Map<JRadioButton, Paradigm> paradigmsOnLemmas = new HashMap<>();

    public static void show(WordItem word) {
        WordInfoPane p = UI.wordInfoPane;

        p.pLemma.removeAll();
        p.pGrammar.removeAll();
        paradigmsOnLemmas.clear();
        if (word == null) {
            p.txtWord.setText("");
        } else {
            p.txtWord.setText(word.lightNormalized);

            if (word.type != null) {
                p.pLemma.add(new JLabel(word.type.name()));
                p.revalidate();
            } else {
                List<Paradigm> pa2 = MainController.gr.filler.getParadigms(word.lightNormalized);
                ButtonGroup rbGroupLemma = new ButtonGroup();
                for (Paradigm pa : pa2) {
                    JRadioButton rb = new JRadioButton(pa.getLemma());
                    rb.setName(pa.getLemma());
                    rb.setFont(p.pLemma.getFont());
                    rbGroupLemma.add(rb);
                    p.pLemma.add(rb);
                    rb.addActionListener(lemmaClick);
                    paradigmsOnLemmas.put(rb, pa);
                }
                if (p.pLemma.getComponentCount() == 1) {
                    ((JRadioButton) p.pLemma.getComponent(0)).setSelected(true);
                    lemmaClick.actionPerformed(new ActionEvent(p.pLemma.getComponent(0), 0, null));
                }
                fillLemmas(word);
            }
        }
        setSaveEnabled();
        p.revalidate();
        p.repaint();
    }

    static void fillLemmas(WordItem word) {
        WordInfoPane p = UI.wordInfoPane;
        p.pGrammar.removeAll();
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.anchor = GridBagConstraints.WEST;
        if (word.tags != null) {
            ButtonGroup rbGroupGrammar = new ButtonGroup();
            for (String c : word.tags.split(";")) {
                if (c.isEmpty()) {
                    continue;
                }
                String outText;
                try {
                    outText = c + ": ";
                    for (String d : BelarusianTags.getInstance().describe(c, null)) {
                        outText += d + ", ";
                    }
                    outText = outText.substring(0, outText.length() - 2);
                } catch (Exception ex) {
                    // unknown code
                    outText = c + ":няправільны код";
                }
                JRadioButton rb = new JRadioButton("<html>" + outText + "</html>");
                rb.setName(c);
                rb.setFont(p.pGrammar.getFont());
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
            if (!(panel.getComponent(i) instanceof JRadioButton)) {
                return null;
            }
            JRadioButton rb = (JRadioButton) panel.getComponent(i);
            if (rb.isSelected()) {
                return rb.getName();
            }
        }
        return null;
    }

    static ActionListener lemmaClick = new ActionListener() {
        public void actionPerformed(ActionEvent e) {
            WordItem w = new WordItem();
            w.lightNormalized = UI.wordInfoPane.txtWord.getText();
            Paradigm p = paradigmsOnLemmas.get(e.getSource());
            MainController.gr.filler.fillFromParadigm(w, p);
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
            MainController.setWordInfo(getSelected(p.pLemma), getSelected(p.pGrammar));
            UI.editor.repaint();
        }
    };
}
