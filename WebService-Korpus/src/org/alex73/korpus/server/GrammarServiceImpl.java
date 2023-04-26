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
import org.alex73.korpus.languages.ILanguage;
import org.alex73.korpus.languages.LanguageFactory;
import org.alex73.korpus.languages.belarusian.FormsReadyFilter;
import org.alex73.korpus.server.data.GrammarInitial;
import org.alex73.korpus.shared.GrammarSearchResult;
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
    static final ILanguage.INormalizer wordNormalizer = LanguageFactory.get("bel").getNormalizer();

    @Context
    HttpServletRequest request;

    private ApplicationKorpus getApp() {
        return ApplicationKorpus.instance;
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
        public String lang;
        public String word;
        public boolean multiForm;
        public String grammar;
        public String outputGrammar;
        public boolean orderReverse;
        public boolean outputGrouping;
        public boolean fullDatabase;
    }

    @Path("search")
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public GrammarSearchResult search(GrammarRequest rq) throws Exception {
        System.out.println(">> Request from " + request.getRemoteAddr());
        System.out.println(">> Request: word=" + rq.word + " orderReverse=" + rq.orderReverse);
        try {
            ILanguage lang = LanguageFactory.get(rq.lang);
            GrammarSearchResult result = new GrammarSearchResult();
            if (rq.word != null) {
                rq.word = rq.word.trim();
            }
            if (rq.word.isEmpty()) {
                rq.word = null;
            }
            if (rq.grammar != null && rq.grammar.isEmpty()) {
                rq.grammar = null;
            }
            int grammarCount = 0;
            int lettersCount = 0;
            boolean hasStar = false;
            if (rq.word != null) {
                hasStar = hasWildcards(rq.word);
                for (char c : rq.word.toCharArray()) {
                    if ("абвгдеёжзійклмнопрстуўфхцчшыьэюя'".indexOf(Character.toLowerCase(c)) >= 0) {
                        lettersCount++;
                    }
                }
            }
            if (rq.grammar != null) {
                grammarCount = rq.grammar.replace(".", "").length();
            }
            if (grammarCount > 0) {
                // ok
            } else if (hasStar && lettersCount >= 2) {
                // ok
            } else if (!hasStar && lettersCount > 0) {
                // ok
            } else {
                result.error = "Увядзіце слова для пошуку альбо удакладніце граматыку";
                return result;
            }
            WordsDetailsChecks check = new WordsDetailsChecks();

            ThreadLocal<Pattern> reGrammar = null, reOutputGrammar = null;
            if (rq.grammar != null && !rq.grammar.isEmpty()) {
                reGrammar = check.createPatternRegexp(rq.grammar);
            }
            if (rq.outputGrammar != null && !rq.outputGrammar.isEmpty()) {
                reOutputGrammar = check.createPatternRegexp(rq.outputGrammar);
            }
            Stream<LemmaInfo> output;
            if (rq.word == null || hasWildcards(rq.word)) {
                output = StreamSupport.stream(
                        new SearchWidlcards(lang, check.createWildcardRegexp(rq.word), reGrammar, rq.multiForm, rq.fullDatabase, reOutputGrammar), false);
            } else {
                output = searchExact(lang, rq.word, reGrammar, rq.multiForm, rq.fullDatabase, reOutputGrammar);
            }
            result.output = output.limit(Settings.GRAMMAR_SEARCH_RESULT_PAGE).collect(Collectors.toList());

            // remove duplicates
            Collections.sort(result.output, (a, b) -> {
                int r = Long.compare(a.pdgId, b.pdgId);
                if (r == 0) {
                    r = BEL.compare(a.output, b.output);
                }
                return r;
            });
            for (int i = 1; i < result.output.size(); i++) {
                LemmaInfo a = result.output.get(i - 1);
                LemmaInfo b = result.output.get(i);
                if (a.pdgId == b.pdgId && a.output.equals(b.output)) {
                    result.output.remove(i);
                    i--;
                }
            }
            for (int i = 1; i < result.output.size(); i++) {
                LemmaInfo a = result.output.get(i - 1);
                LemmaInfo b = result.output.get(i);
                if (a.pdgId == b.pdgId) {
                    result.hasDuplicateParadigms = true;
                    break;
                }
            }
            if (rq.outputGrouping) {
                for (int i = 1; i < result.output.size(); i++) {
                    LemmaInfo a = result.output.get(i - 1);
                    LemmaInfo b = result.output.get(i);
                    if (a.pdgId == b.pdgId) {
                        a.output += ", " + b.output;
                        a.meaning = null;
                        b.meaning = null;
                        result.output.remove(i);
                        i--;
                    }
                }
                Collections.sort(result.output, new Comparator<LemmaInfo>() {
                    @Override
                    public int compare(LemmaInfo o1, LemmaInfo o2) {
                        return BEL.compare(o1.output, o2.output);
                    }
                });
            } else if (rq.orderReverse) {
                Collections.sort(result.output, new Comparator<LemmaInfo>() {
                    @Override
                    public int compare(LemmaInfo o1, LemmaInfo o2) {
                        return BEL.compare(revert(o1.output), revert(o2.output));
                    }
                });
            } else {
                Collections.sort(result.output, new Comparator<LemmaInfo>() {
                    @Override
                    public int compare(LemmaInfo o1, LemmaInfo o2) {
                        return BEL.compare(o1.output, o2.output);
                    }
                });
            }

            if (result.output.isEmpty()) { // nothing found
                if (rq.word != null && rq.grammar == null && !rq.multiForm && !hasWildcards(rq.word)) {// simple word search
                    Stream<LemmaInfo> output2 = searchExact(lang, rq.word, null, true, rq.fullDatabase, null);
                    if (output2.anyMatch(p -> true)) {
                        result.hasMultiformResult = true;
                    }
                }
            }
            System.out.println("<< Result: " + result.output.size());
            return result;
        } catch (Throwable ex) {
            System.out.println(ex);
            throw ex;
        }
    }

    class SearchWidlcards extends Spliterators.AbstractSpliterator<LemmaInfo> {
        private final ILanguage lang;
        private final ThreadLocal<Pattern> re;
        private final ThreadLocal<Pattern> reGrammar;
        private final boolean multiform;
        private final boolean fullDatabase;
        private final ThreadLocal<Pattern> reOutputGrammar;
        private final LinkedList<LemmaInfo> buffer = new LinkedList<>();
        private final List<Paradigm> data;
        private int dataPos;

        public SearchWidlcards(ILanguage lang, ThreadLocal<Pattern> reWord, ThreadLocal<Pattern> reGrammar, boolean multiform, boolean fullDatabase,
                ThreadLocal<Pattern> reOutputGrammar) {
            super(Long.MAX_VALUE, 0);
            this.lang = lang;
            this.re = reWord;
            this.reGrammar = reGrammar;
            this.multiform = multiform;
            this.fullDatabase = fullDatabase;
            this.reOutputGrammar = reOutputGrammar;
            data = getApp().gr.getAllParadigms();
        }

        @Override
        public boolean tryAdvance(Consumer<? super LemmaInfo> action) {
            while (dataPos < data.size() && buffer.isEmpty()) {
                Paradigm p = data.get(dataPos);
                dataPos++;
                createLemmaInfoFromParadigm(lang, p,
                        s -> re == null || re.get().matcher(lang.getNormalizer().lightNormalized(s, ILanguage.INormalizer.PRESERVE_WILDCARDS)).matches(),
                        multiform, fullDatabase, reOutputGrammar, reGrammar, buffer);
            }
            if (buffer.isEmpty()) {
                return false;
            }
            action.accept(buffer.removeFirst());
            return true;
        }
    }

    private Stream<LemmaInfo> searchExact(ILanguage lang, String word, ThreadLocal<Pattern> reGrammar, boolean multiform, boolean fullDatabase,
            ThreadLocal<Pattern> reOutputGrammar) {
        String normWord = wordNormalizer.lightNormalized(word.trim(), ILanguage.INormalizer.PRESERVE_NONE);
        Paradigm[] data = getApp().grFinder.getParadigms(normWord);
        List<LemmaInfo> result = new ArrayList<>();
        for (Paradigm p : data) {
            createLemmaInfoFromParadigm(lang, p, s -> normWord.equals(wordNormalizer.lightNormalized(s, ILanguage.INormalizer.PRESERVE_NONE)), multiform,
                    fullDatabase, reOutputGrammar, reGrammar, result);
        }
        return result.stream();
    }

    private void createLemmaInfoFromParadigm(ILanguage lang, Paradigm p, Predicate<String> checkWord, boolean multiform, boolean fullDatabase,
            ThreadLocal<Pattern> reOutputGrammar, ThreadLocal<Pattern> reGrammar, List<LemmaInfo> result) {
        Set<String> found = new TreeSet<>();
        for (Variant v : p.getVariant()) {
            List<Form> forms = fullDatabase ? v.getForm() : FormsReadyFilter.getAcceptedForms(FormsReadyFilter.MODE.SHOW, p, v);
            if (forms == null) {
                return;
            }
            if (multiform) {
                for (Form f : forms) {
                    if (checkWord.test(f.getValue())) {
                        if (reGrammar != null && !reGrammar.get().matcher(lang.getDbTags().getDBTagString(SetUtils.tag(p, v, f))).matches()) {
                            continue;
                        }
                        found.add(f.getValue());
                    }
                }
            } else {
                if (checkWord.test(v.getLemma())) {
                    if (reGrammar != null) {
                        boolean tagFound = false;
                        for (Form f : forms) {
                            if (reGrammar.get().matcher(lang.getDbTags().getDBTagString(SetUtils.tag(p, v, f))).matches()) {
                                tagFound = true;
                                break;
                            }
                        }
                        if (!tagFound) {
                            continue;
                        }
                    }
                    found.add(v.getLemma());
                }
            }
            for (String f : found) {
                createLemmaInfo(lang, p, v, f, reOutputGrammar, result);
            }
        }
    }

    private void createLemmaInfo(ILanguage lang, Paradigm p, Variant v, String output, ThreadLocal<Pattern> reOutputGrammar, List<LemmaInfo> result) {
        String tag = SetUtils.tag(p, v);
        if (reOutputGrammar != null) {
            Set<String> found = new TreeSet<>();
            for (Form f : v.getForm()) {
                if (reOutputGrammar.get().matcher(lang.getDbTags().getDBTagString(SetUtils.tag(p, v, f))).matches()) {
                    found.add(f.getValue());
                }
            }
            for (String f : found) {
                LemmaInfo w = new LemmaInfo();
                w.pdgId = p.getPdgId();
                w.meaning = p.getMeaning();
                w.output = StressUtils.combineAccute(f);
                w.grammar = String.join(", ", lang.getTags().describe(tag, getApp().grammarInitial.skipGrammar.get(tag.charAt(0))));
                result.add(w);
            }
        } else {
            LemmaInfo w = new LemmaInfo();
            w.pdgId = p.getPdgId();
            w.meaning = p.getMeaning();
            w.output = StressUtils.combineAccute(output);
            w.grammar = String.join(", ", lang.getTags().describe(tag, getApp().grammarInitial.skipGrammar.get(tag.charAt(0))));
            result.add(w);
        }
    }

    @Path("details/{id}")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public LemmaInfo.LemmaParadigm getLemmaDetails(@PathParam("id") long pdgId) throws Exception {
        try {
            for (Paradigm p : getApp().gr.getAllParadigms()) {
                if (p.getPdgId() == pdgId) {
                    return conv(p, false);
                }
            }
        } catch (Exception ex) {
            ex.printStackTrace();
            throw ex;
        }
        return null;
    }

    @Path("detailsFull/{id}")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public LemmaInfo.LemmaParadigm getLemmaFullDetails(@PathParam("id") long pdgId) throws Exception {
        try {
            for (Paradigm p : getApp().gr.getAllParadigms()) {
                if (p.getPdgId() == pdgId) {
                    return conv(p, true);
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
            form = wordNormalizer.lightNormalized(form, ILanguage.INormalizer.PRESERVE_NONE);
            for (Paradigm p : getApp().grFinder.getParadigms(form)) {
                for (Variant v : p.getVariant()) {
                    for (Form f : v.getForm()) {
                        if (wordNormalizer.lightNormalized(f.getValue(), ILanguage.INormalizer.PRESERVE_NONE).equals(form)) {
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

    LemmaInfo.LemmaParadigm conv(Paradigm p, boolean fullDatabase) {
        LemmaInfo.LemmaParadigm r = new LemmaInfo.LemmaParadigm();
        r.lemma = StressUtils.combineAccute(p.getLemma());
        r.tag = p.getTag();
        r.meaning = p.getMeaning();
        for (Variant v : p.getVariant()) {
            List<Form> forms = fullDatabase ? v.getForm() : FormsReadyFilter.getAcceptedForms(FormsReadyFilter.MODE.SHOW, p, v);
            if (forms == null) {
                continue;
            }
            LemmaInfo.LemmaVariant rv = new LemmaInfo.LemmaVariant();
            rv.id = v.getId();
            rv.tag = v.getTag();
            rv.dictionaries.addAll(SetUtils.getSlouniki(v.getSlouniki()).keySet());
            createAuthorsList(p.getLemma(), rv);
            r.variants.add(rv);
            for (Form f : forms) {
                LemmaInfo.LemmaForm rf = new LemmaInfo.LemmaForm();
                rf.value = StressUtils.combineAccute(f.getValue());
                rf.tag = f.getTag();
                rf.options = f.getOptions() != null ? f.getOptions().name() : null;
                rf.type = f.getType() != null ? f.getType().name() : null;
                rv.dictionaries.addAll(SetUtils.getSlouniki(f.getSlouniki()).keySet());
                rv.forms.add(rf);
            }
        }
        return r;
    }

    void createAuthorsList(String lemma, LemmaInfo.LemmaVariant rv) {
        Set<String> origAuthors = getApp().authorsByLemmas.get(lemma);
        if (origAuthors == null) {
            return;
        }

        Set<String> authors = new TreeSet<>(origAuthors);
        for (LemmaInfo.Author author : getApp().authors) {
            if (authors.remove(author.name)) {
                rv.authors.add(author);
            }
        }
    }

    String revert(String s) {
        StringBuilder r = new StringBuilder(s);
        return r.reverse().toString();
    }

    private boolean hasWildcards(String w) {
        return w.contains("*") || w.contains("?");
    }
}
