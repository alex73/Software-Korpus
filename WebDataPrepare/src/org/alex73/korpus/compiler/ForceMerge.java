package org.alex73.korpus.compiler;

import java.nio.file.Paths;

import org.alex73.korpus.server.engine.LuceneFields;
import org.apache.lucene.index.ConcurrentMergeScheduler;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.IndexWriterConfig.OpenMode;
import org.apache.lucene.search.Sort;
import org.apache.lucene.search.SortField;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.NIOFSDirectory;

public class ForceMerge {

    public static void main(String[] args) throws Exception {
        LuceneFields fields = new LuceneFields();
        IndexWriterConfig config = new IndexWriterConfig();
        config.setCommitOnClose(true);
        config.setOpenMode(OpenMode.CREATE);
        config.setRAMBufferSizeMB(8192);
        config.setIndexSort(new Sort(new SortField(fields.fieldTextID.name(), SortField.Type.INT)));
        config.setMergeScheduler(new ConcurrentMergeScheduler());

        Directory[] partDirs = new Directory[8];
        try (Directory dir = new NIOFSDirectory(Paths.get("/home/alex/Korpus-pouny-cache"))) {
            try (IndexWriter indexWriter = new IndexWriter(dir, config)) {
                for (int i = 0; i < 8; i++) {
                    partDirs[i] = new NIOFSDirectory(Paths.get("/home/alex/Korpus-pouny-cache/ProcessLuceneWriter-" + (i + 1)));
                }
                indexWriter.addIndexes(partDirs);

                indexWriter.forceMerge(1, true);
            }
        }
        for (Directory d : partDirs) {
            d.close();
        }
    }
}
