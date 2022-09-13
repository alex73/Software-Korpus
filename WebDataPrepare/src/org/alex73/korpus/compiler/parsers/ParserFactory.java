package org.alex73.korpus.compiler.parsers;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ParserFactory {
    public static final List<Pair> parsers = new ArrayList<>();
    static {
        parsers.add(new Pair("nierazabranaje:.+.zip", OcrTextParser.class));
        parsers.add(new Pair("teksty:.+\\.list", NullParser.class));
        parsers.add(new Pair("teksty:.+\\.text", TextParser.class));
        parsers.add(new Pair("teksty:.+\\.(jpg|gif|png)", NullParser.class));
        parsers.add(new Pair("sajty:.+\\.zip\\.headers", NullParser.class));
        parsers.add(new Pair("sajty:.+\\.zip", TextArchiveParser.class));
        parsers.add(new Pair("kankardans:.+\\.txt", KankardansParser.class));
        parsers.add(new Pair("dyjalektny:.+\\.text", DyjalektnyParser.class));
        parsers.add(new Pair("skaryna:.+\\.text", SkarynaParser.class));
        parsers.add(new Pair("wiki[a-z]*/.+\\.xml(\\.bz2)?", WikiParser.class));
    }

    public static IParser getParser(String relativePath, Path file) {
        String currentSubcorpus = relativePath.substring(0, relativePath.indexOf('/'));
        for (Pair p : parsers) {
            Matcher m = p.re.matcher(relativePath);
            if (m.matches()) {
                try {
                    return p.parser.getConstructor(String.class, Path.class).newInstance(currentSubcorpus, file);
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
