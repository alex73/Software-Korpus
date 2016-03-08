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

package org.alex73.korpus.server;

import java.io.File;
import java.text.Collator;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

import org.alex73.korpus.base.BelarusianTags;
import org.alex73.korpus.base.DBTagsGroups;
import org.alex73.korpus.client.GrammarService;
import org.alex73.korpus.shared.LemmaInfo;
import org.alex73.korpus.utils.WordNormalizer;
import org.apache.commons.lang.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.google.gwt.user.server.rpc.RemoteServiceServlet;

/**
 * Service for find by grammar database.
 */
@SuppressWarnings("serial")
public class GrammarServiceImpl extends RemoteServiceServlet implements GrammarService {

    static final Logger LOGGER = LogManager.getLogger(GrammarServiceImpl.class);

    public static Locale BE = new Locale("be");
    public static Collator BEL = Collator.getInstance(BE);

    public GrammarServiceImpl() throws Exception {
        LOGGER.info("startup");
        try {
            GrammarDBLite.initializeFromDir(new File("GrammarDB"));
        } catch (Throwable ex) {
            LOGGER.error("startup", ex);
            throw new ExceptionInInitializerError(ex);
        }
    }

    @Override
    public void destroy() {
        LOGGER.info("shutdown");
        super.destroy();
    }

    @Override
    protected void doUnexpectedFailure(Throwable e) {
        LOGGER.error("UnexpectedFailure", e);
        super.doUnexpectedFailure(e);
    }

    @Override
    public LemmaInfo[] search(String lemmaMask, String wordMask, String grammar, boolean orderReverse)
            throws Exception {
        LOGGER.info(">> Request from " + getThreadLocalRequest().getRemoteAddr());
        LOGGER.info(">> Request: lemmaMask=" + lemmaMask + " wordMask=" + wordMask + " grammar=" + grammar
                + " orderReverse=" + orderReverse);
        try {
            List<LemmaInfo> result = new ArrayList<>();
            Pattern reLemma = StringUtils.isEmpty(lemmaMask) ? null : Pattern.compile(lemmaMask.replace("*",
                    ".*"));
            Pattern reWord = StringUtils.isEmpty(wordMask) ? null : Pattern.compile(wordMask.replace("*",
                    ".*"));
            Pattern reGrammar = StringUtils.isEmpty(grammar) ? null : Pattern.compile(grammar);

            begpar: for (LiteParadigm p : GrammarDBLite.getInstance().getAllParadigms()) {
                if (reLemma != null) {
                    if (!reLemma.matcher(WordNormalizer.normalize(p.lemma)).matches()) {
                        continue;
                    }
                }
                boolean found = false;
                for (LiteForm f : p.forms) {
                    if (StringUtils.isEmpty(f.value)) {
                        continue;
                    }
                    if (reGrammar != null) {
                        String fTag = p.tag + f.tag;
                        if (!BelarusianTags.getInstance().isValid(fTag, null)) {
                            continue;
                        }
                        if (!reGrammar.matcher(DBTagsGroups.getDBTagString(fTag)).matches()) {
                            continue;
                        }
                    }
                    if (reWord != null) {
                        if (!reWord.matcher(WordNormalizer.normalize(f.value)).matches()) {
                            continue;
                        }
                    }

                    found = true;
                    break;
                }
                if (found) {
                    LemmaInfo w = new LemmaInfo();
                    w.lemma = p.lemma;
                    w.lemmaGrammar = p.tag;
                    result.add(w);
                    if (Settings.GRAMMAR_SEARCH_RESULT_PAGE > 0
                            && result.size() >= Settings.GRAMMAR_SEARCH_RESULT_PAGE + 1) {
                        break begpar;
                    }
                }
            }
            if (orderReverse) {
                Collections.sort(result, new Comparator<LemmaInfo>() {
                    @Override
                    public int compare(LemmaInfo o1, LemmaInfo o2) {
                        return BEL.compare(revert(o1.lemma), revert(o2.lemma));
                    }
                });
            } else {
                Collections.sort(result, new Comparator<LemmaInfo>() {
                    @Override
                    public int compare(LemmaInfo o1, LemmaInfo o2) {
                        return BEL.compare(o1.lemma, o2.lemma);
                    }
                });
            }
            LOGGER.info("<< Result: " + result.size());
            return result.toArray(new LemmaInfo[result.size()]);
        } catch (Throwable ex) {
            LOGGER.error(ex);
            throw ex;
        }
    }

    public LemmaInfo[] getLemmaDetails(LemmaInfo lemma) throws Exception {
        try {
            List<LemmaInfo> result = new ArrayList<>();

            begpar: for (LiteParadigm p : GrammarDBLite.getInstance().getAllParadigms()) {
                if (lemma.lemma.equals(p.lemma)) {
                    LemmaInfo li = new LemmaInfo();
                    li.lemma = p.lemma;
                    li.lemmaGrammar = p.tag;
                    result.add(li);
                    List<LemmaInfo.Word> words = new ArrayList<>();
                    for (LiteForm f : p.forms) {
                        LemmaInfo.Word w = new LemmaInfo.Word();
                        w.value = f.value;
                        w.cat = p.tag + f.tag;
                        words.add(w);
                    }
                    li.words = words.toArray(new LemmaInfo.Word[words.size()]);
                }

                if (Settings.GRAMMAR_SEARCH_RESULT_PAGE > 0
                        && result.size() >= Settings.GRAMMAR_SEARCH_RESULT_PAGE + 1) {
                    break begpar;
                }
            }
            return result.toArray(new LemmaInfo[result.size()]);
        } catch (Exception ex) {
            ex.printStackTrace();
            throw ex;
        }
    }

    String revert(String s) {
        StringBuilder r = new StringBuilder(s);
        return r.reverse().toString();
    }
}
