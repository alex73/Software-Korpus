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
    protected void runWarc(String[] args) throws Exception {
        Path input = Path.of(args[0]);
        Path output = Path.of(args[1]);
        Output os = new Output(output);
        try (InputStream in = new BufferedInputStream(Files.newInputStream(input), 65536)) {
            try (WarcReader rdin = new WarcReader(in)) {
                for (WarcRecord rec : rdin) {
                    if (!(rec instanceof WarcResponse)) continue;
                    String url = ((WarcResponse) rec).target();
                    url = filterUrl(url);
                    if (url == null) continue;
                    System.out.println(url);
                    byte[] data;
                    try (InputStream bin = rec.body().stream()) {
                        data = bin.readAllBytes();
                    }
                    processPage(url, data, os);
                }
            }
        }
        os.close();
    }

    protected void runZip(String[] args) throws Exception {
        Path input = Path.of(args[0]);
        Path output = Path.of(args[1]);
        Output os = new Output(output);
        try (InputStream in = new BufferedInputStream(Files.newInputStream(input), 65536);
             java.util.zip.ZipInputStream zin = new java.util.zip.ZipInputStream(in)) {
            java.util.zip.ZipEntry entry;
            while ((entry = zin.getNextEntry()) != null) {
                if (entry.isDirectory()) continue;
                String url = entry.getName();
                url = filterUrl(url);
                if (url == null) continue;
                System.out.println(url);
                byte[] data = zin.readAllBytes();
                processPage(url, data, os);
            }
        }
        os.close();
    }

    protected void runDir(String[] args) throws Exception {
        Path input = Path.of(args[0]);
        Path output = Path.of(args[1]);
        Output os = new Output(output);
        Files.walk(input).filter(Files::isRegularFile).sorted().forEach(file -> {
            try {
                String url = input.relativize(file).toString().replace('\\', '/');
                url = filterUrl(url);
                if (url == null) return;
                System.out.println(url);
                byte[] data = Files.readAllBytes(file);
                processPage(url, data, os);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
        os.close();
    }

    protected String filterUrl(String url) {
        return url;
    }

    private void processPage(String url, byte[] data, Output os) throws Exception {
        Ctf text = parsePage(url, data);
        if (text != null) {
            String path = url.replaceAll("^https?://[^/]+/", "").replace('?', '_').replaceAll("/$", "_") + ".ctf";
            os.write(path, text);
        }
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

    void removeElement(Element element) {
        if (element != null) {
            element.remove();
        }
    }

    static String fixText(String text) {
        text = text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
        return text;
    }

    static String textFromElement(Element element) {
        element.select("br").append("{{NEWLINE}}");
        element.select("p,div").prepend("{{NEWLINE}}");
        return element.text().replace("{{NEWLINE}}", "\n");
    }

    static List<String> parseText(String text) {
        text = fixText(text).trim();
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
