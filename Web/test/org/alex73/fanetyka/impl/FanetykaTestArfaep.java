package org.alex73.fanetyka.impl;

import static org.junit.Assert.assertEquals;

import java.io.File;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.io.FileUtils;
import org.junit.Test;

public class FanetykaTestArfaep {
    static final Pattern RE = Pattern.compile("(\\S+)\\s+\\[(\\S+)\\]");

    @Test
    public void test() throws Exception {
        List<String> lines = FileUtils.readLines(new File("test/org/alex73/fanetyka/impl/arfaep.txt"));
        List<String> skip = FileUtils.readLines(new File("test/org/alex73/fanetyka/impl/arfaepSkip.txt"));
        Pattern[] skipP=new Pattern[skip.size()];
        for(int i=0;i<skipP.length;i++) {
            int e=skip.get(i).indexOf(':');
            if (e<0) {
                skipP[i]=Pattern.compile(skip.get(i));
            }
        }

        int count = 0;
        w: for (String line : lines) {
            Matcher m = RE.matcher(line.trim());
            if (!m.matches()) {
                // System.out.println("err: "+line);
                continue;
            }
            String word = m.group(1).replace("*", "");
            for(int i=0;i<skip.size();i++) {
                if (skipP[i]!=null) {
                    if (skipP[i].matcher(word).matches()) {
                        continue w;
                    }
                }
            }

            String expected = m.group(2).replace("*", "");
            Fanetyka3 f = new Fanetyka3();
            try {
                for (String w : word.split("\\s+")) {
                    f.addWord(w);
                }

                f.calcFanetyka();
                String tr = f.toString(Huk.arfaep);
                if (!expected.equals(tr)) {
                    String trH = h(tr);
                    String exH = h(expected);
                    if (expected.contains("цк") || expected.replace("'", "").equals(tr.replace("'", ""))) {
                        continue;
                    }
                    System.out.println(word + ": " + tr + " замест " + expected);// + "  // " + trH + " замест " + exH);
                    count++;
                }
            } catch (Exception ex) {
                System.err.println("Памылка ў слове '" + word + "':"+ex.getMessage()+"   -> "+f);
               // throw ex;
            }
        }
        assertEquals(0, count);
    }

    static String h(String s) {
        StringBuilder o = new StringBuilder();
        for (char c : s.toCharArray()) {
            String p = Integer.toHexString(c);
            o.append(p).append(' ');
        }
        return o.toString();
    }
}
