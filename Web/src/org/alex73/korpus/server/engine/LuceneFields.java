package org.alex73.korpus.server.engine;

import org.apache.lucene.document.Field;
import org.apache.lucene.document.FieldType;
import org.apache.lucene.document.IntField;
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
    public Field fieldSentencePBinary;

    public Field fieldSentenceOtherVolume;
    public Field fieldSentenceOtherName;
    public Field fieldSentenceOtherURL;
    public Field fieldSentenceOtherDetails;

    public Field fieldTextID;
    public Field fieldTextAuthors;
    public Field fieldTextTitle;
    public Field fieldTextTranslators;
    public Field fieldTextLangOrig;
    public Field fieldTextStyleGenre;
    public Field fieldTextEdition;
    public Field fieldTextWrittenTime;
    public Field fieldTextPublicationTime;

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
        fieldSentenceTextID = new IntField("textId", 0, TYPE_STORED_NOTINDEXED_INT);
        fieldSentenceTextStyleGenre = new Field("textStyleGenre", "", TYPE_NOTSTORED_INDEXED);
        fieldSentenceTextAuthor = new Field("textAuthor", "", TYPE_NOTSTORED_INDEXED);
        fieldSentenceTextWrittenYear = new IntField("writtenYear", 0, TYPE_NOTSTORED_INDEXED_INT);
        fieldSentenceTextPublishedYear = new IntField("publishedYear", 0, TYPE_NOTSTORED_INDEXED_INT);

        // korpus text info fields
        fieldTextID = new IntField("id", 0, TYPE_NOTSTORED_INDEXED_INT);
        fieldTextAuthors = new Field("authors", "", TYPE_STORED_NOTINDEXED);
        fieldTextTitle = new Field("title", "", TYPE_STORED_NOTINDEXED);
        fieldTextTranslators = new Field("translators", "", TYPE_STORED_NOTINDEXED);
        fieldTextLangOrig = new Field("langOrig", "", TYPE_STORED_NOTINDEXED);
        fieldTextStyleGenre = new Field("styleGenre", "", TYPE_STORED_NOTINDEXED);
        fieldTextEdition = new Field("edition", "", TYPE_STORED_NOTINDEXED);
        fieldTextWrittenTime = new Field("writtenTime", "", TYPE_STORED_NOTINDEXED);
        fieldTextPublicationTime = new Field("publicationTime", "", TYPE_STORED_NOTINDEXED);

        // other text info fields
        fieldSentenceOtherVolume = new Field("otherVolume", "", TYPE_NOTSTORED_INDEXED);
        fieldSentenceOtherName = new Field("otherName", "", TYPE_STORED_NOTINDEXED);
        fieldSentenceOtherURL = new Field("otherURL", "", TYPE_STORED_NOTINDEXED);
        fieldSentenceOtherDetails = new Field("otherDetails", "", TYPE_STORED_NOTINDEXED);
    }
}
