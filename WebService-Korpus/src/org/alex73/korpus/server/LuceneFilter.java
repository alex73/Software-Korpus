package org.alex73.korpus.server;

import java.util.List;

import org.alex73.korpus.languages.ILanguage;
import org.alex73.korpus.languages.LanguageFactory;
import org.alex73.korpus.server.data.LatestMark;
import org.alex73.korpus.server.data.StandardTextRequest;
import org.alex73.korpus.server.data.WordRequest;
import org.alex73.korpus.server.engine.LuceneDriverRead;
import org.alex73.korpus.server.engine.LuceneDriverRead.DocFilter;
import org.alex73.korpus.server.engine.LuceneFields;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.IntRange;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.RegexpQuery;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.WildcardQuery;

public class LuceneFilter {
    LuceneDriverRead lucene;

    public LuceneFilter(String dir, String[] languages) throws Exception {
        lucene = new LuceneDriverRead(dir, languages);
    }

    public void close() throws Exception {
        lucene.shutdown();
    }

    public void addKorpusTextFilter(String textLanguage, BooleanQuery.Builder query, StandardTextRequest filter) {
        LuceneFields.LuceneFieldsLang lf = lucene.byLang.get(textLanguage);
        if (lf == null) {
            throw new RuntimeException("Corpus doesn't have '" + textLanguage + "' language");
        }
        // subcorpus
        if (filter.subcorpuses != null) {
            BooleanQuery.Builder q = new BooleanQuery.Builder();
            for (String a : filter.subcorpuses) {
                q.add(new TermQuery(new Term(lucene.fieldTextSubcorpus.name(), a)), BooleanClause.Occur.SHOULD);
            }
            q.setMinimumNumberShouldMatch(1);
            query.add(q.build(), BooleanClause.Occur.MUST);
        }
        // author
        if (filter.authors != null) {
            BooleanQuery.Builder q = new BooleanQuery.Builder();
            for (String a : filter.authors) {
                q.add(new TermQuery(new Term(lf.fieldTextAuthor.name(), a)), BooleanClause.Occur.SHOULD);
            }
            q.setMinimumNumberShouldMatch(1);
            query.add(q.build(), BooleanClause.Occur.MUST);
        }
        // source
        if (filter.sources != null) {
            BooleanQuery.Builder q = new BooleanQuery.Builder();
            for (String a : filter.sources) {
                q.add(new TermQuery(new Term(lf.fieldTextSource.name(), a)), BooleanClause.Occur.SHOULD);
            }
            q.setMinimumNumberShouldMatch(1);
            query.add(q.build(), BooleanClause.Occur.MUST);
        }
        // style/genre
        if (filter.stylegenres != null) {
            BooleanQuery.Builder q = new BooleanQuery.Builder();
            for (String sg : filter.stylegenres) {
                q.add(new TermQuery(new Term(lucene.fieldTextStyleGenre.name(), sg)), BooleanClause.Occur.SHOULD);
            }
            q.setMinimumNumberShouldMatch(1);
            query.add(q.build(), BooleanClause.Occur.MUST);
        }
        // written year
        if (filter.yearWrittenFrom != null || filter.yearWrittenTo != null) {
            int yFrom = filter.yearWrittenFrom != null ? filter.yearWrittenFrom : 1;
            int yTo = filter.yearWrittenTo != null ? filter.yearWrittenTo : 9999;
            Query q = IntRange.newIntersectsQuery(lf.fieldTextCreationYear.name(), new int[] { yFrom }, new int[] { yTo });
            query.add(q, BooleanClause.Occur.MUST);
        }
        // published year
        if (filter.yearPublishedFrom != null || filter.yearPublishedTo != null) {
            int yFrom = filter.yearPublishedFrom != null ? filter.yearPublishedFrom : 1;
            int yTo = filter.yearPublishedTo != null ? filter.yearPublishedTo : 9999;
            Query q = IntRange.newIntersectsQuery(lf.fieldTextPublishedYear.name(), new int[] { yFrom }, new int[] { yTo });
            query.add(q, BooleanClause.Occur.MUST);
        }
    }

    public void addWordFilter(String textLanguage, BooleanQuery.Builder query, WordRequest w) {
        LuceneFields.LuceneFieldsLang lf = lucene.byLang.get(textLanguage);
        if (lf == null) {
            throw new RuntimeException("Corpus doesn't have " + textLanguage + " language");
        }
        ILanguage.INormalizer normalizer = LanguageFactory.get(textLanguage).getNormalizer();
        if (w.word != null) {
            String wn = normalizer.superNormalized(w.word);
            Term wt = new Term(lf.fieldWordWriteVariant.name(), wn);
            Query wq = WordsDetailsChecks.needWildcardRegexp(wn) ? new WildcardQuery(wt) : new TermQuery(wt);
            query.add(wq, BooleanClause.Occur.MUST);
        }
        if (w.forms != null) {
            BooleanQuery.Builder qforms = new BooleanQuery.Builder();
            for (String form : w.forms) {
                Term t = new Term(lf.fieldWordWriteVariant.name(), normalizer.superNormalized(form));
                qforms.add(new TermQuery(t), BooleanClause.Occur.SHOULD);
            }
            query.add(qforms.build(), BooleanClause.Occur.MUST);
        }
        if (w.grammar != null) {
            Term t = new Term(lf.fieldTagsWriteVariant.name(), w.grammar);
            Query wq = new RegexpQuery(t); // TODO change to WildcardQuery
            query.add(wq, BooleanClause.Occur.MUST);
        }
    }

    public void search(Query query, int pageSize, DocFilter<Void> filter) throws Exception {
        lucene.search(query, pageSize, filter);
    }

    public <T> List<T> search(Query query, LatestMark latest, int maxResults, DocFilter<T> filter) throws Exception {
        return lucene.search(query, latest, maxResults, filter);
    }

    public Document getSentence(int docID) throws Exception {
        return lucene.getSentence(docID);
    }

    public byte[] getXML(Document doc) {
        return doc.getField(lucene.fieldSentencePBinary.name()).binaryValue().bytes;
    }

    public int getTextID(Document doc) {
        return doc.getField(lucene.fieldTextID.name()).numericValue().intValue();
    }
}
