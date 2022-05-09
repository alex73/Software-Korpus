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

import javax.swing.UIManager;

import org.alex73.korpus.base.StaticGrammarFiller2;

public class Editor2 {
    public static String[] params;

    public static void main(String[] args) throws Exception {
        UIManager.setLookAndFeel(
                UIManager.getSystemLookAndFeelClassName());

        StaticGrammarFiller2.fillParadigmOnly = Boolean.parseBoolean(System.getProperty("FILL_PARADIGM_ONLY", "false"));
        StaticGrammarFiller2.fillTagPrefix = System.getProperty("FILL_TAG_PREFIX");
        StaticGrammarFiller2.fillTheme = System.getProperty("FILL_THEME");

        params = args;
        try {
            UI.init();

            UI.showProgress();
            new GrammarDbReader().execute();

            UI.mainWindow.setVisible(true);
        } catch (Throwable ex) {
            ex.printStackTrace();
            UI.showError(ex.getClass().getSimpleName() + ": " + ex.getMessage());
        }
    }

}
