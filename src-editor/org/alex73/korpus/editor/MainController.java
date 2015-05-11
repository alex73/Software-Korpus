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

import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;

import javax.swing.AbstractAction;
import javax.swing.JFileChooser;
import javax.swing.JMenuBar;
import javax.swing.KeyStroke;
import javax.swing.event.CaretEvent;
import javax.swing.event.CaretListener;
import javax.swing.text.DefaultEditorKit;
import javax.swing.text.Document;
import javax.swing.text.Element;
import javax.swing.text.ViewFactory;

import org.alex73.korpus.editor.core.GrammarDB;
import org.alex73.korpus.editor.core.doc.KorpusDocument3;
import org.alex73.korpus.editor.core.doc.KorpusDocument3.MyLineElement;
import org.alex73.korpus.editor.core.doc.KorpusDocument3.MyWordElement;
import org.alex73.korpus.editor.core.doc.KorpusDocumentViewFactory;
import org.alex73.korpus.editor.core.structure.KorpusDocument;
import org.alex73.korpus.parser.TEIParser;
import org.alex73.korpus.parser.TextParser;

import alex73.corpus.paradigm.TEI;
import alex73.corpus.paradigm.W;
import alex73.corpus.text.XMLText;

public class MainController {
    static String baseFileName;

    public static void init() {
        UI.mainWindow.mFileOpen.addActionListener(aFileOpen);
        UI.mainWindow.mFileSave.addActionListener(aFileSave);
        UI.mainWindow.mUnk1.addActionListener(aUnderChange);
        UI.mainWindow.mGoNextMark.addActionListener(aGoNextMark);
        UI.mainWindow.mUnk1.addActionListener(aUnderChange);
        UI.mainWindow.mUnk2.addActionListener(aUnderChange);
        UI.mainWindow.mUnk3.addActionListener(aUnderChange);
        UI.mainWindow.mUndo.addActionListener(aUndo);
        UI.mainWindow.mRedo.addActionListener(aRedo);
        UI.mainWindow.mFontDec.addActionListener(aFontDec);
        UI.mainWindow.mFontInc.addActionListener(aFontInc);

        UI.mainWindow.mGoEditor.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                UI.editor.requestFocus();
            }
        });
        UI.mainWindow.mGoWordInfo.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                if (UI.wordInfoPane.pLemma.getComponentCount() > 0) {
                    UI.wordInfoPane.pLemma.getComponent(0).requestFocus();
                }
            }
        });

        JMenuBar frameContentPane = (JMenuBar) UI.mainWindow.getJMenuBar();
        frameContentPane.getActionMap().put("GoGrammar", actionGoGrammar);

        UI.editor.getInputMap().put(KeyStroke.getKeyStroke("F6"), "GoGrammar");
        UI.editor.getActionMap().put("GoGrammar", actionGoGrammar);
    }

    static AbstractAction actionGoGrammar = new AbstractAction() {
        public void actionPerformed(ActionEvent e) {
            UI.grammarPane.txtWord.requestFocus();
        }
    };

    static ActionListener aFileOpen = new ActionListener() {
        public void actionPerformed(ActionEvent e) {
            JFileChooser fc = new JFileChooser(new File("."));
            if (fc.showOpenDialog(UI.mainWindow) != JFileChooser.APPROVE_OPTION) {
                return;
            }
            openFile(fc.getSelectedFile());
        }
    };
    static ActionListener aFileSave = new ActionListener() {
        public void actionPerformed(ActionEvent e) {
            try {
                XMLText tei = new XMLText();
                tei.setText(TextParser.constructXML(UI.doc.extractText()));
                TextParser.saveXML(getOutFile(), tei);
                UI.showInfo("Захавана ў " + getOutFile());
            } catch (Throwable ex) {
                ex.printStackTrace();
                UI.showError(ex.getClass().getSimpleName() + ": " + ex.getMessage());
            }
        }
    };

    static void saveGrammar() throws Exception {
        GrammarDB.getInstance().saveDocLevelParadygms(getGrammarFile());
    }

    static ActionListener aUnderChange = new ActionListener() {
        public void actionPerformed(ActionEvent e) {
            if (UI.mainWindow.mUnk1.isSelected()) {
                UI.doc.markType = KorpusDocument3.MARK_WORDS.UNK_LEMMA;
            } else if (UI.mainWindow.mUnk2.isSelected()) {
                UI.doc.markType = KorpusDocument3.MARK_WORDS.AMAN_LEMMA;
            } else if (UI.mainWindow.mUnk3.isSelected()) {
                UI.doc.markType = KorpusDocument3.MARK_WORDS.AMAN_GRAM;
            }
            UI.editor.repaint();
        }
    };

    static ActionListener aUndo = new ActionListener() {
        public void actionPerformed(ActionEvent e) {
            if (UI.editorUndoManager.canUndo()) {
                UI.editorUndoManager.undo();
            }
        }
    };

    static ActionListener aRedo = new ActionListener() {
        public void actionPerformed(ActionEvent e) {
            if (UI.editorUndoManager.canRedo()) {
                UI.editorUndoManager.redo();
            }
        }
    };

    static ActionListener aFontInc = new ActionListener() {
        public void actionPerformed(ActionEvent e) {
            Font font = UI.editor.getFont();
            font = new Font(font.getFamily(), font.getStyle(), font.getSize() + 1);
            UI.editor.setFont(font);
        }
    };

    static ActionListener aFontDec = new ActionListener() {
        public void actionPerformed(ActionEvent e) {
            Font font = UI.editor.getFont();
            font = new Font(font.getFamily(), font.getStyle(), font.getSize() - 1);
            UI.editor.setFont(font);
        }
    };

    static ActionListener aGoNextMark = new ActionListener() {
        public void actionPerformed(ActionEvent e) {
            int caretPos = UI.editor.getCaretPosition();
            UI.editor.requestFocus();

            Element root = UI.doc.getDefaultRootElement();
            int p = root.getElementIndex(caretPos);

            MyLineElement elParagraph = (MyLineElement) root.getElement(p);
            int w = elParagraph.getElementIndex(caretPos);
            // to the end of paragraph
            for (w++; w < elParagraph.getElementCount(); w++) {
                MyWordElement elWord = (MyWordElement) elParagraph.getElement(w);
                if (elWord.isMarked()) {
                    UI.editor.setCaretPosition(elWord.getStartOffset());
                    return;
                }
            }
            // all other paragraphs
            for (p++; p < root.getElementCount(); p++) {
                elParagraph = (MyLineElement) root.getElement(p);
                for (w = 0; w < elParagraph.getElementCount(); w++) {
                    MyWordElement elWord = (MyWordElement) elParagraph.getElement(w);
                    if (elWord.isMarked()) {
                        UI.editor.setCaretPosition(elWord.getStartOffset());
                        return;
                    }
                }
            }
        }
    };

    public static void openFile(File f) {
        try {
            baseFileName = f.getPath().replace("\\.[a-z]+$", "");

            UI.mainWindow.mFileOpen.setEnabled(false);
            if (getGrammarFile().exists()) {
                GrammarDB.getInstance().addXMLFile(getGrammarFile(), true);
            }

            KorpusDocument kDoc;
            if (f.getName().endsWith(".xml")) {
                InputStream in = new BufferedInputStream(new FileInputStream(f));
                try {
                    kDoc = TextParser.parseXML(in);
                } finally {
                    in.close();
                }
            } else {
                InputStream in = new BufferedInputStream(new FileInputStream(f));
                try {
                    kDoc = TextParser.parseText(in, false);
                } finally {
                    in.close();
                }
            }

            UI.doc = new KorpusDocument3(kDoc);
            final KorpusDocumentViewFactory viewFactory = new KorpusDocumentViewFactory();
            UI.editor.setEditorKit(new DefaultEditorKit() {
                public ViewFactory getViewFactory() {
                    return viewFactory;
                }

                public Document createDefaultDocument() {
                    return UI.doc;
                }
            });
            // UI.editor.setDocument(UI.doc);
            UI.editor.addCaretListener(onWordChanged);
        } catch (Throwable ex) {
            ex.printStackTrace();
            UI.showError(ex.getClass().getSimpleName() + ": " + ex.getMessage());
        }
    }

    static File getGrammarFile() {
        return new File(baseFileName + "-grammar.xml");
    }

    static File getOutFile() {
        return new File(baseFileName + "-out.xml");
    }

    static CaretListener onWordChanged = new CaretListener() {
        /*
         * Show information about word.
         */
        public void caretUpdate(CaretEvent e) {
            showWordInfos(e.getDot());
        }
    };

    static void showWordInfos(int pos) {
        KorpusDocument3.MyLineElement par = (KorpusDocument3.MyLineElement) UI.doc.getParagraphElement(pos);
        int idxWord = par.getElementIndex(pos);
        KorpusDocument3.MyWordElement word = (KorpusDocument3.MyWordElement) par.getElement(idxWord);
        WordInfoPaneController.show(word.getWordInfo());
        GrammarPaneController.show(word.getWordInfo());
    }

    static void setWordInfo(W w) {
        int pos = UI.editor.getCaretPosition();
        KorpusDocument3.MyLineElement par = (KorpusDocument3.MyLineElement) UI.doc.getParagraphElement(pos);
        int idxWord = par.getElementIndex(pos);
        KorpusDocument3.MyWordElement word = (KorpusDocument3.MyWordElement) par.getElement(idxWord);
        if (word == null) {
            return;
        }
        word.setWordInfo(w);
        UI.editor.repaint();
    }
}
