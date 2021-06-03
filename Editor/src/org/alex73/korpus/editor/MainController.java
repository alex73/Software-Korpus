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
import java.nio.file.Files;
import java.util.List;
import java.util.Map;

import javax.swing.AbstractAction;
import javax.swing.ButtonGroup;
import javax.swing.JFileChooser;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JRadioButtonMenuItem;
import javax.swing.KeyStroke;
import javax.swing.event.CaretEvent;
import javax.swing.event.CaretListener;
import javax.swing.text.DefaultEditorKit;
import javax.swing.text.Document;
import javax.swing.text.Element;
import javax.swing.text.PlainDocument;
import javax.swing.text.ViewFactory;

import org.alex73.korpus.base.GrammarDB2;
import org.alex73.korpus.base.GrammarFinder;
import org.alex73.korpus.base.StaticGrammarFiller2;
import org.alex73.korpus.editor.core.doc.KorpusDocument3;
import org.alex73.korpus.editor.core.doc.KorpusDocument3.MyLineElement;
import org.alex73.korpus.editor.core.doc.KorpusDocument3.MyWordElement;
import org.alex73.korpus.editor.core.doc.KorpusDocumentViewFactory;
import org.alex73.korpus.editor.grammar.EditorGrammar;
import org.alex73.korpus.text.parser.IProcess;
import org.alex73.korpus.text.parser.PtextFileParser;
import org.alex73.korpus.text.parser.PtextFileWriter;
import org.alex73.korpus.text.parser.TextFileParser;
import org.alex73.korpus.text.parser.TextFileWriter;
import org.alex73.korpus.text.structure.corpus.Word;
import org.alex73.korpus.text.structure.corpus.Word.OtherType;
import org.alex73.korpus.text.structure.files.TextLine;
import org.alex73.korpus.text.structure.files.WordItem;

public class MainController {
    static final int[] FONT_SIZES = new int[] { 10, 12, 16, 20, 24, 30, 36, 44 };
    static String baseFileName;

    public static EditorGrammar gr;
    private static GrammarDB2 db;
    private static Map<String, String> headers;
    private static StaticGrammarFiller2 staticFiller;

    public static void initGrammar(GrammarDB2 gr) {
        db = gr;
        staticFiller = new StaticGrammarFiller2(new GrammarFinder(gr));
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
        UI.mainWindow.mSetOtherLanguage.addActionListener(new SetActionListener(OtherType.OTHER_LANGUAGE));
        UI.mainWindow.mSetDigits.addActionListener(new SetActionListener(OtherType.NUMBER));
        UI.mainWindow.mSetTrasianka.addActionListener(new SetActionListener(OtherType.TRASIANKA));
        UI.mainWindow.mSetDyjalekt.addActionListener(new SetActionListener(OtherType.DYJALEKT));
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
                PtextFileWriter.write(getOutFile(), headers, UI.doc.extractText());
                TextFileWriter.write(new File(baseFileName + ".baktext"), headers, UI.doc.extractText());
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
        private final OtherType type;

        public SetActionListener(OtherType type) {
            this.type = type;
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            int p0 = UI.editor.getSelectionStart();
            int p1 = UI.editor.getSelectionEnd();
            List<MyLineElement> lines = UI.doc.getParagraphs(p0, p1);
            for (MyLineElement line : lines) {
                int f = line.getElementIndex(p0);
                int t = line.getElementIndex(p1);
                for (int i = f; i <= t; i++) {
                    MyWordElement w = line.getElement(i);
                    if (w.item instanceof WordItem) {
                        WordItem wi = (WordItem) w.item;
                        wi.type = this.type;
                    }
                }
            }
            UI.editor.repaint();
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
                MyWordElement elWord = elParagraph.getElement(w);
                if (elWord.isMarked()) {
                    UI.editor.setCaretPosition(elWord.getStartOffset());
                    return;
                }
            }
            // all other paragraphs
            for (p++; p < root.getElementCount(); p++) {
                elParagraph = (MyLineElement) root.getElement(p);
                for (w = 0; w < elParagraph.getElementCount(); w++) {
                    MyWordElement elWord = elParagraph.getElement(w);
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

            gr = new EditorGrammar(db, staticFiller, baseFileName + "-grammar.xml");
            List<TextLine> lines;
            if (f.getName().endsWith(".ptext")) {
                try (BufferedInputStream in = new BufferedInputStream(Files.newInputStream(f.toPath()))) {
                    PtextFileParser parser = new PtextFileParser(in, false, new IProcess() {
                        @Override
                        public void showStatus(String status) {
                        }

                        @Override
                        public void reportError(String error, Throwable ex) {
                            throw new RuntimeException(error, ex);
                        }
                    });
                    headers = parser.headers;
                    lines =parser.lines;
                }
            } else if (f.getName().endsWith(".text")) {
                try (BufferedInputStream in = new BufferedInputStream(Files.newInputStream(f.toPath()))) {
                    TextFileParser parser = new TextFileParser(in, false, new IProcess() {
                        @Override
                        public void showStatus(String status) {
                        }

                        @Override
                        public void reportError(String error, Throwable ex) {
                            throw new RuntimeException(error, ex);
                        }
                    });
                    headers = parser.headers;
                    lines =parser.lines;
                }
            } else {
                throw new RuntimeException("Unknown file format");
            }
            gr.filler.fillNonManual(lines);

            UI.doc = new KorpusDocument3(lines);
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
        return new File(baseFileName + ".ptext");
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
        KorpusDocument3.MyWordElement word = par.getElement(idxWord);
        if (word.item instanceof WordItem) {
            WordItem wi = (WordItem) word.item;
            Integer pdgId = null;
            try {
                KorpusDocument3.MyWordElement wordNext = par.getElement(idxWord + 1);
                String textNext = wordNext.getElementText();
                if (textNext.startsWith("~")) {
                    pdgId = Integer.parseInt(textNext.substring(1));
                }
            } catch (Exception ex) {
            }
            WordInfoPaneController.show(wi);
            GrammarPaneController.show(wi, pdgId);
        }
    }

    static void setWordInfo(String manualLemma, String manualTag) {
        int pos = UI.editor.getCaretPosition();
        KorpusDocument3.MyLineElement par = (KorpusDocument3.MyLineElement) UI.doc.getParagraphElement(pos);
        int idxWord = par.getElementIndex(pos);
        KorpusDocument3.MyWordElement word = par.getElement(idxWord);
        if (word == null) {
            return;
        }
        if (word.item instanceof WordItem) {
            WordItem wi = (WordItem) word.item;
            wi.manualLemma = manualLemma;
            wi.manualTag = manualTag;
        }
        UI.editor.repaint();
    }
}
