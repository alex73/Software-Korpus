package org.alex73.korpus.parsers.utils;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TextFileHeaders {
    private static Pattern RE_TAG = Pattern.compile("##([^:]+):?(.*)");
    private static Pattern RE_TAG_BEGIN = Pattern.compile("##([^:]+)_BEGIN");

    private Map<String, String> values = new HashMap<>();

    public String get(String key) {
        return values.get(key);
    }

    public Map<String, String> getAll() {
        return values;
    }

    public void appendDefaultHeaders(TextFileHeaders defaults) {
        for (Map.Entry<String, String> en : defaults.values.entrySet()) {
            if (!values.containsKey(en.getKey())) {
                values.put(en.getKey(), en.getValue());
            }
        }
    }

    public int parse(List<String> lines, int currentLine) {
        String tag = null;
        String value = null;
        String endLine = null;
        for (; currentLine < lines.size(); currentLine++) {
            String s = lines.get(currentLine).trim();
            if (endLine != null) {
                if (s.equals(endLine)) {
                    values.put(tag, value.trim());
                    endLine = null;
                } else {
                    value += s + "\n";
                }
                continue;
            }
            if (s.isBlank()) {
                currentLine++;
                break;
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
        return currentLine;
    }

    public static String[] splitAndTrim(String s) {
        if (s == null || s.isBlank()) {
            return null;
        }
        String[] list = s.split(";");
        for (int i = 0; i < list.length; i++) {
            list[i] = list[i].trim();
            if (list[i].isEmpty()) {
                throw new RuntimeException("Wrong list data: " + Arrays.toString(list));
            }
        }
        return list;
    }

    public static void addHeader(List<String> out, String name, String value) {
        if (value != null && !value.isBlank()) {
            out.add(name + ":" + value);
        }
    }
}
