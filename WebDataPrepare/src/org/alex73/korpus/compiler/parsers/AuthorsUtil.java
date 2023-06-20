package org.alex73.korpus.compiler.parsers;

import java.nio.file.FileVisitOption;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

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
        String authorStandard = tags.get("AuthorStandard");
        if (authorStandard == null) {
            return;
        }
        if (authors.put(authorStandard, tags) != null) {
            throw new RuntimeException("Duplicate author in list: " + tags);
        }
    }

    /**
     * Разбірае вядомыя імёны аўтараў, і перарабляе ў афіцыйны правапіс.
     */
    public static String[] parseAuthors(String s) {
        if (s == null || s.isBlank()) {
            return null;
        }
        String[] list = s.split(";");
        for (int i = 0; i < list.length; i++) {
            list[i] = list[i].trim().replaceAll("\\?$", "");
            if (list[i].isEmpty()) {
                throw new RuntimeException("Wrong list data: " + Arrays.toString(list));
            }
            Map<String, String> tags = authors.get(list[i]);
            if (tags == null) {
                throw new RuntimeException("Невядомы аўтар: " + list[i]);
            }
            list[i] = tags.getOrDefault("AuthorStandard", tags.get("Author"));
        }
        return list;
    }

    /**
     * Пераварочвае імёны аўтараў каб прозвішча ішло першым - для фільтраў пошуку.
     */
    public static String[] reverseNames(String[] au) {
        if (au == null) {
            return null;
        }
        String[] list = new String[au.length];
        for (int i = 0; i < list.length; i++) {
            Map<String, String> tags = authors.get(au[i]);
            if (tags == null) {
                throw new RuntimeException("Невядомы аўтар: " + list[i]);
            }
            String replaced = tags.get("AuthorIndexStandard");
            if (replaced == null) {
                replaced = tags.get("AuthorIndex");
            }
            if (replaced != null) {
                list[i] = replaced;
            } else {
                String standard = tags.get("AuthorStandard");
                if (standard == null) {
                    standard = tags.get("Author");
                }
                String[] a = standard.split("\\s+");
                switch (a.length) {
                case 1:
                    list[i] = a[0];
                    break;
                case 2:
                    list[i] = a[1] + ' ' + a[0];
                    break;
                default:
                    throw new RuntimeException("Impossible to index author: " + list[i]);
                }
            }
        }
        return list;
    }
}
