package org.alex73.korpus.server.engine;

import org.apache.lucene.document.Field;
import org.apache.lucene.document.FieldType;
import org.apache.lucene.document.IntRange;
import org.apache.lucene.index.DocValuesType;
import org.apache.lucene.index.IndexOptions;
import org.apache.lucene.index.IndexableFieldType;

public class LuceneFields {

    public static final FieldType TYPE_STORED_NOTINDEXED = new FieldType();
    static {
        TYPE_STORED_NOTINDEXED.setTokenized(false);
        TYPE_STORED_NOTINDEXED.setOmitNorms(true);
        TYPE_STORED_NOTINDEXED.setIndexOptions(IndexOptions.NONE);
        TYPE_STORED_NOTINDEXED.setStored(true);
        TYPE_STORED_NOTINDEXED.freeze();
    }
    public static final FieldType TYPE_STORED_NOTINDEXED_INT = new FieldType();
    static {
        TYPE_STORED_NOTINDEXED_INT.setTokenized(false);
        TYPE_STORED_NOTINDEXED_INT.setOmitNorms(true);
        TYPE_STORED_NOTINDEXED_INT.setIndexOptions(IndexOptions.NONE);
        TYPE_STORED_NOTINDEXED_INT.setDocValuesType(DocValuesType.NUMERIC);
        TYPE_STORED_NOTINDEXED_INT.setStored(true);
        TYPE_STORED_NOTINDEXED_INT.freeze();
    }
    public static final FieldType TYPE_STORED_INDEXED_INT = new FieldType();
    static {
        TYPE_STORED_INDEXED_INT.setTokenized(false);
        TYPE_STORED_INDEXED_INT.setOmitNorms(true);
        TYPE_STORED_INDEXED_INT.setIndexOptions(IndexOptions.DOCS);
        TYPE_STORED_INDEXED_INT.setDocValuesType(DocValuesType.NUMERIC);
        TYPE_STORED_INDEXED_INT.setStored(true);
        TYPE_STORED_INDEXED_INT.freeze();
    }
    public static final FieldType TYPE_NOTSTORED_INDEXED = new FieldType();
    static {
        TYPE_NOTSTORED_INDEXED.setTokenized(true);
        TYPE_NOTSTORED_INDEXED.setOmitNorms(true);
        TYPE_NOTSTORED_INDEXED.setIndexOptions(IndexOptions.DOCS);
        TYPE_NOTSTORED_INDEXED.setStored(false);
        TYPE_NOTSTORED_INDEXED.freeze();
    }

    public Field fieldSentenceTextSubcorpus;
    // public FieldInt fieldSentenceTextIDOrder;
    public Field fieldSentenceTextStyleGenre;
    public Field fieldSentenceTextAuthor;
    public IntRange fieldSentenceTextCreationYear;
    public IntRange fieldSentenceTextPublishedYear;
    //public Field fieldSentenceTextInfo;

    public Field fieldSentenceValues;
    public Field fieldSentenceDBGrammarTags;
    public Field fieldSentenceLemmas;
    public Field fieldSentencePBinary;

    public FieldInt fieldTextID;

    public String[] authors;
    public String title;
    public String[] translators;
    public String langOrig;
    public String[] styleGenres;
    public String edition;
    public String writtenTime, publicationTime;

    public LuceneFields() {
        // words fields
        fieldSentenceValues = new Field("value", "", TYPE_NOTSTORED_INDEXED);
        fieldSentenceDBGrammarTags = new Field("dbGrammarTag", "", TYPE_NOTSTORED_INDEXED);
        fieldSentenceLemmas = new Field("lemma", "", TYPE_NOTSTORED_INDEXED);
        fieldSentencePBinary = new Field("pbinary", new byte[0], TYPE_STORED_NOTINDEXED);

        // korpus text fields for filtering
        fieldSentenceTextSubcorpus = new Field("textSubcorpus", "", TYPE_NOTSTORED_INDEXED);
        fieldSentenceTextStyleGenre = new Field("textStyleGenre", "", TYPE_NOTSTORED_INDEXED);
        fieldSentenceTextAuthor = new Field("textAuthor", "", TYPE_NOTSTORED_INDEXED);
        fieldSentenceTextCreationYear = new IntRange("creationYear", new int[] { 0 }, new int[] { 0 });
        fieldSentenceTextPublishedYear = new IntRange("publishedYear", new int[] { 0 }, new int[] { 0 });
//        fieldSentenceTextInfo = new Field("textInfo", new byte[0], TYPE_STORED_NOTINDEXED);

        fieldTextID = new FieldInt("textId", TYPE_STORED_INDEXED_INT);
    }

    public static class FieldInt extends Field {
        public FieldInt(String name, IndexableFieldType type) {
            super(name, type);
            fieldsData = 0;
        }
    }
}
