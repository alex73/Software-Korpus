package org.alex73.korpus.compiler.stat;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicInteger;

import org.xerial.snappy.SnappyOutputStream;

public class WordsStat {
    private Writer byFormTempFile;
    private int wordsCount;
    private Set<String> allLemmas = null;
    private Map<String, ParadigmStat> byLemma = null;
    private Map<String, AtomicInteger> byForm = null;
    private Map<String, AtomicInteger> byUnknown = null;

    static class ParadigmStat {
        String para;
        int intCount;
        float floatCount;
        Map<String, Integer> valuesCount = new TreeMap<>();
    }

    public WordsStat(Path tempFile) {
        allLemmas = new HashSet<>();
        byForm = new HashMap<>();
        if (tempFile != null) {
            try {
                byFormTempFile = new BufferedWriter(new OutputStreamWriter(new SnappyOutputStream(Files.newOutputStream(tempFile)), StandardCharsets.UTF_8),
                        1024 * 1024);
            } catch (Exception ex) {
                throw new RuntimeException(ex);
            }
        }
    }

    public synchronized void closeFile() throws IOException {
        writeToFile();
        byFormTempFile.flush();
        byFormTempFile.close();
    }

    public void addWord(String normalizedValue, Set<String> lemmas) {
        wordsCount++;

        if (byForm != null && !normalizedValue.isEmpty()) {
            byForm.computeIfAbsent(normalizedValue, v -> new AtomicInteger()).incrementAndGet();
        }

        if (lemmas.isEmpty()) {
            if (byUnknown != null) {
                // TODO адкідаць пустыя словы, нумары, лацінку,
                if (normalizedValue.isEmpty()) {
                    return;
                }
                for (int i = 0; i < normalizedValue.length(); i++) {
                    char c = normalizedValue.charAt(i);
                    if ((c >= '0' && c <= '9') || (c >= 'a' && c <= 'z')) {
                        return;
                    }
                }
                byUnknown.get(normalizedValue).incrementAndGet();
            }
        } else {
            for (String lemma : lemmas) {
                allLemmas.add(lemma);
            }
            if (byLemma != null) {
                float part = 1.0f / lemmas.size();
                for (String lemma : lemmas) {
                    if (lemma.isEmpty()) {
                        continue;
                    }
                    ParadigmStat ps;
                    ps = byLemma.get(lemma);
                    if (ps == null) {
                        ps = new ParadigmStat();
                        ps.para = lemma;
                        byLemma.put(lemma, ps);
                    }
                    ps.intCount++;
                    ps.floatCount += part;
                    Integer prev = ps.valuesCount.get(normalizedValue);
                    ps.valuesCount.put(normalizedValue, prev == null ? 1 : prev.intValue() + 1);
                }
            }
        }
    }

    public synchronized void mergeFrom(WordsStat stat) throws Exception {
        for (Map.Entry<String, AtomicInteger> en : stat.byForm.entrySet()) {
            byForm.computeIfAbsent(en.getKey(), f -> new AtomicInteger()).addAndGet(en.getValue().get());
        }
        if (byForm.size() > 1000000) {
            writeToFile();
            byForm.clear();
        }
    }

    private void writeToFile() throws IOException {
        for (Map.Entry<String, AtomicInteger> en : byForm.entrySet()) {
            byFormTempFile.write(en.getKey());
            byFormTempFile.write('=');
            byFormTempFile.write(Integer.toString(en.getValue().get()));
            byFormTempFile.write('\n');
        }
    }

    public int getWordsCount() {
        return wordsCount;
    }

    public Set<String> getAllLemmas() {
        return allLemmas;
    }
}
