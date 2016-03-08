package org.alex73.korpus.shared.dto;

import java.io.Serializable;

public class LatestMark implements Serializable {
    public float score;
    public int doc;
    public int shardIndex;
}