package org.alex73.korpus.text.parser;

public interface IProcess {
    void showStatus(String status);
    void reportError(String error, Throwable ex);
}
