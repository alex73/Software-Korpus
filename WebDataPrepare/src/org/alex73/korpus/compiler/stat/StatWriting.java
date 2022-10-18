package org.alex73.korpus.compiler.stat;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.Collator;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.zip.ZipOutputStream;

import org.alex73.korpus.compiler.ProcessStat.TextStatInfo;
import org.alex73.korpus.utils.KorpusFileUtils;
import org.xerial.snappy.SnappyInputStream;

public class StatWriting {
    private static final Collator BE = Collator.getInstance(new Locale("be"));

    public static void write(Map<String, TextStatInfo> textStatInfos, Map<String, Set<String>> authorsByLemmas, Path dir) throws Exception {
        List<String> stat = new ArrayList<>();
        for (Map.Entry<String, TextStatInfo> en : textStatInfos.entrySet()) {
            if (!en.getValue().authors.isEmpty()) {
                stat.add("authors." + en.getKey() + "=" + joinSorted(en.getValue().authors));
            }
            if (!en.getValue().sources.isEmpty()) {
                stat.add("sources." + en.getKey() + "=" + joinSorted(en.getValue().sources));
            }
        }
        for (Map.Entry<String, TextStatInfo> en : textStatInfos.entrySet()) {
            stat.add("texts." + en.getKey() + "=" + en.getValue().texts.get());
            stat.add("words." + en.getKey() + "=" + en.getValue().words.get());
        }
        Files.write(dir.resolve("stat.properties"), stat);

        List<String> lemmas = new ArrayList<>(authorsByLemmas.keySet());
        Collections.sort(lemmas, BE);

        KorpusFileUtils.writeGzip(dir.resolve("lemma-authors.list.gz"), lemmas.stream().map(le -> le + '=' + String.join(";", authorsByLemmas.get(le))));
    }

    public static void mergeCounts(Collection<Path> snappyFiles, ZipOutputStream out, String entryName) throws Exception {
        Map<String, AtomicInteger> wordCounts = new HashMap<>();
        for (Path snappyFile : snappyFiles) {
            try (BufferedReader in = new BufferedReader(
                    new InputStreamReader(new SnappyInputStream(new BufferedInputStream(Files.newInputStream(snappyFile), 1024 * 1024))))) {
                String s;
                while ((s = in.readLine()) != null) {
                    int p = s.lastIndexOf('=');
                    if (p < 0) {
                        throw new Exception("Wrong line: " + s);
                    }
                    String w = s.substring(0, p);
                    int c = Integer.parseInt(s.substring(p + 1));
                    wordCounts.computeIfAbsent(w, word -> new AtomicInteger()).addAndGet(c);
                }
            }
        }
        synchronized (out) {
            KorpusFileUtils.writeZip(out, entryName,
                    wordCounts.entrySet().stream().sorted((a, b) -> Integer.compare(b.getValue().get(), a.getValue().get())).map(en -> en.toString()));
        }
    }

    private static String joinSorted(Collection<String> list) {
        return list.stream().sorted(BE).collect(Collectors.joining(";"));
    }
}
