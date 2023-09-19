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

import javax.swing.SwingWorker;

import org.alex73.grammardb.GrammarDB2;

/**
 * Чытае граматычную базу з файлаў.
 */
public class GrammarDbReader extends SwingWorker<Void, String> {

    GrammarDB2 result;

    public GrammarDbReader() {
        UI.showProgressMessage("Чытаем граматычную базу...");
    }

    @Override
    protected Void doInBackground() throws Exception {
        result = GrammarDB2.initializeFromJar();
        MainController.initGrammar(result);
        return null;
    }

    @Override
    protected void done() {
        try {
            get();

            if (result == null) {
                UI.showError("Няма граматычнай базы ў GrammarDB/");
                System.exit(1);
            }

            UI.showProgressMessage("Граматычная база прачытаная");
            UI.showEditor();
        } catch (Throwable ex) {
            ex.printStackTrace();
            UI.showError(ex.getClass().getSimpleName() + ": " + ex.getMessage());
            System.exit(1);
        }
    }
}
