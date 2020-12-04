package org.alex73.korpus.compiler.parsers;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ParserFactory {
    public static final List<Pair> parsers = new ArrayList<>();
    static {
        parsers.add(new Pair("nierazabranaje/.+\\-texts\\.zip", OcrTextParser.class));
        parsers.add(new Pair("nierazabranaje/.+\\-djvuxml\\.zip", OcrDjvuParser.class));
        parsers.add(new Pair("teksty/.+\\.text", TextParser.class));
        parsers.add(new Pair("teksty/.+\\.(jpg|gif|png)", NullParser.class));
        parsers.add(new Pair("wiki/.+\\.xml\\.bz2", WikiParser.class));
        parsers.add(new Pair(".+\\.7z", TextArchiveParser.class));
        parsers.add(new Pair(".+\\.zip", TextArchiveParser.class));
    }

    public static IParser getParser(String fn) {
        for (Pair p : parsers) {
            Matcher m = p.re.matcher(fn);
            if (m.matches()) {
                try {
                    return p.parser.newInstance();
                } catch (Exception ex) {
                    throw new RuntimeException(ex);
                }
            }
        }
        return null;
    }

    public static class Pair {
        Pattern re;
        Class<? extends IParser> parser;

        public Pair(String regexp, Class<? extends IParser> parser) {
            this.re = Pattern.compile(regexp);
            this.parser = parser;
        }
    }
}
