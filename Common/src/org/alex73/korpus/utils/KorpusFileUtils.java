package org.alex73.korpus.utils;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class KorpusFileUtils {
    public static void writeGzip(Path file, Stream<String> data) throws IOException {
        try (BufferedWriter wr = new BufferedWriter(
                new OutputStreamWriter(new GZIPOutputStream(Files.newOutputStream(file))))) {
            for (String s : data.toList()) {
                wr.write(s);
                wr.write('\n');
            }
        }
    }

    public static void writeZip(ZipOutputStream zip, String entryName, Stream<String> data) throws IOException {
        zip.putNextEntry(new ZipEntry(entryName));
        BufferedWriter wr = new BufferedWriter(new OutputStreamWriter(zip));
        for (String s : data.toList()) {
            wr.write(s);
            wr.write('\n');
        }
        wr.flush();
    }

    public static Stream<String> readGzip(Path file) throws IOException {
        List<String> result = new ArrayList<>();
        try (BufferedReader rd = new BufferedReader(
                new InputStreamReader(new GZIPInputStream(Files.newInputStream(file))))) {
            String s;
            while ((s = rd.readLine()) != null) {
                result.add(s);
            }
        }
        return result.stream();
    }
}
