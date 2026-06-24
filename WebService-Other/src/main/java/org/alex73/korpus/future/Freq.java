package org.alex73.korpus.future;

import org.alex73.grammardb.GrammarDB2;
import org.alex73.korpus.utils.KorpusFileUtils;

import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class Freq extends FutureBaseServlet {
    private final String corpusCachePath;

    public Freq(String corpusCachePath) {
        super("future/freq.html");
        this.corpusCachePath = corpusCachePath;
    }

    @Override
    public List<Pair> process(Map<String, String> params) throws Exception {
        int count = Integer.parseInt(params.get("count"));

        List<String> frequences = KorpusFileUtils.readZip(Paths.get(corpusCachePath).resolve("stat-freq.zip"), "forms/freq.tab");
        frequences = frequences.subList(0, Math.min(frequences.size(), count));
        return frequences.stream().map(line -> new Pair(line)).collect(Collectors.toList());
    }

    public static class Pair {
        private final String word;
        private final int count;

        public Pair(String line) {
            int p = line.indexOf('=');
            word = line.substring(0, p);
            count = Integer.parseInt(line.substring(p + 1));
        }

        public String getWord() {
            return word;
        }

        public int getCount() {
            return count;
        }
    }
}
