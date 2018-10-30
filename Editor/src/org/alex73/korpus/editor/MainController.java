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

import java.awt.Component;
import java.awt.Container;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;

import javax.swing.AbstractAction;
import javax.swing.ButtonGroup;
import javax.swing.JFileChooser;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JRadioButtonMenuItem;
import javax.swing.KeyStroke;
import javax.swing.event.CaretEvent;
import javax.swing.event.CaretListener;
import javax.swing.text.BadLocationException;
import javax.swing.text.DefaultEditorKit;
import javax.swing.text.Document;
import javax.swing.text.Element;
import javax.swing.text.PlainDocument;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.ViewFactory;

import org.alex73.korpus.base.GrammarDB2;
import org.alex73.korpus.base.GrammarFiller;
import org.alex73.korpus.base.GrammarFinder;
import org.alex73.korpus.editor.core.doc.KorpusDocument3;
import org.alex73.korpus.editor.core.doc.KorpusDocument3.MyLineElement;
import org.alex73.korpus.editor.core.doc.KorpusDocument3.MyWordElement;
import org.alex73.korpus.editor.core.doc.KorpusDocumentViewFactory;
import org.alex73.korpus.editor.grammar.EditorGrammar;
import org.alex73.korpus.editor.grammar.GrammarConstructor;
import org.alex73.korpus.text.TextGeneral;
import org.alex73.korpus.text.TextIO;
import org.alex73.korpus.text.TextPlain;
import org.alex73.korpus.text.parser.IProcess;
import org.alex73.korpus.text.parser.Splitter2;
import org.alex73.korpus.text.xml.W;
import org.alex73.korpus.text.xml.XMLText;

public class MainController {
    static final int[] FONT_SIZES = new int[] { 10, 12, 16, 20, 24, 30, 36, 44 };
    static String baseFileName;

    private static GrammarDB2 globalGr;
    private static GrammarFinder globalGrFinder;
    public static EditorGrammar gr;
    public static GrammarFiller filler;

    public static void initGrammar(GrammarDB2 gr) {
        globalGr = gr;
        globalGrFinder = new GrammarFinder(globalGr);
    }

    public static void init() {
        UI.mainWindow.mFileOpen.addActionListener(aFileOpen);
        UI.mainWindow.mFileSave.addActionListener(aFileSave);
        UI.mainWindow.mFileClose.addActionListener(aFileClose);
        UI.mainWindow.mUnk1.addActionListener(aUnderChange);
        UI.mainWindow.mGoNextMark.addActionListener(aGoNextMark);
        UI.mainWindow.mUnk1.addActionListener(aUnderChange);
        UI.mainWindow.mUnk2.addActionListener(aUnderChange);
        UI.mainWindow.mUnk3.addActionListener(aUnderChange);
        UI.mainWindow.mUndo.addActionListener(aUndo);
        UI.mainWindow.mRedo.addActionListener(aRedo);

        createFontChanger(UI.mainWindow.fontText, UI.mainWindow.bgText, UI.editor);
        createFontChanger(UI.mainWindow.fontInfo, UI.mainWindow.bgInfo, UI.wordInfoPane);
        createFontChanger(UI.mainWindow.fontGrammar, UI.mainWindow.bgGrammar, UI.grammarPane);

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

        UI.mainWindow.mSetText.addActionListener(new SetActionListener(null));
        UI.mainWindow.mSetOtherLanguage.addActionListener(new SetActionListener(KorpusDocument3.ATTRS_OTHER_LANGUAGE));
        UI.mainWindow.mSetDigits.addActionListener(new SetActionListener(KorpusDocument3.ATTRS_DIGITS));
        UI.mainWindow.mSetTrasianka.addActionListener(new SetActionListener(KorpusDocument3.ATTRS_TRASIANKA));
        UI.mainWindow.mSetDyjalekt.addActionListener(new SetActionListener(KorpusDocument3.ATTRS_DYJALEKT));
    }

    static void createFontChanger(JMenu menu, ButtonGroup bg, Container container) {
        for (int sz : FONT_SIZES) {
            JRadioButtonMenuItem rb = new JRadioButtonMenuItem("" + sz);
            rb.addActionListener(new FontChanger(container, sz));
            bg.add(rb);
            menu.add(rb);
        }
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
                File bak = new File(getOutFile().getPath() + ".bak");
                bak.delete();
                getOutFile().renameTo(bak);
                XMLText text = UI.doc.extractText();
                TextIO.saveXML(getOutFile(), text);
                UI.showInfo("Захавана ў " + getOutFile());
            } catch (Throwable ex) {
                ex.printStackTrace();
                UI.showError(ex.getClass().getSimpleName() + ": " + ex.getMessage());
            }
        }
    };
    static ActionListener aFileClose = new ActionListener() {
        public void actionPerformed(ActionEvent e) {
            closeFile();
        }
    };

    static void saveGrammar() throws Exception {
        gr.save();
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

    static class SetActionListener implements ActionListener {
        private final SimpleAttributeSet attrs;

        public SetActionListener(SimpleAttributeSet attrs) {
            this.attrs = attrs;
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            int p0 = UI.editor.getSelectionStart();
            int p1 = UI.editor.getSelectionEnd();
            try {
                String text = UI.doc.getText(p0, p1 - p0);
                UI.doc.replace(p0, p1 - p0, text, attrs);
            } catch (BadLocationException ex) {
                throw new RuntimeException(ex);
            }
        }
    };

    static class FontChanger implements ActionListener {
        private final Container c;
        private final int size;

        public FontChanger(Container c, int size) {
            this.c = c;
            this.size = size;
        }

        public void actionPerformed(ActionEvent e) {
            Font font = UI.mainWindow.getFont();
            font = new Font(font.getFamily(), font.getStyle(), size);
            setFont(c, font);
            GrammarPaneController.applyFont();
        }
    };

    static void setFont(Component c, Font font) {
        c.setFont(font);
        if (c instanceof Container) {
            Container co = (Container) c;
            for (int i = 0; i < co.getComponentCount(); i++) {
                setFont(co.getComponent(i), font);
            }
        }
    }

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
            baseFileName = f.getPath().replaceAll("\\.[a-z]+$", "");

            gr = new EditorGrammar(globalGr, baseFileName + "-grammar.xml");
            filler = new GrammarFiller(globalGrFinder, gr);
            GrammarPaneController.grConstr = new GrammarConstructor(gr);

            XMLText kDoc;
            if (f.getName().endsWith(".xml")) {
                InputStream in = new BufferedInputStream(new FileInputStream(f));
                try {
                    kDoc = TextIO.parseXML(in);
                } finally {
                    in.close();
                }
            } else if (f.getName().endsWith(".text")) {
                kDoc = new TextGeneral(f, new IProcess() {
                    @Override
                    public void showStatus(String status) {
                    }

                    @Override
                    public void reportError(String error) {
                        throw new RuntimeException(error);
                    }
                }).parse();
            } else {
                kDoc = new TextPlain(f, new IProcess() {
                    @Override
                    public void showStatus(String status) {
                    }

                    @Override
                    public void reportError(String error) {
                        throw new RuntimeException(error);
                    }
                }).parse();
            }
            TextIO.saveXML(new File(baseFileName + ".orig.xml"), kDoc);

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

            UI.mainWindow.mFileOpen.setEnabled(false);
            UI.mainWindow.mFileSave.setEnabled(true);
            UI.mainWindow.mFileClose.setEnabled(true);
        } catch (Throwable ex) {
            ex.printStackTrace();
            UI.showError(ex.getClass().getSimpleName() + ": " + ex.getMessage());
        }
    }

    static void closeFile() {
        gr = null;
        UI.doc = null;
        UI.editor.removeCaretListener(onWordChanged);
        UI.editor.setEditorKit(UI.editor.getEditorKitForContentType("text/plain"));
        UI.editor.setDocument(new PlainDocument());

        UI.mainWindow.mFileOpen.setEnabled(true);
        UI.mainWindow.mFileSave.setEnabled(false);
        UI.mainWindow.mFileClose.setEnabled(false);
    }

    static File getOutFile() {
        return new File(baseFileName + ".xml");
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
