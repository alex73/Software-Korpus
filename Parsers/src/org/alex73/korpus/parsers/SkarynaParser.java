package org.alex73.korpus.parsers;

import java.io.ByteArrayInputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.Collator;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

import org.alex73.korpus.base.Ctf;
import org.alex73.korpus.parsers.utils.Output;
import org.alex73.korpus.parsers.utils.TextFileParser;

public class SkarynaParser {
    static final Collator BE = Collator.getInstance(Locale.of("be"));
    static List<TF> texts = new ArrayList<>();

    public static void main(String[] args) throws Exception {
        if (args.length != 2) {
            System.err.println("SkarynaParser <каталог з зыходнымі файламі> <файл каб захаваць падкорпус>");
            System.exit(1);
        }
        Path in = Path.of(args[0]);
        for (Path f1 : Files.list(in).sorted().filter(p -> p.toString().endsWith(".text")).toList()) {
            parse(f1);
        }

        Collections.sort(texts, (o1, o2) -> BE.compare(o1.text.languages[0].title, o2.text.languages[0].title));
        Path fo = Path.of(args[1]);
        try (Output os = new Output(fo)) {
            for (TF tf : texts) {
                os.write(tf.file, tf.text);
            }
        }
    }

    static class TF {
        String file;
        Ctf text = new Ctf();
    }

    public static void parse(Path file) throws Exception {
        System.out.println("Чытаем " + file + "...");
        byte[] data = Files.readAllBytes(file);
        try {
            TextFileParser.OneText doc = new TextFileParser(new ByteArrayInputStream(data)).oneTextExpected();

            TF tf = new TF();
            tf.text.setParagraphs("bel", doc.paragraphs);
            tf.file = file.getFileName().toString().replaceAll("\\.text$", ".ctf");
            tf.text.languages[0].title = doc.headers.get("Title");
            if (tf.text.languages[0].title == null) {
                throw new RuntimeException("Нявызначаны загаловак 'Title' у " + file);
            }
            tf.text.languages[0].label = tf.text.languages[0].title;

            tf.text.languages[0].headers = new String[] { "Назва:" + tf.text.languages[0].title };
            texts.add(tf);
        } catch (Exception ex) {
            throw new RuntimeException("Error in " + file, ex);
        }
    }
}
