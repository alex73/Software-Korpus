package org.alex73.korpus.server.data;

import java.io.Serializable;

public class LatestMark implements Serializable {
    public float score;
    public int doc;
    public int shardIndex;
}