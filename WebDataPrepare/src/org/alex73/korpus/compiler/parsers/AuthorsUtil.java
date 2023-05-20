package org.alex73.korpus.compiler.parsers;

import java.nio.file.FileVisitOption;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

import org.alex73.korpus.base.TextInfo;

public class AuthorsUtil {
    private static Map<String, Map<String, String>> authors = new HashMap<>();

    public static void init(Path dir) throws Exception {
        Files.find(dir, Integer.MAX_VALUE,
                (p, a) -> a.isRegularFile() && p.getFileName().toString().contains("autary")
                        && p.getFileName().toString().endsWith(".list"),
                FileVisitOption.FOLLOW_LINKS).forEach(p -> initializeAuthors(p));
    }

    private static void initializeAuthors(Path file) {
        try {
            Map<String, String> tags = new TreeMap<>();
            for (String s : Files.readAllLines(file)) {
                s = s.trim();
                if (s.isEmpty()) {
                    addAuthorToIndex(tags);
                    tags = new TreeMap<>();
                } else if (!s.startsWith("##")) {
                    continue;
                } else {
                    int pos = s.indexOf(':');
                    if (pos < 0) {
                        throw new Exception("Error parse " + file + ": " + s);
                    }
                    String key = s.substring(2, pos).trim();
                    String value = s.substring(pos + 1).trim();
                    tags.put(key, value);
                }
            }
            addAuthorToIndex(tags);
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    private static void addAuthorToIndex(Map<String, String> tags) {
        String author = tags.get("Author");
        if (author == null) {
            return;
        }
        if (authors.put(author, tags) != null) {
            throw new RuntimeException("Duplicate author in list: " + tags);
        }
    }

    public static synchronized void fixAuthors(TextInfo.Subtext textInfo) {
        if (textInfo.authors != null) {
            for (int i = 0; i < textInfo.authors.length; i++) {
                Map<String, String> tags = authors.get(textInfo.authors[i]);
                if (tags == null) {
                    tags = Map.of("Author", textInfo.authors[i]);
                }
                String replaced = tags.get("AuthorIndexStandard");
                if (replaced == null) {
                    replaced = tags.get("AuthorIndex");
                }
                if (replaced != null) {
                    textInfo.authors[i] = replaced;
                } else {
                    String standard = tags.get("AuthorStandard");
                    if (standard == null) {
                        standard = tags.get("Author");
                    }
                    String[] a = standard.split("\\s+");
                    switch (a.length) {
                    case 1:
                        textInfo.authors[i] = a[0];
                        break;
                    case 2:
                        textInfo.authors[i] = a[1] + ' ' + a[0];
                        break;
                    default:
                        throw new RuntimeException("Impossible to index author: " + textInfo.authors[i]);
                    }
                }
            }
        }
    }
}
