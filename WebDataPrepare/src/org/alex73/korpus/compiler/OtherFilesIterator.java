package org.alex73.korpus.compiler;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.alex73.korpus.text.parser.IProcess;
import org.alex73.korpus.text.parser.Splitter2;
import org.alex73.korpus.text.xml.Content;
import org.alex73.korpus.text.xml.Header;
import org.alex73.korpus.text.xml.P;
import org.alex73.korpus.text.xml.Tag;
import org.alex73.korpus.text.xml.XMLText;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;

public class OtherFilesIterator {
    Pattern RE_ID = Pattern.compile("([0-9]+).+?OCR\\-texts\\.zip");

    IProcess errors;
    IFilesIterator callback;

    public OtherFilesIterator(IProcess errors, IFilesIterator callback) {
        this.errors = errors;
        this.callback = callback;
    }

    public void iterate(String filesDir) throws Exception {
        List<File> files = new ArrayList<>(FileUtils.listFiles(new File(filesDir), new String[] { "zip" }, true));
        if (files.isEmpty()) {
            throw new Exception("Няма тэкстаў ў " + filesDir);
        }
        Collections.shuffle(files);
        int c = 0;
        for (File f : files) {
            errors.showStatus("loadFileToOther " + f + ": " + (++c) + "/" + files.size());
            loadZipPagesToOther(f);
        }
    }

    protected void loadZipPagesToOther(File f) throws Exception {
        if (f.length() == 0) {
            return;
        }

        Matcher m = RE_ID.matcher(f.getName());
        if (!m.matches()) {
            throw new Exception("Wrong name: " + f);
        }

        System.out.println("loadFileToOther " + f);

        XMLText doc = new XMLText();
        doc.setHeader(new Header());
        doc.getHeader().getTag().add(new Tag("id", m.group(1)));
        doc.setContent(new Content());

        try (ZipFile zip = new ZipFile(f)) {
            String prevtext = null;
            for (Enumeration<? extends ZipEntry> it = zip.entries(); it.hasMoreElements();) {
                ZipEntry en = it.nextElement();
                if (en.isDirectory() || en.getSize() == 0) {
                    continue;
                }
                String text;
                try (InputStream in = new BufferedInputStream(zip.getInputStream(en))) {
                    text = IOUtils.toString(in, "UTF-8");
                }
                if (!text.isEmpty() && !text.equals(prevtext)) {
                    prevtext = text;
                    P p = new Splitter2(text, false, errors).getP();
                    doc.getContent().getPOrTagOrPoetry().add(p);
                }
            }
        }

        if (!doc.getContent().getPOrTagOrPoetry().isEmpty()) {
            callback.onText(doc);
        }
    }

    public static String getId(XMLText doc) {
        for (Tag t : doc.getHeader().getTag()) {
            if ("id".equals(t.getName())) {
                return t.getValue();
            }
        }
        return "";
    }
}
