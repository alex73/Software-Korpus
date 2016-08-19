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

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.alex73.korpus.base.GrammarDB2;
import org.alex73.korpus.base.GrammarFiller;
import org.alex73.korpus.base.GrammarFinder;
import org.alex73.korpus.text.parser.IProcess;
import org.alex73.korpus.text.parser.Splitter2;
import org.alex73.korpus.text.xml.P;

/**
 * Class for prepare data for search.
 */
public class PrepareCache {
    private static GrammarDB2 gr;
    public static void main(String[] args) throws Exception {
        Locale.setDefault(new Locale("be"));

        new PrepareCache().process(true);
    }
    
    public void process(boolean processOther) throws Exception {
        System.out.println("Load GrammarDB...");

        gr = GrammarDB2.initializeFromDir("GrammarDB");
        Splitter2.init(new GrammarFiller(new GrammarFinder(gr)));

        new KorpusLoading(errors, new CallbackP() {
            public void processP(P p) {
                doProcessP(p);
            }
        }).processKorpus();
        if (processOther) {
            new OtherLoading(errors, new CallbackP() {
                public void processP(P p) {
                    doProcessP(p);
                }
            }).processOther();
        }

        List<String> errorNames = new ArrayList<>(errorsCount.keySet());
        Collections.sort(errorNames, new Comparator<String>() {
            @Override
            public int compare(String o1, String o2) {
                int c1 = errorsCount.get(o1);
                int c2 = errorsCount.get(o2);
                return c1 - c2;
            }
        });
        for (String e : errorNames) {
            System.err.println("ERROR: " + e + ": " + errorsCount.get(e));
        }
    }

    public void doProcessP(P p) {}

    Map<String, Integer> errorsCount = new HashMap<>();
    public IProcess errors = new IProcess() {
        @Override
        public void showStatus(String status) {
            // System.out.println(status);
        }

        @Override
        public synchronized void reportError(String error) {
            Integer count = errorsCount.get(error);
            if (count == null) {
                count = 1;
            } else {
                count++;
            }
            // errorsCount.put(error, count);
            System.err.println(error);
        }
    };

    interface CallbackP {
        public void processP(P p);
    }
}
