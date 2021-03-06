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

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.List;
import java.util.concurrent.CancellationException;

import javax.swing.SwingWorker;
import javax.swing.event.CaretEvent;
import javax.swing.event.CaretListener;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.Document;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyledDocument;

import org.alex73.corpus.paradigm.Paradigm;
import org.alex73.korpus.belarusian.BelarusianTags;
import org.alex73.korpus.belarusian.TagLetter;
import org.alex73.korpus.editor.grammar.GrammarConstructor;
import org.alex73.korpus.text.structure.files.WordItem;

public class GrammarPaneController {
    static WordItem currentWord;
    static Integer intoParadigmId;
    static boolean notRealUpdate = false;

    public static synchronized void show(WordItem word, Integer pdgId) {
        currentWord = word;
        intoParadigmId = pdgId;
        notRealUpdate = true;
        try {
            if (word == null) {
                UI.grammarPane.txtWord.setText("");
            } else if (word.normalized != null) {
                UI.grammarPane.txtWord.setText(word.normalized.replace('\u0301', '+'));
            } else {
                UI.grammarPane.txtWord.setText(word.lightNormalized.replace('\u0301', '+'));
            }
            UI.grammarPane.txtLooksLike.setText("");
            updateInfo();
        } finally {
            notRealUpdate = false;
        }
        updateXML();
    }

    public static void init() {
        UI.grammarPane.labelError.setVisible(false);
        UI.grammarPane.txtWord.getDocument().addDocumentListener(new DocumentListener() {
            public void changedUpdate(DocumentEvent e) {
                updateXML();
            }

            public void insertUpdate(DocumentEvent e) {
                updateXML();
            }

            public void removeUpdate(DocumentEvent e) {
                updateXML();
            }
        });
        UI.grammarPane.txtLooksLike.getDocument().addDocumentListener(new DocumentListener() {
            public void changedUpdate(DocumentEvent e) {
                updateXML();
            }

            public void insertUpdate(DocumentEvent e) {
                updateXML();
            }

            public void removeUpdate(DocumentEvent e) {
                updateXML();
            }
        });
        UI.grammarPane.cbPreserveCase.addItemListener(e -> updateXML());
        UI.grammarPane.cbLikeLemma.addItemListener(e -> updateXML());
        UI.grammarPane.txtGrammar.addCaretListener(new CaretListener() {
            public void caretUpdate(CaretEvent e) {
                updateInfo();
            }
        });
        UI.grammarPane.txtGrammar.getDocument().addDocumentListener(new DocumentListener() {
            public void changedUpdate(DocumentEvent e) {
                updateXML();
                updateInfo();
            }

            public void insertUpdate(DocumentEvent e) {
                updateXML();
                updateInfo();
            }

            public void removeUpdate(DocumentEvent e) {
                updateXML();
                updateInfo();
            }
        });
        UI.grammarPane.txtTheme.getDocument().addDocumentListener(new DocumentListener() {
            public void changedUpdate(DocumentEvent e) {
                updateXML();
            }

            public void insertUpdate(DocumentEvent e) {
                updateXML();
            }

            public void removeUpdate(DocumentEvent e) {
                updateXML();
            }
        });
        UI.grammarPane.btnSave.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                UI.grammarPane.labelError.setVisible(false);
                try {
                    Paradigm p = parseXML();
                    MainController.gr.addDocLevelParadigm(p);
                    MainController.saveGrammar();
                    MainController.gr.filler.fillFromParadigm(currentWord, p);
                    UI.editor.repaint();
                    UI.grammarPane.outXML.setText("Захавана");
                } catch (Exception ex) {
                    UI.grammarPane.labelError.setToolTipText(ex.getClass().getSimpleName() + ": " + ex.getMessage());
                    UI.grammarPane.labelError.setVisible(true);
                }
            }
        });
        UI.grammarPane.btnChooseLike.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                Paradigm p;
                try {
                    p = parseXML();
                } catch (Exception ex) {
                    return;
                }
                if (p.getTag() == null) {
                    return;
                }
                String result = ThemesDialogController.getInstance().show(p.getTag().toUpperCase(),
                        UI.grammarPane.txtTheme.getText());
                if (result != null) {
                    UI.grammarPane.txtTheme.setText(result);
                }
            }
        });
    }

    static Paradigm parseXML() throws Exception {
        String xml = UI.grammarPane.outXML.getText();
        xml = xml.substring(xml.indexOf('<'));
        return GrammarConstructor.parseAndValidate(xml);
    }

    static UpdaterInfo updaterInfo;

    static synchronized void updateInfo() {
        if (updaterInfo != null) {
            updaterInfo.cancel(true);
        }
        updaterInfo = new UpdaterInfo();
        updaterInfo.execute();
    }

    static UpdaterXML updaterXML;

    static synchronized void updateXML() {
        if (notRealUpdate) {
            return;
        }
        if (updaterXML != null) {
            updaterXML.cancel(true);
        }
        updaterXML = new UpdaterXML();
        updaterXML.execute();
    }

    static class UpdaterInfo extends SwingWorker<String, Void> {
        String txt;
        int posCaret;

        public UpdaterInfo() {
            txt = UI.grammarPane.txtGrammar.getText().toUpperCase();
            posCaret = Math.min(UI.grammarPane.txtGrammar.getCaretPosition(), txt.length());
        }

        @Override
        protected String doInBackground() throws Exception {
            try {
                String outText = "";
                List<String> descr = BelarusianTags.getInstance().describe(txt, null);
                outText += "Палі: ";
                for (String d : descr) {
                    outText += d + ",";
                }
                outText = outText.substring(0, outText.length() - 1);
                outText += "\n";

                TagLetter next = BelarusianTags.getInstance().getNextAfter(txt.substring(0, posCaret));
                if (next == null) {
                    outText += "Наступны код: няма";
                } else {
                    String nextNames = next.getNextGroupNames();
                    outText += "Наступны код: " + (nextNames != null ? nextNames : "няма") + "\n";
                    for (TagLetter.OneLetterInfo li : next.letters) {
                        outText += li.letter + ": " + li.description + ", ";
                    }
                    outText = outText.substring(0, outText.length() - 2);
                }
                return outText;
            } catch (Exception ex) {
                return ex.getMessage();
            }
        }

        @Override
        protected void done() {
            try {
                UI.grammarPane.outInfo.setText(get());
                UI.grammarPane.outInfo.setCaretPosition(0);
                applyFont();
            } catch (CancellationException ex) {
            } catch (Throwable ex) {
                ex.printStackTrace();
                UI.grammarPane.outInfo.setText("Памылка: " + ex.getMessage());
            }
        }
    }
    
    static void applyFont() {
        Document doc = UI.grammarPane.outInfo.getDocument();
        if (doc instanceof StyledDocument) {
            SimpleAttributeSet attrs = new SimpleAttributeSet();
            StyleConstants.setFontSize(attrs, UI.grammarPane.outInfo.getFont().getSize());
            ((StyledDocument) doc).setCharacterAttributes(0, doc.getLength() + 1, attrs, false);
        }
    }

    static class UpdaterXML extends SwingWorker<String, Void> {
        String word, grammar, theme, looksLike;
        boolean preserveCase, likeLemma;

        public UpdaterXML() {
            word = UI.grammarPane.txtWord.getText();
            grammar = UI.grammarPane.txtGrammar.getText().toUpperCase();
            theme = UI.grammarPane.txtTheme.getText();
            looksLike = UI.grammarPane.txtLooksLike.getText().trim();
            preserveCase = UI.grammarPane.cbPreserveCase.isSelected();
            likeLemma = UI.grammarPane.cbLikeLemma.isSelected();
            if (theme.trim().length() == 0) {
                theme = null;
            }
        }

        @Override
        protected String doInBackground() throws Exception {
            GrammarConstructor grConstr = new GrammarConstructor(MainController.gr);

            String out;
            StringBuilder like = new StringBuilder();
            boolean checkForms = intoParadigmId == null && looksLike.isEmpty();
            if (likeLemma) {
                checkForms = false;
            }
           Paradigm p = null;//grConstr.getLooksLike(word, looksLike, preserveCase, checkForms, grammar, like, intoParadigmId);
            //if (intoParadigmId != null) {
                /*Paradigm pInto = grConstr.ed.filler.gr.getAllParadigms().stream()
                        .filter(pa -> pa.getPdgId() == intoParadigmId.intValue()).findFirst().get();

                pInto = GrammarDBSaver.cloneParadigm(pInto);
                pInto.getVariant().get(0).getForm().clear();
                pInto.getVariant().get(0).getForm().addAll(p.getVariant().get(0).getForm());
                p = pInto;*/
                //throw new RuntimeException();
            //}
            if (p != null) {
                p.setTheme(theme);
                try {
                    out = "Таксама як " + like + " :\n" + grConstr.toText(p);
                } catch (Throwable ex) {
                    out = "Error: " + ex.getMessage();
                }
            } else {
                out = "";
            }
            return out;
        }

        @Override
        protected void done() {
            try {
                UI.grammarPane.outXML.setText(get());
                UI.grammarPane.outXML.setCaretPosition(0);
            } catch (CancellationException ex) {
            } catch (Throwable ex) {
                ex.printStackTrace();
                UI.grammarPane.outXML.setText("Памылка: " + ex.getMessage());
            }
        }
    }
}
