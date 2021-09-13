package org.alex73.korpus.compiler.parsers;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ParserFactory {
    public static final List<Pair> parsers = new ArrayList<>();
    static {
        parsers.add(new Pair("nierazabranaje:.+.text", OcrTextParser.class));
        parsers.add(new Pair("teksty:.+\\.list", NullParser.class));
        parsers.add(new Pair("teksty:.+\\.text", TextParser.class));
        parsers.add(new Pair("teksty:.+\\.(jpg|gif|png)", NullParser.class));
        parsers.add(new Pair("wiki:.+\\.xml(\\.bz2)?", WikiParser.class));
        parsers.add(new Pair("sajty:.+\\.zip\\.headers", NullParser.class));
        parsers.add(new Pair("sajty:.+\\.zip", TextArchiveParser.class));
    }

    public static IParser getParser(String subcorpus, Path file) {
        for (Pair p : parsers) {
            Matcher m = p.re.matcher(subcorpus + ":" + file.toString());
            if (m.matches()) {
                try {
                    return p.parser.getConstructor(String.class, Path.class).newInstance(subcorpus, file);
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
