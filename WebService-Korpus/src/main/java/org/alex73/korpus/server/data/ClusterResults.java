package org.alex73.korpus.server.data;

import java.io.Serializable;

public class ClusterResults implements Serializable {
    public String error;
    public Row[] rows;

    public static class Row implements Serializable {
        public String[] wordsBefore;
        public String word;
        public String[] wordsAfter;
        public int count;
    }
}
