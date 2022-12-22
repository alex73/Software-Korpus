package org.alex73.korpus.server.data;

import java.io.Serializable;

/**
 * DTO for total count result of search by corpus documents.
 */
public class SearchTotalResult implements Serializable {
    public String error;
    public int totalCount;
}
