package org.alex73.korpus.server.data;

import java.io.Serializable;

@SuppressWarnings("serial")
public class WordRequest implements Serializable {
    public enum WordMode {
        /**
         * Звычайны пошук - параўноўваем уведзенае слова(пасля lightNormalize) з словам
         * у корпусе, не зважаем на розніцу малых/вялікіх літар, у/ў, etc.
         * 
         * Калі пазначана "з варыянтамі напісання", папярэдне адкідаем мяккія знакі.
         * 
         * Калі пазначана граматыка, бяром з базы толькі граматыку, але словы
         * параўноўваем як раней.
         */
        USUAL,
        /**
         * Усе словаформы - шукаем канкрэтнае слова ў граматычнай базе, і выцягваем спіс
         * усіх формаў з гэтай лемы. Калі пазначана граматыка, бяром з базы яшчэ і
         * граматыку.
         * 
         * Калі пазначана "з варыянтамі напісання", папярэдне адкідаем мяккія знакі
         * паўсюль.
         * 
         * Параўноўваем слова ў тэксце з спісам усіх формаў.
         * 
         * P.S. Ручное зняцце аманіміі пакуль не падтрымліваем.
         */
        ALL_FORMS,
        /**
         * Дакладны пошук - зважаем на розніцу малых/вялікіх літар, у/ў, etc.
         */
        EXACT
    };

    public WordMode mode;
    public boolean variants;
    public String word;
    public String grammar;
    public transient String[] lemmas; // list of calculated lemmas for find in Lucene

    @Override
    public String toString() {
        return "mode=" + mode + " variants=" + variants + " word=" + word + " grammar=" + grammar;
    }
}
