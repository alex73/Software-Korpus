package org.alex73.korpus.editor;

import java.awt.Rectangle;
import java.awt.event.ItemListener;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

import javax.swing.AbstractListModel;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JScrollPane;
import javax.swing.SwingWorker;
import javax.swing.UIManager;
import javax.swing.undo.UndoManager;

import org.alex73.grammardb.GrammarDB2;
import org.alex73.korpus.base.GrammarFinder;
import org.alex73.korpus.base.StaticGrammarFiller2;
import org.alex73.korpus.editor.grammar.EditorGrammar;
import org.alex73.korpus.editor.ui.ErrorDialog;
import org.alex73.korpus.editor.ui.GrammarPane;
import org.alex73.korpus.editor.ui.JCheckBoxList;
import org.alex73.korpus.editor.ui.MainWindow2;
import org.alex73.korpus.text.structure.files.WordItem;

public class UI2 {
    public static MainWindow2 mainWindow;
    public static UndoManager editorUndoManager = new UndoManager();
    public static GrammarPane grammarPane;
    public static GrammarDB2 db;
    public static EditorGrammar gr;
    public static StaticGrammarFiller2 staticFiller;
    public static List<JCheckBox> cbs;
    public static GrammarPaneController3Main main;
    static MyList model;
    static boolean internalChange;

    public static void main(String[] args) throws Exception {
        UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());

        StaticGrammarFiller2.fillParadigmOnly = Boolean.parseBoolean(System.getProperty("FILL_PARADIGM_ONLY", "false"));
        StaticGrammarFiller2.fillTagPrefix = System.getProperty("FILL_TAG_PREFIX");
        StaticGrammarFiller2.fillTheme = System.getProperty("FILL_THEME");
        init();
    }

    public static void showEditor() {
        try {
            List<String> lines = Files.readAllLines(Paths.get("daviednik_Lemciuhovaj.text"), StandardCharsets.UTF_8);
            cbs = lines.stream().map(l -> {
                JCheckBox cb = new JCheckBox();
                cb.setToolTipText(l);
                cb.setName(l.replaceAll("^\\S+\\s+\\S+\\s+(\\S+)\\s+.+$", "$1"));
                cb.addItemListener(cbClicked);
                return cb;
            }).toList();

            model = new MyList();
        } catch (Exception ex) {
            ex.printStackTrace();
            return;
        }
        updateList();
        JScrollPane s = new JScrollPane(new JCheckBoxList(model));
        s.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        s.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
        mainWindow.jSplitPane1.setLeftComponent(s);
        main = new GrammarPaneController3Main();
        main.init();
        mainWindow.jSplitPane1.setRightComponent(main.ui);
    }

    static void updateList() {
        internalChange = true;
        cbs.forEach(cb -> {
            cb.setSelected(false);
            WordItem wi = new WordItem(cb.getName());
            gr.filler.fill(wi);
            cb.setText((wi.lemmas != null ? "зроблена / " : "") + cb.getToolTipText());
        });
        internalChange = false;
        model.update();
    }

    static ItemListener cbClicked = (e) -> {
        if (internalChange) {
            return;
        }
        List<String> selectedWords = cbs.stream().filter(cb -> cb.isSelected()).map(cb -> cb.getName()).sorted().distinct().toList();
        main.setWords(selectedWords);
    };

    public static void init() throws Exception {
        mainWindow = new MainWindow2();
        mainWindow.setTitle("Разгортванне слоў, v.2022-09-23");
        mainWindow.setBounds(200, 200, 900, 600);

        new SwingWorker<Void, String>() {

            @Override
            protected Void doInBackground() throws Exception {
                db = GrammarDB2.initializeFromJar();
                staticFiller = new StaticGrammarFiller2(new GrammarFinder(db));
                gr = new EditorGrammar(db, staticFiller, "daviednik_Lemciuhovaj-grammar.txt");
                return null;
            }

            @Override
            protected void done() {
                try {
                    get();
                    showEditor();
                } catch (Throwable ex) {
                    ex.printStackTrace();
                    showError(ex.getClass().getSimpleName() + ": " + ex.getMessage());
                    System.exit(1);
                }
            }
        }.execute();
        mainWindow.setVisible(true);
    }

    public static void setOnCenter(JFrame parent, JDialog child) {
        Rectangle r = parent.getBounds();
        Rectangle rd = child.getBounds();
        r.x += r.width / 2 - rd.width / 2;
        r.y += r.height / 2 - rd.height / 2;
        r.width = rd.width;
        r.height = rd.height;
        child.setBounds(r);
    }

    public static void showError(String error) {
        ErrorDialog dlg = new ErrorDialog(mainWindow, true);
        dlg.message.setText(error);
        dlg.pack();

        UI.setOnCenter(mainWindow, dlg);
        dlg.setVisible(true);
    }

    public static void showInfo(String info) {
        ErrorDialog dlg = new ErrorDialog(mainWindow, true);
        dlg.message.setText(info);
        dlg.pack();

        UI.setOnCenter(mainWindow, dlg);
        dlg.setVisible(true);
    }

    static class MyList extends AbstractListModel<JCheckBox> {
        @Override
        public int getSize() {
            return cbs.size();
        }

        @Override
        public JCheckBox getElementAt(int index) {
            return cbs.get(index);
        }

        public void update() {
            fireContentsChanged(this, 0, getSize());
        }
    };
}
