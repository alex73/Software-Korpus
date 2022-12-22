import java.util.HashMap;
import java.util.Map;

import org.alex73.corpus.paradigm.Form;
import org.alex73.corpus.paradigm.Variant;
import org.alex73.korpus.base.GrammarDB2;
import org.alex73.korpus.utils.SetUtils;

public class StatTags {

    static Map<String, Integer> countVariantTags = new HashMap<>();
    static Map<String, Integer> countFormTags = new HashMap<>();

    public static void main(String[] args) throws Exception {
        GrammarDB2 db = GrammarDB2.initializeFromDir("/data/gits/GrammarDB");
        db.getAllParadigms().stream().forEach(p -> {
            for (Variant v : p.getVariant()) {
                String tag = SetUtils.tag(p, v);
                inc(countVariantTags, tag);
                for (Form f : v.getForm()) {
                    tag = SetUtils.tag(p, v, f);
                    inc(countFormTags, tag);
                }
            }
        });
        System.out.println("Variant tags:");
        countVariantTags.entrySet().stream().sorted((a, b) -> a.getKey().compareTo(b.getKey()))
                .forEach(System.out::println);
        System.out.println("Form tags:");
        countFormTags.entrySet().stream().sorted((a, b) -> a.getKey().compareTo(b.getKey()))
                .forEach(System.out::println);
    }

    static void inc(Map<String, Integer> counts, String tag) {
        Integer prev = counts.get(tag);
        prev = prev == null ? 1 : prev.intValue() + 1;
        counts.put(tag, prev);
    }
}
