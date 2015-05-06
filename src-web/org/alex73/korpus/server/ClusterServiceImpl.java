package org.alex73.korpus.server;

import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;

import org.alex73.korpus.server.engine.LuceneDriverRead;
import org.alex73.korpus.shared.dto.ClusterParams;
import org.alex73.korpus.shared.dto.ClusterResults;
import org.alex73.korpus.shared.dto.CorpusType;
import org.alex73.korpus.shared.dto.ResultText;
import org.alex73.korpus.shared.dto.WordRequest;
import org.alex73.korpus.shared.dto.WordResult;
import org.apache.lucene.document.Document;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.ScoreDoc;

public class ClusterServiceImpl {
    private final static int SEARCH_BLOCK = 10000;
    private final SearchServiceImpl parent;
    private ClusterParams params;
    private final Map<String, Result> results = new HashMap<>();
    private StringBuilder str = new StringBuilder();

    public ClusterServiceImpl(SearchServiceImpl parent) {
        this.parent = parent;
    }

    public ClusterResults calc(ClusterParams params) throws Exception {
        this.params = params;

        BooleanQuery query = new BooleanQuery();
        LuceneFilter process = parent.getProcess(params.corpusType);
        if (params.corpusType == CorpusType.STANDARD) {
            process.addKorpusTextFilter(query, params.textStandard);
        } else {
            process.addOtherTextFilter(query, params.textUnprocessed);
        }

        WordRequest w = params.word;
        process.addWordFilter(query, w);

        ScoreDoc latestDoc = null;
        while (true) {
            ScoreDoc[] found = process.search(query, latestDoc, SEARCH_BLOCK,
                    new LuceneDriverRead.DocFilter() {
                        public boolean isDocAllowed(int docID) {
                            return true;
                        }
                    });

            timeAll = 0;
            timeParse = 0;
            long be = System.currentTimeMillis();
            for (ScoreDoc doc : found) {
                process(doc.doc);
            }
            long af = System.currentTimeMillis();
            timeAll += af - be;
            System.out.println("all: " + timeAll + "  parse:" + timeParse);
            if (found.length < SEARCH_BLOCK) {
                break;
            }
            latestDoc = found[found.length - 1];
        }

        return createResults();
    }

    static long timeAll, timeParse;

    private void process(int docID) throws Exception {
        Document doc = parent.getProcess(params.corpusType).getSentence(docID);

        ResultText text = parent.restoreText(params.corpusType, doc);

        for (int i = 0; i < text.words.length; i++) {
            for (int j = 0; j < text.words[i].length; j++) {
                if (WordsDetailsChecks.isWordMatchsParam(params.word, text.words[i][j])) {
                    if (params.wordsBefore > j) {
                        continue; // no such many words before
                    }
                    if (params.wordsAfter > text.words[i].length - j - 1) {
                        continue; // no such many words after
                    }
                    process(text.words[i], j);
                }
            }
        }
    }

    private void process(WordResult[] words, int pos) {
        Result r = new Result(words, pos - params.wordsBefore, pos + params.wordsAfter);

        String key = r.getKey();
        Result prevResult = results.get(key);
        if (prevResult == null) {
            prevResult = r;
            results.put(key, r);
        } else if (!prevResult.equals(r)) {
            prevResult.toLowerCase = true;
        }
        prevResult.counts++;
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
        String[] words;
        boolean toLowerCase;

        public Result(WordResult[] w, int from, int to) {
            words = new String[to - from + 1];
            for (int i = 0; i < words.length; i++) {
                words[i] = w[i + from].value;
            }
        }

        public String getKey() {
            str.setLength(0);
            for (int i = 0; i < words.length; i++) {
                str.append(words[i].toLowerCase());
                str.append('\t');
            }
            str.setLength(str.length() - 1);
            return str.toString();
        }

        @Override
        public boolean equals(Object other) {
            Result r = (Result) other;
            if (words.length != r.words.length) {
                return false;
            }
            for (int i = 0; i < words.length; i++) {
                if (!words[i].equals(r.words[i])) {
                    return false;
                }
            }
            return true;
        }

        public ClusterResults.Row toRow() {
            ClusterResults.Row r = new ClusterResults.Row();
            if (toLowerCase) {
                for (int i = 0; i < words.length; i++) {
                    words[i] = words[i].toLowerCase();
                }
            }
            r.words = words;
            r.count = counts;
            return r;
        }
    }
}
