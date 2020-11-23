package org.alex73.korpus.server;

import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;

import org.alex73.korpus.base.BelarusianWordNormalizer;
import org.alex73.korpus.server.data.ClusterParams;
import org.alex73.korpus.server.data.ClusterResults;
import org.alex73.korpus.server.data.ResultText;
import org.alex73.korpus.server.data.WordRequest;
import org.alex73.korpus.server.data.WordResult;
import org.alex73.korpus.server.engine.LuceneDriverRead;
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

        BooleanQuery query = new BooleanQuery();
        process.addKorpusTextFilter(query, params.textStandard);

        WordRequest w = params.word;
        w.word = BelarusianWordNormalizer.normalize(w.word);
        process.addWordFilter(query, w);

        process.search(query, SEARCH_BLOCK, new LuceneDriverRead.DocFilter<Void>() {
            @Override
            public Void processDoc(int docID) throws Exception {
                process(docID, process);
                return null;
            }
        });

        return createResults();
    }

    private void process(int docID, LuceneFilter process) throws Exception {
        Document doc = process.getSentence(docID);

        ResultText text = parent.restoreText(params.corpusType, doc);

        for (int i = 0; i < text.words.length; i++) {
            for (int j = 0; j < text.words[i].length; j++) {
                if (WordsDetailsChecks.isOneWordMatchsParam(params.word, text.words[i][j])) {
                    process(text.words[i], j);
                }
            }
        }
    }

    private void process(WordResult[] words, int pos) {
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

        public Result(WordResult[] w, int pos, int beforeCount, int afterCount) {
            word = w[pos].orig;
            if (word == null) {
                word = "";
            }
            wordsBefore = new String[beforeCount];
            wordsAfter = new String[afterCount];

            for (int i = pos - 1, count = 0; i >= 0 && count < beforeCount; i--) {
                if (w[i].isWord) {
                    wordsBefore[beforeCount - count - 1] = w[i].orig;
                    count++;
                }
            }
            for (int i = pos + 1, count = 0; i < w.length && count < afterCount; i++) {
                if (w[i].isWord) {
                    wordsAfter[count] = w[i].orig;
                    count++;
                }
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
