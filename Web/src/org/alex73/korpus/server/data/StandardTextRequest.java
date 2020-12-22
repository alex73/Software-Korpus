package org.alex73.korpus.server.data;

import java.io.Serializable;
import java.util.List;

public class StandardTextRequest implements Serializable {
    public List<String> subcorpuses;
    public List<String> authors;
    public List<String> sources;
    public List<String> stylegenres;
    public Integer yearWrittenFrom, yearWrittenTo, yearPublishedFrom, yearPublishedTo;
}
