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

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.ComponentOrientation;
import java.awt.Dimension;
import java.awt.Rectangle;
import java.io.File;
import java.io.InputStream;
import java.util.Locale;

import javax.swing.JDialog;
import javax.swing.JEditorPane;
import javax.swing.JFrame;
import javax.swing.JScrollPane;
import javax.swing.ScrollPaneConstants;
import javax.swing.undo.UndoManager;

import org.alex73.korpus.editor.core.doc.KorpusDocument3;
import org.alex73.korpus.editor.ui.ErrorDialog;
import org.alex73.korpus.editor.ui.GrammarPane;
import org.alex73.korpus.editor.ui.MainWindow;
import org.alex73.korpus.editor.ui.ProgressPane;
import org.alex73.korpus.editor.ui.WordInfoPane;

import com.vlsolutions.swing.docking.DockKey;
import com.vlsolutions.swing.docking.Dockable;
import com.vlsolutions.swing.docking.DockingConstants;
import com.vlsolutions.swing.docking.DockingDesktop;
import com.vlsolutions.swing.docking.event.DockableStateWillChangeEvent;
import com.vlsolutions.swing.docking.event.DockableStateWillChangeListener;
import com.vlsolutions.swing.docking.ui.DockingUISettings;

public class UI {
    public static MainWindow mainWindow;
    public static JEditorPane editor;
    public static UndoManager editorUndoManager = new UndoManager();
    public static KorpusDocument3 doc;
    static ProgressPane mainProgress;
    public static WordInfoPane wordInfoPane;
    public static GrammarPane grammarPane;

    public static void init() throws Exception {
        mainWindow = new MainWindow();
        mainWindow.setTitle("Рэдагаваньне файлаў корпусу, v.2.1");

        // GrammarDialogController.init(mainWindow);
        mainWindow.setBounds(200, 200, 900, 600);
    }

    public static void showProgress() {
        mainProgress = new ProgressPane();
        mainWindow.getContentPane().add(mainProgress);
    }
    
    public static void showProgressMessage(String message) {
        mainProgress.lblText.setText(message);
    }

    public static void showEditor() throws Exception {
        mainProgress = null;
        mainWindow.getContentPane().remove(0);

        DockingUISettings.getInstance().installUI();
        DockingDesktop desktop = new DockingDesktop();
        desktop.addDockableStateWillChangeListener(new DockableStateWillChangeListener() {
            public void dockableStateWillChange(DockableStateWillChangeEvent event) {
                if (event.getFutureState().isClosed())
                    event.cancel();
            }
        });
        mainWindow.getContentPane().add(desktop, BorderLayout.CENTER);

        editor = new JEditorPane();
        DockableScrollPane pane = new DockableScrollPane("EDITOR", "Тэкст", editor, false);
        pane.setComponentOrientation(ComponentOrientation.getOrientation(Locale.getDefault()));
        pane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        pane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);
        pane.setMinimumSize(new Dimension(100, 100));

        desktop.addDockable(pane);

        wordInfoPane = new WordInfoPane();
        wordInfoPane.setMinimumSize(new Dimension(100, 100));
        pane = new DockableScrollPane("WORD_INFO", "Зьвесткі пра слова", wordInfoPane, true);
        pane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        desktop.addDockable(pane);
        WordInfoPaneController.init();

        grammarPane = new GrammarPane();
        grammarPane.setMinimumSize(new Dimension(100, 100));
        pane = new DockableScrollPane("GRAMMAR_DB", "Граматычная база", grammarPane, true);
        pane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        desktop.addDockable(pane);
        GrammarPaneController.init();

        MainController.init();

        resetDesktopLayout(desktop);

        if (Editor2.params.length > 0) {
            MainController.openFile(new File(Editor2.params[0]));
        }
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

    static void resetDesktopLayout(final DockingDesktop desktop) throws Exception {
        InputStream in = UI.class.getResourceAsStream("docking.xml");
        try {
            desktop.readXML(in);
        } finally {
            in.close();
        }
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

    @SuppressWarnings("serial")
    public static class DockableScrollPane extends JScrollPane implements Dockable {
        DockKey dockKey;

        /** Updates the name of the docking pane. */
        public void setName(String name) {
            dockKey.setName(name);
        }

        /** Creates a new instance of DockableScrollBox */
        public DockableScrollPane(String key, String name, Component view, boolean detouchable) {
            super(view);
            dockKey = new DockKey(key, name, null, null, DockingConstants.HIDE_BOTTOM);
            dockKey.setFloatEnabled(detouchable);
        }

        public DockKey getDockKey() {
            return dockKey;
        }

        public Component getComponent() {
            return this;
        }
    }
}
