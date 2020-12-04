package org.alex73.korpus.server.engine;

import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import org.alex73.korpus.base.BelarusianTags;
import org.alex73.korpus.base.BelarusianWordNormalizer;
import org.alex73.korpus.base.DBTagsGroups;
import org.alex73.korpus.base.TextInfo;
import org.alex73.korpus.text.xml.P;
import org.alex73.korpus.text.xml.W;
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
    protected String currentName;
    protected String currentUrl;
    protected String currentDetails;

    public LuceneDriverWrite(String rootDir) throws Exception {
        IndexWriterConfig config = new IndexWriterConfig(new WhitespaceAnalyzer());
        config.setOpenMode(OpenMode.CREATE);
        config.setRAMBufferSizeMB(512);

        dir = new NIOFSDirectory(Paths.get(rootDir));
        indexWriter = new IndexWriter(dir, config);

        docSentence = new Document();
        docSentence.add(fieldSentenceValues);
        docSentence.add(fieldSentenceDBGrammarTags);
        docSentence.add(fieldSentenceLemmas);
        docSentence.add(fieldSentencePBinary);

        docSentence.add(fieldSentenceTextSubcorpus);
        docSentence.add(fieldSentenceTextID);
        docSentence.add(fieldSentenceTextStyleGenre);
        docSentence.add(fieldSentenceTextAuthor);
        docSentence.add(fieldSentenceTextWrittenYear);
        docSentence.add(fieldSentenceTextPublishedYear);
        docSentence.add(fieldSentenceTextDate);

        docText = new Document();
        docText.add(fieldTextID);
        docText.add(fieldTextURL);
        docText.add(fieldTextSubcorpus);
        docText.add(fieldTextAuthors);
        docText.add(fieldTextTitle);
        docText.add(fieldTextTranslators);
        docText.add(fieldTextLangOrig);
        docText.add(fieldTextStyleGenre);
        docText.add(fieldTextEdition);
        docText.add(fieldTextWrittenTime);
        docText.add(fieldTextPublicationTime);
    }

    public synchronized void shutdown() throws Exception {
        indexWriter.forceMerge(1);
        indexWriter.commit();
        indexWriter.close();
        dir.close();
    }

    public synchronized void setTextInfo(TextInfo info) throws Exception {
        textId++;
        currentTextInfo = info;
        currentVolume = null;
        currentUrl = null;

        fieldTextID.setIntValue(textId);
        fieldTextURL.setStringValue(nvl(info.url));
        fieldTextSubcorpus.setStringValue(info.subcorpus);
        fieldTextAuthors.setStringValue(merge(info.authors, ";"));
        fieldTextTitle.setStringValue(nvl(info.title));
        fieldTextTranslators.setStringValue(merge(info.translators, ";"));
        fieldTextLangOrig.setStringValue(nvl(info.langOrig));
        fieldTextStyleGenre.setStringValue(merge(info.styleGenres, ";"));
        fieldTextEdition.setStringValue(nvl(info.edition));
        fieldTextWrittenTime.setStringValue(nvl(info.writtenTime));
        fieldTextPublicationTime.setStringValue(nvl(info.publicationTime));

        indexWriter.addDocument(docText);
    }

    /**
     * Add sentence to database. Sentence linked to previously added text.
     * 
     * @return words count
     */
    public void addSentence(P paragraph, byte[] xml) throws Exception {
        StringBuilder values = new StringBuilder(8192);
        StringBuilder dbGrammarTags = new StringBuilder(8192);
        StringBuilder lemmas = new StringBuilder(8192);

        paragraph.getSe().forEach(op -> {
            op.getWOrSOrZ().forEach(o -> {
                if (o instanceof W) {
                    W w = (W) o;
                    if (w.getValue() != null) {
                        String wc = BelarusianWordNormalizer.normalize(w.getValue());
                        values.append(wc).append(' ');
                    }
                    if (StringUtils.isNotEmpty(w.getCat())) {
                        for (String t : w.getCat().split("_")) {
                            if (!BelarusianTags.getInstance().isValid(t, null)) {
                                // TODO throw new Exception("Няправільны тэг:
                            } else {
                                dbGrammarTags.append(DBTagsGroups.getDBTagString(t)).append(' ');
                            }
                        }
                    }
                    if (w.getLemma() != null) {
                        lemmas.append(w.getLemma().replace('_', ' ')).append(' ');
                    }
                }
            });
        });

        synchronized (this) {
            fieldSentenceTextSubcorpus.setStringValue(currentTextInfo.subcorpus);
            fieldSentenceTextID.setIntValue(textId);

            fieldSentenceValues.setStringValue(values.toString());
            fieldSentenceDBGrammarTags.setStringValue(dbGrammarTags.toString());
            fieldSentenceLemmas.setStringValue(lemmas.toString());
            fieldSentencePBinary.setBytesValue(xml);

            fieldSentenceTextStyleGenre.setTokenStream(new StringArrayTokenStream(currentTextInfo.styleGenres));
            //TODO fieldSentenceTextWrittenYear.setIntValue(nvl(currentTextInfo.writtenYear));
            //TODO fieldSentenceTextPublishedYear.setIntValue(nvl(currentTextInfo.publishedYear));
            fieldSentenceTextAuthor.setTokenStream(new StringArrayTokenStream(currentTextInfo.authors));
            fieldSentenceTextDate.setLongValue(currentTextInfo.date);
log.add("lemmas: "+lemmas);
log.add("values: "+values);
log.add("gramma: "+dbGrammarTags);
log.add("");
            indexWriter.addDocument(docSentence);
        }
    }
public static List<String> log=new ArrayList<>();

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

    private String nvl(String n) {
        return n != null ? n : "";
    }
}
