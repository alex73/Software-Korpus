package org.alex73.korpus.server.data;

import java.io.Serializable;

@SuppressWarnings("serial")
public class LatestMark implements Serializable {
    public float score;
    public int doc;
    public int shardIndex;
    public Object sortValue;
}