package org.alex73.korpus.shared.dto;

import java.io.Serializable;

public class ClusterResults implements Serializable {
    public Row[] rows;

    public static class Row implements Serializable {
        public String[] wordsBefore;
        public String word;
        public String[] wordsAfter;
        public int count;
    }
}