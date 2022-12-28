package org.alex73.korpus.utils;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.alex73.korpus.compiler.parsers.TmxParser;

public class ValidateTMX {
    static final Pattern RE_NUMBERS = Pattern.compile("[0-9]+");

    public static void main(String[] args) throws Exception {
        for (Path f : Files.list(Paths.get("")).sorted().filter(p -> p.toString().endsWith(".tmx")).toList()) {
            System.err.println("Processing " + f + "...");
            TmxParser.ParsedTMX parsed = new TmxParser.ParsedTMX(f);
            if (parsed.tmxLanguages.size() != 2) {
                throw new Exception("Wrong languages count");
            }
            for (Map<String, String> tu : parsed.segments) {
                String[] tuvs = tu.values().toArray(new String[0]);
                if (tuvs.length != 2) {
                    throw new Exception("Wrong tuv count");
                }
                List<String> n1 = getNumbers(tuvs[0]);
                List<String> n2 = getNumbers(tuvs[1]);
                if (!n1.equals(n2)) {
                    System.err.println("Wrong numbers (" + n1 + " vs " + n2 + ") in :\n    " + tuvs[0] + "\n    " + tuvs[1]);
                }
            }
        }
    }

    static List<String> getNumbers(String s) {
        List<String> allMatches = new ArrayList<String>();
        Matcher m = RE_NUMBERS.matcher(s);
        while (m.find()) {
            allMatches.add(m.group());
        }
        Collections.sort(allMatches);
        return allMatches;
    }
}
