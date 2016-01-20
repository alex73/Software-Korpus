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

import java.io.File;
import java.util.List;

import javax.swing.SwingWorker;

import org.alex73.korpus.editor.core.GrammarDB;

/**
 * Чытае граматычную базу з файлаў.
 */
public class GrammarDbReader extends SwingWorker<Void, String> implements GrammarDB.LoaderProgress {
    File themesFile;
    File[] xmlFiles;

    @Override
    public void setFilesCount(int count) {
        UI.mainProgress.progressBar.setMaximum(count * 2);
    }

    @Override
    public void beforeFileLoading(String file) {
        publish(file);
    }

    @Override
    public void afterFileLoading() {
        publish("");
    }

    @Override
    protected Void doInBackground() throws Exception {
        GrammarDB.initializeFromJar(this);
        if (GrammarDB.getInstance() == null) {
            GrammarDB.initializeFromDir(new File("GrammarDB"), this);
        }
        publish((String) null);
        return null;
    }

    @Override
    protected void process(List<String> chunks) {
        for (String f : chunks) {
            if (f == null) {
                UI.mainProgress.setVisible(false);
            } else {
                UI.mainProgress.progressBar.setValue(UI.mainProgress.progressBar.getValue() + 1);
                UI.mainProgress.lblText.setText(f);
            }
        }
    }

    @Override
    protected void done() {
        try {
            get();

            if (GrammarDB.getInstance().getAllParadigms().isEmpty()) {
                UI.showError("Няма граматычнай базы ў GrammarDB/");
                System.exit(1);
            }

            UI.showEditor();
        } catch (Throwable ex) {
            ex.printStackTrace();
            UI.showError(ex.getClass().getSimpleName() + ": " + ex.getMessage());
            System.exit(1);
        }
    }
}
