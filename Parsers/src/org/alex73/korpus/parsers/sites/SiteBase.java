package org.alex73.korpus.parsers.sites;

import java.io.BufferedInputStream;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import org.alex73.korpus.base.Ctf;
import org.alex73.korpus.parsers.utils.Output;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.netpreserve.jwarc.WarcReader;
import org.netpreserve.jwarc.WarcRecord;
import org.netpreserve.jwarc.WarcResponse;

public abstract class SiteBase {
    void run(String[] args) throws Exception {
        Path input = Path.of(args[0]);
        Path output = Path.of(args[1]);
        Output os = new Output(output);
        try (InputStream in = new BufferedInputStream(Files.newInputStream(input), 65536)) {
            try (WarcReader rdin = new WarcReader(in)) {
                for (WarcRecord rec : rdin) {
                    String url = ((WarcResponse) rec).target();
                    System.out.println(url);
                    byte[] data;
                    try (InputStream bin = rec.body().stream()) {
                        data = bin.readAllBytes();
                    }
                    Ctf text = parsePage(url, data);
                    if (text != null) {
                        String path = url.replaceAll("^https?://[^/]+/", "").replace('?', '_').replaceAll("/$", "_") + ".ctf";
                        os.write(path, text);
                    }
                }
            }
        }
        os.close();
    }

    abstract protected Ctf parsePage(String url, byte[] page) throws Exception;

    Element oneElement(Elements list) {
        switch (list.size()) {
        case 0:
            throw new RuntimeException("No expected element");
        case 1:
            return list.get(0);
        default:
            throw new RuntimeException("Too many expected elements");
        }
    }

    Element oneOrNoneElement(Elements list) {
        switch (list.size()) {
        case 0:
            return null;
        case 1:
            return list.get(0);
        default:
            throw new RuntimeException("Too many expected elements");
        }
    }

    static String fixText(String text) {
        text = text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
        return text;
    }

    static List<String> parseText(String text) {
        text = fixText(text);
        List<String> result = new ArrayList<>();
        StringBuilder s = new StringBuilder();
        boolean wasSpace = false;
        for (char c : text.toCharArray()) {
            if (c == '\n') {
                if (!s.isEmpty()) {
                    result.add(s.toString());
                }
                s.setLength(0);
                wasSpace = false;
            } else if (c == '\u00A0' || Character.isWhitespace(c)) {
                if (!s.isEmpty()) {
                    wasSpace = true;
                }
            } else {
                if (wasSpace) {
                    s.append(' ');
                }
                wasSpace = false;
                s.append(c);
            }
        }
        if (!s.isEmpty()) {
            result.add(s.toString());
        }
        return result;
    }
}
