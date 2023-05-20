package org.alex73.korpus.text.parser;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Headers {
    private static Pattern RE_TAG = Pattern.compile("##([^:]+):?(.*)");
    private static Pattern RE_TAG_BEGIN = Pattern.compile("##([^:]+)_BEGIN");

    private List<String> lines = new ArrayList<>();
    private Map<String, String> values;

    public void add(String s) {
        lines.add(s);
    }

    public List<String> getLines() {
        return lines;
    }

    public String get(String key) {
        if (values == null) {
            parse();
        }
        return values.get(key);
    }

    public Map<String, String> getAll() {
        if (values == null) {
            parse();
        }
        return new TreeMap<>(values);
    }

    public void appendDefaultHeaders(Headers defaults) {
        if (defaults.values == null) {
            defaults.parse();
        }
        if (values == null) {
            parse();
        }
        for (Map.Entry<String, String> en : defaults.values.entrySet()) {
            if (!values.containsKey(en.getKey())) {
                values.put(en.getKey(), en.getValue());
            }
        }
    }

    private void parse() {
        values = new HashMap<>();
        String tag = null;
        String value = null;
        String endLine = null;
        for (String s : lines) {
            s = s.trim();
            if (endLine != null) {
                if (s.equals(endLine)) {
                    values.put(tag, value.trim());
                    endLine = null;
                } else {
                    value += s + "\n";
                }
                continue;
            }
            Matcher m;
            if ((m = RE_TAG_BEGIN.matcher(s)).matches()) {
                tag = m.group(1);
                endLine = "##" + tag + "_END";
            } else if ((m = RE_TAG.matcher(s)).matches()) {
                if (values.containsKey(m.group(1))) {
                    throw new RuntimeException("Загаловак '##" + m.group(1) + "' вызначаны двойчы");
                }
                values.put(m.group(1), m.group(2).trim());
            } else {
                throw new RuntimeException("Няправільны загаловак '" + s + "'");
            }
        }
    }
}
