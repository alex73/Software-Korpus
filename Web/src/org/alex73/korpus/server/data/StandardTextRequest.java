package org.alex73.korpus.server.data;

import java.io.Serializable;
import java.util.List;

public class StandardTextRequest implements Serializable {
    public String author;
    public List<String> stylegenres;
    public Integer yearWrittenFrom, yearWrittenTo, yearPublishedFrom, yearPublishedTo;
}
