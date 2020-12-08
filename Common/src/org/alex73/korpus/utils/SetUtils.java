package org.alex73.korpus.utils;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.alex73.corpus.paradigm.Form;
import org.alex73.corpus.paradigm.Paradigm;
import org.alex73.corpus.paradigm.Variant;

public class SetUtils {

    public static String set2string(Set<String> set) {
        if (set.isEmpty()) {
            return null;
        }
        StringBuilder r = new StringBuilder();
        for (String s : set) {
            if (r.length() > 0) {
                r.append('_');
            }
            r.append(s);
        }
        return r.toString();
    }

    public static String addTag(String orig, String tag) {
        if (orig == null || orig.trim().isEmpty()) {
            return tag;
        }
        if (hasTag(orig, tag)) {
            return orig;
        }
        return orig + ',' + tag;
    }

    public static boolean hasTag(String orig, String tag) {
        if (orig == null || orig.trim().isEmpty()) {
            return false;
        }
        for (String v : orig.split(",")) {
            if (v.trim().equals(tag)) {
                return true;
            }
        }
        return false;
    }

    public static String removeTag(String orig, String tag) {
        if (orig == null || orig.trim().isEmpty()) {
            return null;
        }
        List<String> out = new ArrayList<>();
        for (String v : orig.split(",")) {
            if (!v.trim().equals(tag)) {
                out.add(v);
            }
        }
        return out.isEmpty() ? null : String.join(",", out);
    }

    public static boolean hasSlounik(Form f, String slounik) {
        return hasTag(f.getSlouniki(), slounik);
    }

    public static void addSlounik(Form f, String slounik) {
        f.setSlouniki(addTag(f.getSlouniki(), slounik));
    }

    public static void removeSlounik(Form f, String slounik) {
        f.setSlouniki(removeTag(f.getSlouniki(), slounik));
    }

    public static boolean hasPravapis(Form f, String pravapis) {
        return hasTag(f.getPravapis(), pravapis);
    }

    public static boolean hasPravapis(Variant v, String pravapis) {
        return hasTag(v.getPravapis(), pravapis);
    }

    public static void addPravapis(Form f, String pravapis) {
        f.setPravapis(addTag(f.getPravapis(), pravapis));
    }

    public static void addPravapis(Variant v, String pravapis) {
        v.setPravapis(addTag(v.getPravapis(), pravapis));
    }

    public static String tag(Paradigm p, Variant v) {
        String pt = p.getTag() != null ? p.getTag() : "";
        String vt = v.getTag() != null ? v.getTag() : "";
        return pt + vt;
    }

    public static String tag(Paradigm p, Variant v, Form f) {
        String pt = p.getTag() != null ? p.getTag() : "";
        String vt = v.getTag() != null ? v.getTag() : "";
        return pt + vt + f.getTag();
    }

    public static String toString(Paradigm p) {
        return "Paradigm: " + p.getTag() + "/" + p.getLemma() + "[" + p.getPdgId() + "]";
    }

    public static String concatNullable(String s1, String s2) {
        if (s1 == null && s2 == null) {
            return null;
        } else if (s1 == null && s2 != null) {
            return s2;
        } else if (s1 != null && s2 == null) {
            return s1;
        } else {
            return s1 + s2;
        }
    }
}
