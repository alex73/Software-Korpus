package org.alex73.korpus.compiler;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.alex73.korpus.base.TextInfo;
import org.alex73.korpus.text.TextGeneral;
import org.alex73.korpus.text.TextIO;
import org.alex73.korpus.text.parser.IProcess;
import org.alex73.korpus.text.xml.Tag;
import org.alex73.korpus.text.xml.XMLText;
import org.apache.commons.compress.archivers.sevenz.SevenZArchiveEntry;
import org.apache.commons.compress.archivers.sevenz.SevenZFile;
import org.apache.commons.io.FileUtils;

public class KorpusFilesIterator {
    static final int BUFFER = 256 * 1024;

    IFilesIterator callback;
    IProcess errors;
    List<File> files;

    public KorpusFilesIterator(IProcess errors, IFilesIterator callback) throws Exception {
        this.errors = errors;
        this.callback = callback;
    }

    public static TextInfo createTextInfo(XMLText text) {
        TextInfo r = new TextInfo();
        r.url = getTag(text, "URL");
        String date = getTag(text, "Date");
        r.date = date != null ? Long.parseLong(date) : 0;
        String authors = getTag(text, "Authors");
        if (authors != null) {
            r.authors = authors.split(",");
        } else {
            r.authors = new String[0];
        }
        r.title = getTag(text, "Title");
        String translation = getTag(text, "Translation");
        if (translation != null) {
            r.translators = translation.split(",");
        } else {
            r.translators = new String[0];
        }
        r.langOrig = getTag(text, "LangOrig");
        r.publicationTime = getTag(text, "FirstPublicationYear");
        r.writtenTime = getTag(text, "CreationYear");
        r.edition = getTag(text, "Edition");
        if (r.edition == null) {
            r.edition = getTag(text, "HiddenEdition");
        }
        String styleGenres = getTag(text, "StyleGenre");
        if (styleGenres != null) {
            r.styleGenres = styleGenres.split(",");
        } else {
            r.styleGenres = new String[0];
        }

        return r;
    }

    private static String getTag(XMLText text, String name) {
        for (Tag tag : text.getHeader().getTag()) {
            if (name.equals(tag.getName())) {
                return tag.getValue().isEmpty() ? null : tag.getValue();
            }
        }
        return null;
    }
}
