package org.alex73.korpus.server.engine;

import org.alex73.korpus.base.BelarusianTags;
import org.alex73.korpus.base.DBTagsGroups;
import org.alex73.korpus.base.TextInfo;
import org.alex73.korpus.utils.WordNormalizer;
import org.apache.commons.lang.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.lucene.analysis.core.WhitespaceAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.IndexWriterConfig.OpenMode;
import org.apache.lucene.store.Directory;

import alex73.corpus.paradigm.P;
import alex73.corpus.paradigm.S;
import alex73.corpus.paradigm.W;

public class LuceneDriverWrite extends LuceneFields {
    protected final Logger LOGGER = LogManager.getLogger(LuceneDriverWrite.class);

    protected Directory dir;

    protected IndexWriter indexWriter;

    protected Document docText;
    protected Document docSentence;

    public LuceneDriverWrite(String rootDir) throws Exception {
        IndexWriterConfig config = new IndexWriterConfig(new WhitespaceAnalyzer());
        config.setOpenMode(OpenMode.CREATE);
        config.setRAMBufferSizeMB(256);
        indexWriter = new IndexWriter(dir, config);

        docSentence = new Document();
        docSentence.add(fieldSentenceValues);
        docSentence.add(fieldSentenceDBGrammarTags);
        docSentence.add(fieldSentenceLemmas);
        docSentence.add(fieldSentenceXML);

        docSentence.add(fieldSentenceTextID);
        docSentence.add(fieldSentenceTextStyleGenre);
        docSentence.add(fieldSentenceTextAuthor);
        docSentence.add(fieldSentenceTextWrittenYear);
        docSentence.add(fieldSentenceTextPublishedYear);

        docSentence.add(fieldSentenceTextVolume);
        docSentence.add(fieldSentenceTextURL);

        docText = new Document();
        docText.add(fieldTextID);
        docText.add(fieldTextAuthors);
        docText.add(fieldTextTitle);
        docText.add(fieldTextYearWritten);
        docText.add(fieldTextYearPublished);
    }

    public void shutdown() throws Exception {

        indexWriter.forceMerge(1);
        indexWriter.commit();
        indexWriter.close();
        dir.close();
    }

    /**
     * Add sentence to database. Sentence linked to previously added text.
     * 
     * @return words count
     */
    public int addSentence(P paragraph, byte[] xml, int textId, TextInfo info, String volume, String textURL)
            throws Exception {

        StringBuilder values = new StringBuilder();
        StringBuilder dbGrammarTags = new StringBuilder();
        StringBuilder lemmas = new StringBuilder();

        int wordsCount = 0;
        for (Object op : paragraph.getSOrTag()) {
            if (op instanceof S) {
                for (Object o : ((S) op).getWOrTag()) {
                    if (!(o instanceof W)) {
                        continue;
                    }
                    W w = (W) o;
                    wordsCount++;
                    if (w.getValue() != null) {
                        String wc = WordNormalizer.normalize(w.getValue());
                        values.append(wc).append(' ');
                    }
                    if (StringUtils.isNotEmpty(w.getCat())) {
                        for (String t : w.getCat().split("_")) {
                            if (!BelarusianTags.getInstance().isValid(t, null)) {
                                // TODO throw new Exception("Няправільны тэг: " + t);
                            } else {
                                dbGrammarTags.append(DBTagsGroups.getDBTagString(t)).append(' ');
                            }
                        }
                    }
                    if (w.getLemma() != null) {
                        lemmas.append(w.getLemma().replace('_', ' ')).append(' ');
                    }
                }
            }
        }

        // fieldID.setIntValue(id);
        fieldSentenceTextVolume.setStringValue(volume);
        fieldSentenceTextURL.setStringValue(textURL);

        fieldSentenceTextID.setIntValue(textId);
        fieldSentenceValues.setStringValue(values.toString());
        fieldSentenceDBGrammarTags.setStringValue(dbGrammarTags.toString());
        fieldSentenceLemmas.setStringValue(lemmas.toString());
        fieldSentenceXML.setBytesValue(xml);
        fieldSentenceTextStyleGenre.setTokenStream(new StringArrayTokenStream(info.styleGenres));
        fieldSentenceTextWrittenYear.setIntValue(nvl(info.writtenYear));
        fieldSentenceTextPublishedYear.setIntValue(nvl(info.publishedYear));
        fieldSentenceTextAuthor.setTokenStream(new StringArrayTokenStream(info.authors));

        indexWriter.addDocument(docSentence);

        return wordsCount;
    }

    /**
     * Add text to database.
     */
    public void addText(int textId, TextInfo info) throws Exception {
        fieldTextID.setIntValue(textId);
        fieldTextAuthors.setStringValue(merge(info.authors, ";"));
        fieldTextTitle.setStringValue(info.title);
        fieldTextYearWritten.setIntValue(info.writtenYear != null ? info.writtenYear : 0);
        fieldTextYearPublished.setIntValue(info.publishedYear != null ? info.publishedYear : 0);
        indexWriter.addDocument(docText);
    }

    private String merge(String[] strs, String sep) {
        StringBuilder out = new StringBuilder();
        for (String s : strs) {
            if (out.length() > 0) {
                out.append(sep);
            }
            out.append(s);
        }
        return out.toString();
    }

    private String nvl(String n) {
        return n != null ? n : "";
    }

    private int nvl(Integer n) {
        return n != null ? n : 0;
    }
}
