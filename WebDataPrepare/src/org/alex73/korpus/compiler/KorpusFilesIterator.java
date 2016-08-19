package org.alex73.korpus.compiler;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.alex73.korpus.base.TextInfo;
import org.alex73.korpus.text.TextGeneral;
import org.alex73.korpus.text.TextIO;
import org.alex73.korpus.text.parser.IProcess;
import org.alex73.korpus.text.xml.Tag;
import org.alex73.korpus.text.xml.XMLText;
import org.apache.commons.compress.archivers.sevenz.SevenZArchiveEntry;
import org.apache.commons.compress.archivers.sevenz.SevenZFile;
import org.apache.commons.io.FileUtils;

public class KorpusFilesIterator {
    static final int BUFFER = 256 * 1024;

    BlockingQueue<XMLText> queue;
    IProcess errors;
    List<File> files;

    public KorpusFilesIterator(IProcess errors, BlockingQueue<XMLText> queue, String filesDir) throws Exception {
        this.errors = errors;
        this.queue = queue;

        files = new ArrayList<>(
                FileUtils.listFiles(new File(filesDir), new String[] { "xml", "text", "7z", "zip" }, true));
        if (files.isEmpty()) {
            throw new Exception("Няма тэкстаў ў " + filesDir);
        }
        Collections.shuffle(files);
    }

    public void process(ExecutorService executor) {
        for (File f : files) {
            // errors.showStatus("loadFileToCorpus " + f + ": " + (++c) + "/" +
            // files.size());
            executor.execute(new Runnable() {
                @Override
                public void run() {
                    System.out.println("read " + f);
                    try {
                        if (f.getName().endsWith(".xml") || f.getName().endsWith(".text")) {
                            loadXmlOrTextFileToCorpus(f);
                        } else if (f.getName().endsWith(".zip") || f.getName().endsWith(".7z")) {
                            loadArchiveFileToCorpus(f);
                        }
                    } catch (Exception ex) {
                        throw new RuntimeException(ex);
                    }
                }
            });
        }
        executor.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    queue.put(null);
                } catch (Exception ex) {
                    throw new RuntimeException(ex);
                }
            }
        });
    }

    protected void loadXmlOrTextFileToCorpus(File f) throws Exception {
        if (f.getName().endsWith(".xml")) {
            XMLText doc;
            InputStream in = new BufferedInputStream(new FileInputStream(f), BUFFER);
            try {
                doc = TextIO.parseXML(in);
            } finally {
                in.close();
            }
            queue.put(doc);
        } else if (f.getName().endsWith(".text")) {
            try {
                XMLText doc = new TextGeneral(f, errors).parse();
                queue.put(doc);
            } catch (Exception ex) {
                throw new RuntimeException("Памылка ў " + f + ": " + ex.getMessage(), ex);
            }
        } else {
            throw new RuntimeException("Unknown file: " + f);
        }
    }

    protected void loadArchiveFileToCorpus(File f) throws Exception {
        if (f.getName().endsWith(".zip")) {
            try (ZipFile zip = new ZipFile(f)) {
                int c = 0;
                for (Enumeration<? extends ZipEntry> it = zip.entries(); it.hasMoreElements();) {
                    ZipEntry en = it.nextElement();
                    if (en.isDirectory()) {
                        continue;
                    }
                    errors.showStatus("loadFileToCorpus " + f + "/" + en.getName() + ": " + (++c));
                    XMLText doc;
                    InputStream in = new BufferedInputStream(zip.getInputStream(en), BUFFER);
                    try {
                        if (en.getName().endsWith(".text")) {
                            doc = new TextGeneral(in, errors).parse();
                        } else if (en.getName().endsWith(".xml")) {
                            doc = TextIO.parseXML(in);
                        } else {
                            throw new RuntimeException("Unknown entry '" + en.getName() + "' in " + f);
                        }
                    } finally {
                        in.close();
                    }
                    queue.put(doc);
                }
            }
        } else if (f.getName().endsWith(".7z")) {
            SevenZFile sevenZFile = new SevenZFile(f);
            int c = 0;
            for (SevenZArchiveEntry en = sevenZFile.getNextEntry(); en != null; en = sevenZFile.getNextEntry()) {
                if (en.isDirectory()) {
                    continue;
                }
                errors.showStatus("loadFileToCorpus " + f + "/" + en.getName() + ": " + (++c));
                byte[] content = new byte[(int) en.getSize()];
                for (int p = 0; p < content.length;) {
                    p += sevenZFile.read(content, p, content.length - p);
                }
                try {
                    XMLText doc;
                    try (InputStream in = new ByteArrayInputStream(content)) {
                        if (en.getName().endsWith(".text")) {
                            doc = new TextGeneral(in, errors).parse();
                        } else if (en.getName().endsWith(".xml")) {
                            doc = TextIO.parseXML(in);
                        } else {
                            throw new RuntimeException("Unknown entry '" + en.getName() + "' in " + f);
                        }
                    }
                    queue.put(doc);
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
            sevenZFile.close();
        } else {
            throw new RuntimeException("Unknown file: " + f);
        }
    }

    public static TextInfo createTextInfo(XMLText text) {
        TextInfo r = new TextInfo();
        String authors = getTag(text, "Authors");
        if (authors != null) {
            r.authors = authors.split(",");
        } else {
            r.authors = new String[0];
        }
        String publishedYear = getTag(text, "PublishedYear");
        r.publishedYear = publishedYear != null ? Integer.parseInt(publishedYear) : 0;
        String writtenYear = getTag(text, "WrittenYear");
        r.writtenYear = writtenYear != null ? Integer.parseInt(writtenYear) : 0;
        String styleGenres = getTag(text, "StyleGenre");
        if (styleGenres != null) {
            r.styleGenres = styleGenres.split(",");
        } else {
            r.styleGenres = new String[0];
        }
        r.title = getTag(text, "Title");

        return r;
    }

    private static String getTag(XMLText text, String name) {
        for (Tag tag : text.getHeader().getTag()) {
            if (name.equals(tag.getName())) {
                return tag.getValue();
            }
        }
        return null;
    }
}
