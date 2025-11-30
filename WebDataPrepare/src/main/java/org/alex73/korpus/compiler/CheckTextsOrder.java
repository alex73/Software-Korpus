package org.alex73.korpus.compiler;

import java.nio.file.Paths;

import org.apache.lucene.document.Document;
import org.apache.lucene.document.StoredField;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.store.NIOFSDirectory;

public class CheckTextsOrder {

    public static void main(String[] args) throws Exception {
        NIOFSDirectory dir = new NIOFSDirectory(Paths.get("."));
        DirectoryReader directoryReader = DirectoryReader.open(dir);
        long prevId = -1;
        for (int i = 0; i < directoryReader.maxDoc(); i++) {
            Document doc = directoryReader.document(i);
            StoredField field = (StoredField) doc.getField("textId");
            long id = field.numericValue().longValue();
            if (i % 1000 == 0)
                System.out.println(i + " " + id);
            if (id < prevId) {
                throw new Exception();
            }
            prevId = id;
        }
    }
}
