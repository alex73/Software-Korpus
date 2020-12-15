package org.alex73.korpus.compiler;

import java.nio.file.Paths;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.alex73.korpus.base.BelarusianTags;
import org.alex73.korpus.base.BelarusianWordNormalizer;
import org.alex73.korpus.base.DBTagsGroups;
import org.alex73.korpus.base.TextInfo;
import org.alex73.korpus.server.engine.LuceneFields;
import org.alex73.korpus.server.engine.StringArrayTokenStream;
import org.alex73.korpus.server.text.BinaryParagraphWriter;
import org.alex73.korpus.text.xml.P;
import org.alex73.korpus.text.xml.W;
import org.alex73.korpus.utils.KorpusDateTime;
import org.apache.commons.lang.StringUtils;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.IntRange;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.IndexWriterConfig.OpenMode;
import org.apache.lucene.search.Sort;
import org.apache.lucene.search.SortField;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.NIOFSDirectory;

public class LuceneDriverWrite extends LuceneFields {
    static final String[] STRING_ARRAY = new String[0];

    protected Directory dir;

    protected IndexWriter indexWriter;

    protected Document docSentence;

    public LuceneDriverWrite(String rootDir) throws Exception {
        IndexWriterConfig config = new IndexWriterConfig();
        config.setOpenMode(OpenMode.CREATE);
        config.setRAMBufferSizeMB(1024);
        config.setIndexSort(new Sort(new SortField(fieldTextID.name(), SortField.Type.INT)));

        dir = new NIOFSDirectory(Paths.get(rootDir));
        indexWriter = new IndexWriter(dir, config);

        docSentence = new Document();
        docSentence.add(fieldSentenceValues);
        docSentence.add(fieldSentenceDBGrammarTags);
        docSentence.add(fieldSentenceLemmas);
        docSentence.add(fieldSentencePBinary);

        docSentence.add(fieldSentenceTextSubcorpus);
        // docSentence.add(fieldSentenceTextIDOrder);
        docSentence.add(fieldSentenceTextStyleGenre);
        docSentence.add(fieldSentenceTextAuthor);
        docSentence.add(fieldSentenceTextCreationYear);
        docSentence.add(fieldSentenceTextPublishedYear);

        docSentence.add(fieldTextID);
    }

    public synchronized void shutdown() throws Exception {
        indexWriter.forceMerge(1, true);
        indexWriter.commit();
        indexWriter.close();
        dir.close();
    }

    /**
     * Add sentence to database. Sentence linked to previously added text.
     * 
     * @return words count
     */
    protected synchronized void addSentence(TextInfo textInfo, String[] values, String[] dbGrammarTags, String[] lemmas,
            byte[] xml) throws Exception {
        fieldSentenceTextSubcorpus.setStringValue(textInfo.subcorpus);
        fieldSentenceTextStyleGenre.setTokenStream(new StringArrayTokenStream(textInfo.styleGenres));
        setYearsRange(textInfo.creationTime, fieldSentenceTextCreationYear);
        setYearsRange(textInfo.publicationTime, fieldSentenceTextPublishedYear);
        fieldSentenceTextAuthor.setTokenStream(new StringArrayTokenStream(textInfo.authors));

        fieldSentenceValues.setTokenStream(new StringArrayTokenStream(values));
        fieldSentenceDBGrammarTags.setTokenStream(new StringArrayTokenStream(dbGrammarTags));
        fieldSentenceLemmas.setTokenStream(new StringArrayTokenStream(lemmas));
        fieldSentencePBinary.setBytesValue(xml);

        fieldTextID.setIntValue(PrepareCache3.textPositionsBySourceFile.get(textInfo.sourceFilePath));

        indexWriter.addDocument(docSentence);
    }

    public void addSentences(TextInfo textInfo, List<P> content) throws Exception {
        BinaryParagraphWriter pwr = new BinaryParagraphWriter();
        Set<String> values = new HashSet<>();
        Set<String> dbGrammarTags = new HashSet<>();
        Set<String> lemmas = new HashSet<>();
        for (P p : content) {
            values.clear();
            dbGrammarTags.clear();
            lemmas.clear();
            p.getSe().forEach(op -> {
                op.getWOrSOrZ().forEach(o -> {
                    if (o instanceof W) {
                        W w = (W) o;
                        if (w.getValue() != null) {
                            String wc = BelarusianWordNormalizer.normalizePreserveCase(w.getValue());
                            values.add(wc);
                        }
                        if (StringUtils.isNotEmpty(w.getCat())) {
                            for (String t : w.getCat().split("_")) {
                                if (t.isEmpty()) {
                                    continue;
                                }
                                if (!BelarusianTags.getInstance().isValid(t, null)) {
                                    throw new RuntimeException("Няправільны тэг: " + t);
                                } else {
                                    dbGrammarTags.add(DBTagsGroups.getDBTagString(t));
                                }
                            }
                        }
                        if (w.getLemma() != null) {
                            for (String v : w.getLemma().split("_")) {
                                if (v.isEmpty()) {
                                    continue;
                                }
                                lemmas.add(v);
                            }
                        }
                    }
                });
            });
            byte[] pxml = pwr.write(p);
            addSentence(textInfo, values.toArray(STRING_ARRAY), dbGrammarTags.toArray(STRING_ARRAY),
                    lemmas.toArray(STRING_ARRAY), pxml);
        }
    }

    private void setYearsRange(String date, IntRange rangeField) {
        if (date == null) {
            rangeField.setRangeValues(new int[] { Integer.MIN_VALUE }, new int[] { Integer.MIN_VALUE });
        } else {
            KorpusDateTime dt = new KorpusDateTime(date);
            rangeField.setRangeValues(new int[] { dt.getEarliestYear() }, new int[] { dt.getLatestYear() });
        }
    }
}
