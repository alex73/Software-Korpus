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

import java.text.Collator;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Pattern;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;

import org.alex73.corpus.paradigm.Form;
import org.alex73.corpus.paradigm.Paradigm;
import org.alex73.corpus.paradigm.Variant;
import org.alex73.korpus.base.BelarusianTags;
import org.alex73.korpus.base.BelarusianWordNormalizer;
import org.alex73.korpus.base.DBTagsGroups;
import org.alex73.korpus.server.data.GrammarInitial;
import org.alex73.korpus.server.data.WordRequest;
import org.alex73.korpus.shared.LemmaInfo;
import org.alex73.korpus.utils.SetUtils;
import org.alex73.korpus.utils.StressUtils;
import org.apache.commons.lang.StringUtils;

/**
 * Service for find by grammar database.
 */
@Path("/grammar")
public class GrammarServiceImpl {

    public static Locale BE = new Locale("be");
    public static Collator BEL = Collator.getInstance(BE);

    public static String dirPrefix = System.getProperty("KORPUS_DIR");

    @Context
    HttpServletRequest request;

    private KorpusApplication getApp() {
        return KorpusApplication.instance;
    }

    @Path("initial")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public GrammarInitial getInitialData() throws Exception {
        System.out.println("getInitialData from " + request.getRemoteAddr());
        try {
            return getApp().grammarInitial;
        } catch (Exception ex) {
            System.err.println("getInitialData");
            ex.printStackTrace();
            throw ex;
        }
    }

    public static class GrammarRequest {
        public WordRequest word;
        public boolean orderReverse;
    }

    @Path("search")
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public LemmaInfo[] search(GrammarRequest rq) throws Exception {
        System.out.println(">> Request from " + request.getRemoteAddr());
        System.out.println(">> Request: word=" + rq.word + " orderReverse=" + rq.orderReverse);
        try {
            List<LemmaInfo> result = new ArrayList<>();
            Pattern reLemma = null;
            Pattern reWord = null;
            if (rq.word.allForms) {
                if (StringUtils.isNotBlank(rq.word.word)) {
                    reWord = Pattern.compile(rq.word.word.replace("*", ".*"));
                }
            } else {
                if (StringUtils.isNotBlank(rq.word.word)) {
                    reLemma = Pattern.compile(rq.word.word.replace("*", ".*"));
                }
            }

            Pattern reGrammar = StringUtils.isEmpty(rq.word.grammar) ? null : Pattern.compile(rq.word.grammar);

         // TODO change to finder
            begpar: for (Paradigm p : getApp().gr.getAllParadigms()) {
                if (reLemma != null) {
                    if (!reLemma.matcher(StressUtils.unstress(BelarusianWordNormalizer.normalizePreserveCase(p.getLemma())))
                            .matches()) {
                        continue;
                    }
                }
                boolean found = false;
                for (Variant v : p.getVariant()) {
                    for (Form f : v.getForm()) {
                        if (StringUtils.isEmpty(f.getValue())) {
                            continue;
                        }
                        if (reGrammar != null) {
                            String fTag = SetUtils.tag(p, v, f);
                            if (!BelarusianTags.getInstance().isValid(fTag, null)) {
                                continue;
                            }
                            if (!reGrammar.matcher(DBTagsGroups.getDBTagString(fTag)).matches()) {
                                continue;
                            }
                        }
                        if (reWord != null) {
                            if (!reWord.matcher(StressUtils.unstress(BelarusianWordNormalizer.normalizePreserveCase(f.getValue())))
                                    .matches()) {
                                continue;
                            }
                        }

                        found = true;
                        break;
                    }
                }
                if (found) {
                    LemmaInfo w = new LemmaInfo();
                    w.pdgId = p.getPdgId();
                    w.lemma = StressUtils.combineAccute(p.getLemma());
                    w.lemmaGrammar = p.getTag();
                    result.add(w);
                    if (Settings.GRAMMAR_SEARCH_RESULT_PAGE > 0
                            && result.size() >= Settings.GRAMMAR_SEARCH_RESULT_PAGE + 1) {
                        break begpar;
                    }
                }
            }
            if (rq.orderReverse) {
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
            System.out.println("<< Result: " + result.size());
            return result.toArray(new LemmaInfo[result.size()]);
        } catch (Throwable ex) {
            System.out.println(ex);
            throw ex;
        }
    }

    @Path("details/{id}")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public LemmaInfo.LemmaParadigm getLemmaDetails(@PathParam("id") long pdgId) throws Exception {
        try {
            for (Paradigm p : getApp().gr.getAllParadigms()) {
                if (p.getPdgId() == pdgId) {
                    return conv(p);
                }
            }
        } catch (Exception ex) {
            ex.printStackTrace();
            throw ex;
        }
        return null;
    }

    @Path("lemmas/{form}")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public String[] getLemmasByForm(@PathParam("form") String form) throws Exception {
        System.out.println(">> Find lemmas by form " + form);
        Set<String> result = Collections.synchronizedSet(new TreeSet<>());
        try {
            form = BelarusianWordNormalizer.normalizePreserveCase(form);
            for (Paradigm p : getApp().grFinder.getParadigms(form)) {
                for (Variant v : p.getVariant()) {
                    for (Form f : v.getForm()) {
                        if (form.equals(BelarusianWordNormalizer.normalizePreserveCase(f.getValue()))) {
                            result.add(BelarusianWordNormalizer.normalizePreserveCase(v.getLemma()));
                            break;
                        }
                    }
                }
            }
            System.out.println("<< Find lemmas by form result: " + result);
            List<String> resultList = new ArrayList<>(result);
            Collections.sort(resultList, BEL);
            return resultList.toArray(new String[result.size()]);
        } catch (Exception ex) {
            ex.printStackTrace();
            throw ex;
        }
    }

    LemmaInfo.LemmaParadigm conv(Paradigm p) {
        LemmaInfo.LemmaParadigm r = new LemmaInfo.LemmaParadigm();
        for (Variant v : p.getVariant()) {
            r.lemma = StressUtils.combineAccute(p.getLemma());
            r.tag = p.getTag();
            LemmaInfo.LemmaVariant rv = new LemmaInfo.LemmaVariant();
            r.variants.add(rv);
            for (Form f : v.getForm()) {
                LemmaInfo.LemmaForm rf = new LemmaInfo.LemmaForm();
                rf.value = StressUtils.combineAccute(f.getValue());
                rf.tag = f.getTag();
                rv.forms.add(rf);
            }
        }
        return r;
    }

    String revert(String s) {
        StringBuilder r = new StringBuilder(s);
        return r.reverse().toString();
    }
}
