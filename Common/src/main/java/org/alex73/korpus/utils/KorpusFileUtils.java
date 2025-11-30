package org.alex73.korpus.utils;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Stream;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

public class KorpusFileUtils {
    public static void writeGzip(Path file, Stream<String> data) throws IOException {
        try (BufferedWriter wr = new BufferedWriter(new OutputStreamWriter(new GZIPOutputStream(Files.newOutputStream(file))))) {
            for (Iterator<String> it = data.iterator(); it.hasNext();) {
                wr.write(it.next());
                wr.write('\n');
            }
        }
    }

    public static void writeZip(ZipOutputStream zip, String entryName, Stream<String> data) throws IOException {
        zip.putNextEntry(new ZipEntry(entryName));
        BufferedWriter wr = new BufferedWriter(new OutputStreamWriter(zip));
        for (Iterator<String> it = data.iterator(); it.hasNext();) {
            wr.write(it.next());
            wr.write('\n');
        }
        wr.flush();
    }

    public static List<String> readGzip(Path file) throws IOException {
        List<String> result = new ArrayList<>();
        try (BufferedReader rd = new BufferedReader(new InputStreamReader(new GZIPInputStream(Files.newInputStream(file))))) {
            String s;
            while ((s = rd.readLine()) != null) {
                result.add(s);
            }
        }
        return result;
    }

    public static List<String> readZip(Path file, String entry) throws IOException {
        try (ZipFile zip = new ZipFile(file.toFile())) {
            ZipEntry en = zip.getEntry(entry);
            if (en == null) {
                throw new IOException("There is no entry " + entry + " in the " + file.toAbsolutePath());
            }

            List<String> result = new ArrayList<>();
            try (BufferedReader rd = new BufferedReader(new InputStreamReader(zip.getInputStream(en)))) {
                String s;
                while ((s = rd.readLine()) != null) {
                    result.add(s);
                }
            }
            return result;
        }
    }
}
