package org.alex73.korpus.compiler;

import org.alex73.korpus.text.xml.XMLText;

public interface IFilesIterator {
    void onText(XMLText doc) throws Exception;
}
