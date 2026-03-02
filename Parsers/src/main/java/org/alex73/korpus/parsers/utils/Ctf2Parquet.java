package org.alex73.korpus.parsers.utils;

import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.alex73.korpus.base.Ctf;
import org.apache.avro.reflect.Nullable;
import org.apache.avro.reflect.ReflectData;
import org.apache.parquet.avro.AvroParquetWriter;
import org.apache.parquet.hadoop.ParquetWriter;
import org.apache.parquet.hadoop.metadata.CompressionCodecName;
import org.apache.parquet.io.LocalOutputFile;

import com.google.gson.Gson;

public class Ctf2Parquet {
    static final Pattern RE_SUBCORPUS_NAME = Pattern.compile("[0-9]+\\.(.+)\\.zip");
    static final int BLOCK_SIZE_LINES = 100000;
    static List<Pattern> RE_SKIP_SUBCORPUS = List.of("skaryna", "dyjalektny", "hukavy\\..+").stream().map(s -> Pattern.compile(s)).toList();
    static List<Text> buffer = new ArrayList<Text>();

    static final Path OUT = Path.of("/tmp/korpus.parquet");
    static ParquetWriter<Text> writer;

    public static void main(String[] args) throws Exception {
        Files.deleteIfExists(OUT);
        var builder = AvroParquetWriter.<Text>builder(new LocalOutputFile(OUT));
        builder.withSchema(ReflectData.get().getSchema(Text.class));
        builder.withDataModel(ReflectData.get());
        builder.withCompressionCodec(CompressionCodecName.SNAPPY);
        writer = builder.build();
        for (Path f : Files.list(Path.of(".")).sorted().toList()) {
            if (!f.getFileName().toString().endsWith(".zip")) {
                continue;
            }
            processZip(f);
        }
        writer.close();
    }

    static void processZip(Path f) throws Exception {
        Matcher m = RE_SUBCORPUS_NAME.matcher(f.getFileName().toString());
        if (!m.matches()) {
            throw new Exception("Wrong zip file name: " + f);
        }
        String subcorpus = m.group(1);
        for (Pattern re : RE_SKIP_SUBCORPUS) {
            if (re.matcher(subcorpus).matches()) {
                return;
            }
        }

        System.out.println("Read " + f);
        try (Writer wr = Files.newBufferedWriter(Path.of(f.toString() + ".paragraphs.json"))) {
            try (ZipInputStream zip = new ZipInputStream(Files.newInputStream(f))) {
                ZipEntry zipEntry = null;
                while ((zipEntry = zip.getNextEntry()) != null) {
                    if (!zipEntry.getName().endsWith(".ctf")) {
                        throw new Exception("Wrong file name: " + f + "!" + zipEntry.getName());
                    }

                    process(subcorpus, zip.readAllBytes());
                    if (buffer.size() > BLOCK_SIZE_LINES) {
                        flush();
                    }
                }
                flush();
            }
        }
    }

    static void process(String subcorpus, byte[] file) throws Exception {
        Ctf data = new Gson().fromJson(new String(file, StandardCharsets.UTF_8), Ctf.class);
        for (Ctf.Language lang : data.languages) {
            if (lang.lang.equals("bel")) {
                List.of(lang.pages).stream().flatMap(p -> List.of(p.paragraphs).stream()).forEach(p -> {
                    Text txt = new Text();
                    txt.subcorpus = subcorpus;
                    txt.paragraphText = p;
                    txt.styleGenres = data.styleGenres;
                    txt.creationTime = lang.creationTime;
                    txt.publicationTime = lang.publicationTime;
                    buffer.add(txt);
                });
            }
        }
    }

    static void flush() throws Exception {
        Collections.shuffle(buffer);
        for (Text s : buffer) {
            writer.write(s);
        }
        buffer.clear();
    }

    public static class Text {
        public String subcorpus;
        public String paragraphText;
        @Nullable
        public String[] styleGenres;
        @Nullable
        public String creationTime, publicationTime;
    }
}
