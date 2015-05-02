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
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.RegexpQuery;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TermQuery;

public class ClusterServiceImpl {
    private final static int SEARCH_BLOCK = 1000;
    private final SearchServiceImpl parent;
    private ClusterParams params;
    private final Map<String, Integer> counts = new HashMap<>();

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
        if (w.word.length() > 0) {
            Query wq;
            if (w.allForms) {
                w.lemmas = parent.findAllLemmas(w.word);
                if (w.lemmas.isEmpty()) {
                    throw new RuntimeException(ServerError.REQUIEST_LEMMA_NOT_FOUND);
                }
                BooleanQuery qLemmas = new BooleanQuery();
                for (String lemma : w.lemmas) {
                    qLemmas.add(new TermQuery(process.getLemmaTerm(lemma)), BooleanClause.Occur.SHOULD);
                }
                wq = qLemmas;
            } else {
                wq = new TermQuery(process.getValueTerm(w.word));
            }

            query.add(wq, BooleanClause.Occur.MUST);
        }
        if (w.grammar != null) {
            Query wq = new RegexpQuery(process.getGrammarTerm(w.grammar));
            query.add(wq, BooleanClause.Occur.MUST);
        }

        ScoreDoc latestDoc = null;
        while (true) {
            ScoreDoc[] found = process.search(query, latestDoc, SEARCH_BLOCK,
                    new LuceneDriverRead.DocFilter() {
                        public boolean isDocAllowed(int docID) {
                            return true;
                        }
                    });

            for (ScoreDoc doc : found) {
                process(doc.doc);
            }
            if (found.length < SEARCH_BLOCK) {
                break;
            }
            latestDoc = found[found.length - 1];
        }

        return createResults();
    }

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
        StringBuilder str = new StringBuilder();
        for (int i = pos - params.wordsBefore; i <= pos + params.wordsAfter; i++) {
            str.append(words[i].value);
            str.append('\t');
        }
        str.setLength(str.length() - 1);
        String s = str.toString();
        Integer count = counts.get(s);
        if (count == null) {
            count = 0;
        }
        count++;
        counts.put(s, count);
    }

    private ClusterResults createResults() {
        ClusterResults r = new ClusterResults();
        r.rows = new ClusterResults.Row[counts.size()];
        int i = 0;
        for (Map.Entry<String, Integer> en : counts.entrySet()) {
            r.rows[i] = new ClusterResults.Row();
            r.rows[i].words = en.getKey().split("\t");
            r.rows[i].count = en.getValue();
            i++;
        }
        Arrays.sort(r.rows, new Comparator<ClusterResults.Row>() {
            @Override
            public int compare(ClusterResults.Row r1, ClusterResults.Row r2) {
                return r2.count - r1.count;
            }
        });
        return r;
    }
}
