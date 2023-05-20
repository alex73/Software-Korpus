package org.alex73.korpus.server;

import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.alex73.korpus.base.TextInfo;
import org.alex73.korpus.languages.ILanguage;
import org.alex73.korpus.languages.LanguageFactory;
import org.alex73.korpus.server.data.ChainRequest;
import org.alex73.korpus.server.data.ClusterParams;
import org.alex73.korpus.server.data.ClusterResults;
import org.alex73.korpus.server.data.WordRequest;
import org.alex73.korpus.server.data.WordResult;
import org.alex73.korpus.server.engine.LuceneDriverRead;
import org.alex73.korpus.text.structure.corpus.Paragraph;
import org.alex73.korpus.text.structure.corpus.Sentence;
import org.apache.lucene.document.Document;
import org.apache.lucene.search.BooleanQuery;

public class ClusterServiceImpl {
    private final static int SEARCH_BLOCK = 10000;
    private final SearchServiceImpl parent;
    private ClusterParams params;
    private final Map<String, Result> results = new HashMap<>();

    public ClusterServiceImpl(SearchServiceImpl parent) {
        this.parent = parent;
    }

    public ClusterResults calc(ClusterParams params, LuceneFilter process) throws Exception {
        this.params = params;
        ILanguage lang = LanguageFactory.get(params.lang);

        BooleanQuery.Builder query = new BooleanQuery.Builder();
        process.addKorpusTextFilter(params.lang, query, params.textStandard);

        WordRequest w = params.word;
        process.addWordFilter(params.lang, query, w);

        ChainRequest chain = new ChainRequest();
        chain.words = List.of(w);
        WordsDetailsChecks check = new WordsDetailsChecks(lang, List.of(chain), false, ApplicationKorpus.instance.grFiller);

        process.search(query.build(), SEARCH_BLOCK, new LuceneDriverRead.DocFilter<Void>() {
            @Override
            public Void processDoc(int docID) throws Exception {
                process(check, docID, process);
                return null;
            }
        });

        return createResults();
    }

    private void process(WordsDetailsChecks check, int docID, LuceneFilter process) throws Exception {
        Document doc = process.getSentence(docID);
        TextInfo textInfo = ApplicationKorpus.instance.getTextInfo(process.getTextID(doc));
        Paragraph[] ps = parent.restoreText(doc);

        for (int pi = 0; pi < ps.length; pi++) {
            for (int i = 0; i < ps[pi].sentences.length; i++) {
                for (int j = 0; j < ps[pi].sentences[i].words.length; j++) {
                    if (check.isOneWordAllowed(textInfo, (WordResult) ps[pi].sentences[i].words[j])) {
                        process(ps[pi].sentences[i], j);
                    }
                }
            }
        }
    }

    private void process(Sentence words, int pos) {
        Result r = new Result(words, pos, params.wordsBefore, params.wordsAfter);

        String key = r.getKey();
        synchronized (results) {
            Result prevResult = results.get(key);
            if (prevResult == null) {
                prevResult = r;
                results.put(key, r);
            }
            prevResult.counts++;
        }
    }

    private ClusterResults createResults() {
        ClusterResults res = new ClusterResults();
        res.rows = new ClusterResults.Row[results.size()];
        int i = 0;
        for (Result r : results.values()) {
            res.rows[i] = r.toRow();
            i++;
        }
        Arrays.sort(res.rows, new Comparator<ClusterResults.Row>() {
            @Override
            public int compare(ClusterResults.Row r1, ClusterResults.Row r2) {
                return r2.count - r1.count;
            }
        });
        return res;
    }

    public class Result {
        int counts;
        String[] wordsBefore;
        String word;
        String[] wordsAfter;

        public Result(Sentence w, int pos, int beforeCount, int afterCount) {
            word = w.words[pos].word;
            if (word == null) {
                word = "";
            }
            wordsBefore = new String[beforeCount];
            wordsAfter = new String[afterCount];

            for (int i = pos - 1, count = 0; i >= 0 && count < beforeCount; i--) {
                wordsBefore[beforeCount - count - 1] = w.words[i].word;
                count++;
            }
            for (int i = pos + 1, count = 0; i < w.words.length && count < afterCount; i++) {
                wordsAfter[count] = w.words[i].word;
                count++;
            }
        }

        public String getKey() {
            StringBuilder str = new StringBuilder(256);
            for (String w : wordsBefore) {
                if (w != null) {
                    str.append(w.toLowerCase());
                }
                str.append('\t');
            }
            str.append(word.toLowerCase());
            for (String w : wordsAfter) {
                str.append('\t');
                if (w != null) {
                    str.append(w.toLowerCase());
                }
            }
            return str.toString();
        }

        @Override
        public boolean equals(Object other) {
            Result r = (Result) other;
            if (!Arrays.equals(wordsBefore, r.wordsBefore)) {
                return false;
            }
            if (word.equals(r.word)) {
                return false;
            }
            if (!Arrays.equals(wordsAfter, r.wordsAfter)) {
                return false;
            }
            return true;
        }

        public ClusterResults.Row toRow() {
            ClusterResults.Row r = new ClusterResults.Row();
            r.wordsBefore = wordsBefore;
            r.word = word;
            r.wordsAfter = wordsAfter;
            r.count = counts;
            return r;
        }
    }
}
