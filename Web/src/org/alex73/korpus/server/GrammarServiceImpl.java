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
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.Spliterators;
import java.util.TreeSet;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

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
import org.alex73.korpus.base.DBTagsGroups;
import org.alex73.korpus.belarusian.BelarusianWordNormalizer;
import org.alex73.korpus.belarusian.FormsReadyFilter;
import org.alex73.korpus.server.data.GrammarInitial;
import org.alex73.korpus.shared.LemmaInfo;
import org.alex73.korpus.utils.SetUtils;
import org.alex73.korpus.utils.StressUtils;

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
        public String word;
        public boolean multiForm;
        public String grammar;
        public String outputGrammar;
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
            Pattern reGrammar = null, reOutputGrammar = null;
            if (rq.grammar != null && !rq.grammar.isEmpty()) {
                reGrammar = WordsDetailsChecks.getPatternRegexp(rq.grammar);
            }
            if (rq.outputGrammar != null && !rq.outputGrammar.isEmpty()) {
                reOutputGrammar = WordsDetailsChecks.getPatternRegexp(rq.outputGrammar);
            }
            if (rq.word != null) {
                rq.word = rq.word.trim();
                if (rq.word.isEmpty()) {
                    rq.word = null;
                }
            }
            Stream<LemmaInfo> output;
            if (rq.word == null || WordsDetailsChecks.needWildcardRegexp(rq.word)) {
                output = StreamSupport.stream(new SearchWidlcards(rq.word, reGrammar, rq.multiForm, reOutputGrammar), false);
            } else {
                output = searchExact(rq.word, reGrammar, rq.multiForm, reOutputGrammar);
            }
            List<LemmaInfo> result = output.limit(Settings.GRAMMAR_SEARCH_RESULT_PAGE).collect(Collectors.toList());

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

    class SearchWidlcards extends Spliterators.AbstractSpliterator<LemmaInfo> {
        private final Pattern re;
        private final Pattern reGrammar;
        private final boolean multiform;
        private final Pattern reOutputGrammar;
        private final LinkedList<LemmaInfo> buffer = new LinkedList<>();
        private final List<Paradigm> data;
        private int dataPos;

        public SearchWidlcards(String word, Pattern reGrammar, boolean multiform, Pattern reOutputGrammar) {
            super(Long.MAX_VALUE, 0);
            this.re = word == null ? null : WordsDetailsChecks.getWildcardRegexp(word.trim());
            this.reGrammar = reGrammar;
            this.multiform = multiform;
            this.reOutputGrammar = reOutputGrammar;
            data = getApp().gr.getAllParadigms();
        }

        @Override
        public boolean tryAdvance(Consumer<? super LemmaInfo> action) {
            while (dataPos < data.size() && buffer.isEmpty()) {
                Paradigm p = data.get(dataPos);
                dataPos++;
                createLemmaInfoFromParadigm(p, s -> re == null || re.matcher(StressUtils.unstress(s)).matches(),
                        multiform, reOutputGrammar, reGrammar, buffer);
            }
            if (buffer.isEmpty()) {
                return false;
            }
            action.accept(buffer.removeFirst());
            return true;
        }
    }

    private Stream<LemmaInfo> searchExact(String word, Pattern reGrammar, boolean multiform, Pattern reOutputGrammar) {
        String normWord = BelarusianWordNormalizer.lightNormalized(word.trim());
        Paradigm[] data = getApp().grFinder.getParadigms(normWord);
        List<LemmaInfo> result = new ArrayList<>();
        for (Paradigm p : data) {
            createLemmaInfoFromParadigm(p, s -> BelarusianWordNormalizer.equals(normWord, s), multiform, reOutputGrammar,
                    reGrammar, result);
        }
        return result.stream();
    }

    private void createLemmaInfoFromParadigm(Paradigm p, Predicate<String> checkWord, boolean multiform,
            Pattern reOutputGrammar, Pattern reGrammar, List<LemmaInfo> result) {
        for (Variant v : p.getVariant()) {
            List<Form> forms = FormsReadyFilter.getAcceptedForms(FormsReadyFilter.MODE.SHOW, p, v);
            if (forms == null) {
                return;
            }
            if (multiform) {
                for (Form f : forms) {
                    if (checkWord.test(f.getValue())) {
                        if (reGrammar != null
                                && !reGrammar.matcher(DBTagsGroups.getDBTagString(SetUtils.tag(p, v, f))).matches()) {
                            continue;
                        }
                        result.add(createLemmaInfo(p, v, reOutputGrammar));
                        break;
                    }
                }
            } else {
                if (checkWord.test(v.getLemma())) {
                    if (reGrammar != null
                            && !reGrammar.matcher(DBTagsGroups.getDBTagString(SetUtils.tag(p, v))).matches()) {
                        continue;
                    }
                    result.add(createLemmaInfo(p, v, reOutputGrammar));
                }
            }
        }
    }

    private LemmaInfo createLemmaInfo(Paradigm p, Variant v, Pattern reOutputGrammar) {
        LemmaInfo w = new LemmaInfo();
        w.pdgId = p.getPdgId();
        w.meaning = p.getMeaning();
        if (reOutputGrammar != null) {
            List<String> fs = new ArrayList<>();
            for (Form f : v.getForm()) {
                if (reOutputGrammar.matcher(DBTagsGroups.getDBTagString(SetUtils.tag(p, v, f))).matches()) {
                    fs.add(StressUtils.combineAccute(f.getValue()));
                }
            }
            if (!fs.isEmpty()) {
                w.lemma = String.join(", ", fs);
            }
        }
        if (w.lemma == null) {
            w.lemma = StressUtils.combineAccute(v.getLemma());
        }
        w.lemmaGrammar = SetUtils.tag(p, v);
        return w;
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
            form = BelarusianWordNormalizer.lightNormalized(form);
            for (Paradigm p : getApp().grFinder.getParadigms(form)) {
                for (Variant v : p.getVariant()) {
                    for (Form f : v.getForm()) {
                        if (BelarusianWordNormalizer.equals(f.getValue(), form)) {
                            result.add(p.getLemma());
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
            List<Form> forms = FormsReadyFilter.getAcceptedForms(FormsReadyFilter.MODE.SHOW, p, v);
            if (forms == null) {
                continue;
            }
            r.lemma = StressUtils.combineAccute(p.getLemma());
            r.tag = p.getTag();
            r.meaning = p.getMeaning();
            LemmaInfo.LemmaVariant rv = new LemmaInfo.LemmaVariant();
            r.variants.add(rv);
            for (Form f : forms) {
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
