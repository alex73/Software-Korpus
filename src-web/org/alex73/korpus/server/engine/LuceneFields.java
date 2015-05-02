package org.alex73.korpus.server.engine;

import org.apache.lucene.document.Field;
import org.apache.lucene.document.FieldType;
import org.apache.lucene.index.IndexOptions;

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
        TYPE_STORED_NOTINDEXED_INT.setNumericType(FieldType.NumericType.INT);
        TYPE_STORED_NOTINDEXED_INT.setStored(true);
        TYPE_STORED_NOTINDEXED_INT.freeze();
    }
    public static final FieldType TYPE_NOTSTORED_INDEXED_INT = new FieldType();
    static {
        TYPE_NOTSTORED_INDEXED_INT.setTokenized(false);
        TYPE_NOTSTORED_INDEXED_INT.setOmitNorms(true);
        TYPE_NOTSTORED_INDEXED_INT.setIndexOptions(IndexOptions.DOCS);
        TYPE_NOTSTORED_INDEXED_INT.setNumericType(FieldType.NumericType.INT);
        TYPE_NOTSTORED_INDEXED_INT.setStored(false);
        TYPE_NOTSTORED_INDEXED_INT.freeze();
    }
    public static final FieldType TYPE_NOTSTORED_INDEXED = new FieldType();
    static {
        TYPE_NOTSTORED_INDEXED.setTokenized(true);
        TYPE_NOTSTORED_INDEXED.setOmitNorms(true);
        TYPE_NOTSTORED_INDEXED.setIndexOptions(IndexOptions.DOCS);
        TYPE_NOTSTORED_INDEXED.setStored(false);
        TYPE_NOTSTORED_INDEXED.freeze();
    }

    public Field fieldSentenceTextID;
    public Field fieldSentenceTextStyleGenre;
    public Field fieldSentenceTextAuthor;
    public Field fieldSentenceTextWrittenYear;
    public Field fieldSentenceTextPublishedYear;

    public Field fieldSentenceValues;
    public Field fieldSentenceDBGrammarTags;
    public Field fieldSentenceLemmas;
    public Field fieldSentenceXML;

    public Field fieldSentenceTextURL;
    public Field fieldSentenceTextVolume;

    public Field fieldTextID;
    public Field fieldTextAuthors;
    public Field fieldTextTitle;
    public Field fieldTextYearWritten;
    public Field fieldTextYearPublished;
}
