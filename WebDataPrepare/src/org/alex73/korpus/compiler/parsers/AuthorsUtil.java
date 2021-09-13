package org.alex73.korpus.compiler.parsers;

import java.nio.file.FileVisitOption;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

import org.alex73.korpus.base.TextInfo;

public class AuthorsUtil {
    private static Map<String, String> authorsIndex = new HashMap<>();

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
                    tags.clear();
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
        String authorIndex = tags.get("AuthorIndex");
        if (author != null && authorIndex != null) {
            if (authorsIndex.put(author, authorIndex) != null) {
                throw new RuntimeException("Duplicate author in list: " + tags);
            }
        }
    }

    public static void fixAuthors(TextInfo textInfo) {
        if (textInfo.authors != null) {
            for (int i = 0; i < textInfo.authors.length; i++) {
                String replaced = authorsIndex.get(textInfo.authors[i]);
                if (replaced != null) {
                    textInfo.authors[i] = replaced;
                } else {
                    String[] a = textInfo.authors[i].split("\\s+");
                    switch (a.length) {
                    case 1:
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
