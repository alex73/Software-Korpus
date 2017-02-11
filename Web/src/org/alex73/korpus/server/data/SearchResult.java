package org.alex73.korpus.server.data;

import java.io.Serializable;

public class SearchResult implements Serializable {
    public int[] foundIDs;
    public LatestMark latest;
    public boolean hasMore;
}