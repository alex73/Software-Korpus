package org.alex73.korpus.parsers;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.Reader;

import org.alex73.korpus.parsers.KnihiComParser.Autary;

import com.google.gson.Gson;

public class Authors {
    static Autary autary;
    static {
        try (Reader in = new BufferedReader(new InputStreamReader(KnihiComParser.class.getResourceAsStream("Authors.json"), "UTF-8"))) {
            autary = new Gson().fromJson(in, Autary.class);
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    public static String[] autaryIndexes(String au) {
        if (au == null || au.isBlank()) {
            return null;
        }
        String[] textAuthors = au.split(";");
        String[] authorIndexes = new String[textAuthors.length];
        for (int i = 0; i < textAuthors.length; i++) {
            String a = autaryPravapis(textAuthors[i].trim().replaceAll("\\s+", " "));
            String index = autary.author2index.get(a);
            if (index == null) {
                String[] nm = a.split(" ");
                switch (nm.length) {
                case 1:
                    index = nm[0];
                    break;
                case 2:
                    index = nm[1] + " " + nm[0];
                    break;
                default:
                    throw new RuntimeException("Author index for '" + a + "' should be defined");
                }
            }
            authorIndexes[i] = index;
        }
        return authorIndexes;
    }

    public static String autaryPravapis(String au) {
        if (au == null || au.isBlank()) {
            return null;
        }
        String[] textAuthors = au.split(";");
        for (int i = 0; i < textAuthors.length; i++) {
            String a = textAuthors[i].trim().replaceAll("\\s+", " ");
            textAuthors[i] = autary.author2pravapis.getOrDefault(a, a);
        }
        return String.join(",", textAuthors);
    }
}
