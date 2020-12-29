package org.alex73.korpus.compiler;

import java.util.Arrays;
import java.util.Map;

import org.alex73.korpus.base.TextInfo;
import org.alex73.korpus.utils.KorpusDateTime;

public class TextUtils {
    public static void fillFromHeaders(TextInfo info, Map<String, String> headers) {
        String s;
        if ((s = get(headers, "URL")) != null) {
            info.url = s;
        }
        if ((s = get(headers, "Authors")) != null) {
            info.authors = trims(s.split(";"));
        }
        if ((s = get(headers, "Title")) != null) {
            info.title = s;
        }
        if ((s = get(headers, "Translation")) != null) {
            info.translators = trims(s.split(";"));
        }
        if ((s = get(headers, "Lang")) != null) {
            info.lang = s;
        }
        if ((s = get(headers, "LangOrig")) != null) {
            info.langOrig = s;
        }
        if ((s = get(headers, "StyleGenre")) != null) {
            info.styleGenres = trims(s.split("[;,]")); // TODO change to ';' separator
        }
        if ((s = get(headers, "Edition")) != null) {
            info.edition = s;
        }
        if ((s = get(headers, "PublicationYear")) != null) {
            info.publicationTime = s;
            new KorpusDateTime(info.publicationTime);
        }
        if ((s = get(headers, "CreationYear")) != null) {
            info.creationTime = s;
            new KorpusDateTime(info.creationTime);
        }
    }

    private static String[] trims(String[] list) {
        for (int i = 0; i < list.length; i++) {
            list[i] = list[i].trim();
            if (list[i].isEmpty()) {
                throw new RuntimeException("Wrong list data: " + Arrays.toString(list));
            }
        }
        return list;
    }

    private static String get(Map<String, String> headers, String key) {
        String v = headers.get(key);
        if (v != null) {
            v = v.trim();
            if (v.isEmpty()) {
                v = null;
            }
        }
        return v;
    }
}
