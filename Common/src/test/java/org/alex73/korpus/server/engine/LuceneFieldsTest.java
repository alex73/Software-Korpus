package org.alex73.korpus.server.engine;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class LuceneFieldsTest {

    @Test
    public void testLuceneFieldsWithoutVectors() {
        LuceneFields fields = new LuceneFields(new String[]{"be"});
        Assertions.assertNotNull(fields.byLang.get("be"));
        Assertions.assertNull(fields.byLang.get("be").fieldVector);

        LuceneFields fieldsExplicit = new LuceneFields(new String[]{"be"}, false);
        Assertions.assertNull(fieldsExplicit.byLang.get("be").fieldVector);
    }

    @Test
    public void testLuceneFieldsWithVectors() {
        LuceneFields fields = new LuceneFields(new String[]{"be"}, true);
        Assertions.assertNotNull(fields.byLang.get("be"));
        Assertions.assertNotNull(fields.byLang.get("be").fieldVector);
        Assertions.assertEquals("be_vector", fields.byLang.get("be").fieldVector.name());
    }
}
