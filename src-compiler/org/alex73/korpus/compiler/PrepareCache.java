/**************************************************************************
 Korpus - Corpus Linguistics Software.

 Copyright (C) 2015 Aleś Bułojčyk (alex73mail@gmail.com)
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

package org.alex73.korpus.compiler;

import java.io.File;
import java.util.Locale;

import org.alex73.korpus.editor.core.GrammarDB;

/**
 * Class for prepare data for search.
 */
public class PrepareCache {
    public static void main(String[] args) throws Exception {
        Locale.setDefault(new Locale("be"));

        System.out.println("Load GrammarDB...");

        GrammarDB.initializeFromDir(new File("GrammarDB"), new GrammarDB.LoaderProgress() {
            public void setFilesCount(int count) {
            }

            public void beforeFileLoading(String file) {
                System.out.println("Load " + file);
            }

            public void afterFileLoading() {
            }
        });

        KorpusLoading.processKorpus();
        OtherLoading.processOther();
    }
}
