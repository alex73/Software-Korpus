package org.alex73.korpus.server.engine;

import org.alex73.korpus.base.TextInfo;
import org.mapdb.DB;
import org.mapdb.DBMaker;
import org.mapdb.Serializer;

import java.util.Set;
import java.util.concurrent.ConcurrentMap;

public class TextInfos {
    public final ConcurrentMap<Integer, TextInfo> textInfos;
    public final ConcurrentMap<String, Set<String>> authorsByLemmas;

    public TextInfos(String filePath) {
        DB readDb = DBMaker.fileDB(filePath)
                .readOnly()
                .fileMmapEnable()
                .make();

        textInfos = readDb
                .hashMap("textInfos", Serializer.INTEGER, Serializer.ELSA)
                .open();
        authorsByLemmas = readDb
                .hashMap("authorsByLemmas", Serializer.STRING, Serializer.ELSA)
                .open();
    }
}
