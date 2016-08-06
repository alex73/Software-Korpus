package org.alex73.korpus.server.engine;

import java.nio.file.Paths;

import org.alex73.korpus.base.BelarusianTags;
import org.alex73.korpus.base.DBTagsGroups;
import org.alex73.korpus.base.TextInfo;
import org.alex73.korpus.text.xml.P;
import org.alex73.korpus.text.xml.Se;
import org.alex73.korpus.text.xml.W;
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
import org.apache.lucene.store.NIOFSDirectory;

public class LuceneDriverWrite extends LuceneFields {
    protected final Logger LOGGER = LogManager.getLogger(LuceneDriverWrite.class);

    protected Directory dir;

    protected IndexWriter indexWriter;

    protected Document docText;
    protected Document docSentence;

    protected int textId;
    protected TextInfo currentTextInfo;
    protected String currentVolume;
    protected String currentUrl;

    public LuceneDriverWrite(String rootDir) throws Exception {
        IndexWriterConfig config = new IndexWriterConfig(new WhitespaceAnalyzer());
        config.setOpenMode(OpenMode.CREATE);
        config.setRAMBufferSizeMB(256);

        dir = new NIOFSDirectory(Paths.get(rootDir));
        indexWriter = new IndexWriter(dir, config);

        docSentence = new Document();
        docSentence.add(fieldSentenceValues);
        docSentence.add(fieldSentenceDBGrammarTags);
        docSentence.add(fieldSentenceLemmas);
        docSentence.add(fieldSentencePBinary);

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

    public synchronized void setTextInfo(TextInfo info) throws Exception {
        textId++;
        currentTextInfo = info;

        fieldTextID.setIntValue(textId);
        fieldTextAuthors.setStringValue(merge(info.authors, ";"));
        fieldTextTitle.setStringValue(info.title);
        fieldTextYearWritten.setIntValue(info.writtenYear != null ? info.writtenYear : 0);
        fieldTextYearPublished.setIntValue(info.publishedYear != null ? info.publishedYear : 0);
        indexWriter.addDocument(docText);
    }

    public synchronized void setOtherInfo(String volume, String textURL) throws Exception {
        currentVolume = volume;
        currentUrl = textURL;
    }

    /**
     * Add sentence to database. Sentence linked to previously added text.
     * 
     * @return words count
     */
    public int addSentence(P paragraph, byte[] xml) throws Exception {
        StringBuilder values = new StringBuilder(8192);
        StringBuilder dbGrammarTags = new StringBuilder(8192);
        StringBuilder lemmas = new StringBuilder(8192);

        int wordsCount = 0; // TODO: check performance
        for (int i = 0; i < paragraph.getSe().size(); i++) {
            Se op = paragraph.getSe().get(i);
            for (int j = 0; j < op.getWOrSOrZ().size(); j++) {
                Object o = op.getWOrSOrZ().get(j);
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

        synchronized (this) {
            if (currentVolume != null && currentUrl != null) {
                fieldSentenceTextVolume.setStringValue(currentVolume);
                fieldSentenceTextURL.setStringValue(currentUrl);
            } else {
                fieldSentenceTextID.setIntValue(textId);
            }

            fieldSentenceValues.setStringValue(values.toString());
            fieldSentenceDBGrammarTags.setStringValue(dbGrammarTags.toString());
            fieldSentenceLemmas.setStringValue(lemmas.toString());
            fieldSentencePBinary.setBytesValue(xml);

            if (currentTextInfo != null) {
                fieldSentenceTextStyleGenre.setTokenStream(new StringArrayTokenStream(
                        currentTextInfo.styleGenres));
                fieldSentenceTextWrittenYear.setIntValue(nvl(currentTextInfo.writtenYear));
                fieldSentenceTextPublishedYear.setIntValue(nvl(currentTextInfo.publishedYear));
                fieldSentenceTextAuthor.setTokenStream(new StringArrayTokenStream(currentTextInfo.authors));
            }

            indexWriter.addDocument(docSentence);
        }

        return wordsCount;
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

    private int nvl(Integer n) {
        return n != null ? n : 0;
    }
}
