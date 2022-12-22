package org.alex73.korpus.server.data;

import java.io.Serializable;

@SuppressWarnings("serial")
public class LatestMark implements Serializable {
    public int doc;
    public float score;
    public Object[] fields;
    public int shardIndex;
}