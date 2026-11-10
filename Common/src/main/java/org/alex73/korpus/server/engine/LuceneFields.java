package org.alex73.korpus.server.engine;

import java.util.Map;
import java.util.TreeMap;

import org.apache.lucene.document.Field;
import org.apache.lucene.document.FieldType;
import org.apache.lucene.document.IntRange;
import org.apache.lucene.document.KnnFloatVectorField;
import org.apache.lucene.index.DocValuesType;
import org.apache.lucene.index.IndexOptions;
import org.apache.lucene.index.IndexableFieldType;
import org.apache.lucene.index.VectorSimilarityFunction;

public class LuceneFields {

    public static final int VECTOR_DIMENSIONS = 1024;
    public static final VectorSimilarityFunction VECTOR_SIMILARITY = VectorSimilarityFunction.DOT_PRODUCT;

    public static String getVectorFieldName(String lang) {
        return lang + "_vector";
    }

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
    public static final FieldType TYPE_NOTSTORED_INDEXED_ATOMIC = new FieldType();
    static {
        TYPE_NOTSTORED_INDEXED_ATOMIC.setTokenized(false);
        TYPE_NOTSTORED_INDEXED_ATOMIC.setOmitNorms(true);
        TYPE_NOTSTORED_INDEXED_ATOMIC.setIndexOptions(IndexOptions.DOCS);
        TYPE_NOTSTORED_INDEXED_ATOMIC.setStored(false);
        TYPE_NOTSTORED_INDEXED_ATOMIC.freeze();
    }

    public static class LuceneFieldsLang {
        public Field fieldWordWriteVariant; // all supernormalized words from this paragraph
        public Field fieldTagsWriteVariant; // all grammar tags from this paragraph
        public Field fieldTextAuthor;
        public Field fieldTextSource;
        public IntRange fieldTextCreationYear;
        public IntRange fieldTextPublishedYear;
        public KnnFloatVectorField fieldVector;
    }

    public Field fieldTextSubcorpus;
    public Field fieldTextStyleGenre;

    public Map<String, LuceneFieldsLang> byLang = new TreeMap<>();
    public Field fieldSentencePBinary;

    public FieldInt fieldTextID;

    public LuceneFields(String[] allLanguages) {
        this(allLanguages, false);
    }

    public LuceneFields(String[] allLanguages, boolean withVectors) {
        StringArrayTokenStream emptyStream = new StringArrayTokenStream(new String[0]);
        for (int l = 0; l < allLanguages.length; l++) {
            String lang = allLanguages[l];
            // words fields
            LuceneFieldsLang lf = new LuceneFieldsLang();
            byLang.put(lang, lf);
            lf.fieldWordWriteVariant = new Field(lang + "_WordWriteVariant", emptyStream, TYPE_NOTSTORED_INDEXED);
            lf.fieldTagsWriteVariant = new Field(lang + "_TagsWriteVariant", emptyStream, TYPE_NOTSTORED_INDEXED);
            if (withVectors) {
                float[] initialVector = new float[VECTOR_DIMENSIONS];
                initialVector[0] = 1.0f;
                lf.fieldVector = new KnnFloatVectorField(getVectorFieldName(lang), initialVector, VECTOR_SIMILARITY);
            }
            if (l == 0) {
                // толькі для першай мовы
                lf.fieldTextAuthor = new Field("textAuthor", emptyStream, TYPE_NOTSTORED_INDEXED);
                lf.fieldTextSource = new Field("textSource", emptyStream, TYPE_NOTSTORED_INDEXED);
                lf.fieldTextCreationYear = new IntRange("creationYear", new int[] { 0 }, new int[] { 0 });
                lf.fieldTextPublishedYear = new IntRange("publishedYear", new int[] { 0 }, new int[] { 0 });
            }
        }
        fieldSentencePBinary = new Field("pbinary", new byte[0], TYPE_STORED_NOTINDEXED);

        // korpus text fields for filtering
        fieldTextSubcorpus = new Field("textSubcorpus", "", TYPE_NOTSTORED_INDEXED_ATOMIC);
        fieldTextStyleGenre = new Field("textStyleGenre", emptyStream, TYPE_NOTSTORED_INDEXED);

        fieldTextID = new FieldInt("textId", TYPE_STORED_INDEXED_INT);
    }

    public static class FieldInt extends Field {
        public FieldInt(String name, IndexableFieldType type) {
            super(name, type);
            fieldsData = 0;
        }
    }
}
