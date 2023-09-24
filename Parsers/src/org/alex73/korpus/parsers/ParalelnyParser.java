package org.alex73.korpus.parsers;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.Collator;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.alex73.korpus.base.Ctf;
import org.alex73.korpus.parsers.utils.Output;

/**
 * Разбірае паралельныя тэксты заканадаўства.
 */
public class ParalelnyParser {
    static final Collator BE = Collator.getInstance(Locale.of("be"));

    public static void main(String[] args) throws Exception {
        if (args.length != 2) {
            System.err.println("ParalelnyParser <каталог з зыходнымі файламі> <файл каб захаваць падкорпус>");
            System.exit(1);
        }
        Path in = Path.of(args[0]);
        List<TF> texts = new ArrayList<>();
        for (Path f1 : Files.list(in).sorted().filter(p -> p.toString().endsWith(".rus.txt")).toList()) {
            Path f2 = Path.of(f1.toString().replaceAll("\\.rus\\.txt$", ".bel.txt"));
            TF tf = new TF();
            tf.file = f1.getFileName().toString().replaceAll("\\.rus\\.txt$", ".ctf");
            tf.text = parse(f1, f2);
            texts.add(tf);
        }

        Collections.sort(texts, (a, b) -> BE.compare(a.text.languages[1].title, b.text.languages[1].title));
        Path fo = Path.of(args[1]).resolve("paralelny.zip");
        try (Output os = new Output(fo)) {
            for (TF tf : texts) {
                os.write(tf.file, tf.text);
            }
        }
    }

    static class TF {
        String file;
        Ctf text;
    }

    public static Ctf parse(Path f1, Path f2) throws Exception {
        System.out.println("Чытаем " + f1 + "...");
        List<String> data1 = Files.readAllLines(f1);
        List<String> data2 = Files.readAllLines(f2);
        removeEmpty(data1);
        removeEmpty(data2);
        if (data1.size() != data2.size()) {
            Files.write(Paths.get("/tmp/f1.txt"), data1);
            Files.write(Paths.get("/tmp/f2.txt"), data2);
            throw new Exception("Розны памер: " + f1 + " / " + f2);
        }
        for (int i = 0; i < data1.size(); i++) {
            String s1 = data1.get(i);
            String s2 = data2.get(i);
            List<String> n1 = getNumbers(s1);
            List<String> n2 = getNumbers(s2);
            if (!n1.equals(n2)) {
                System.err.println("Няправільныя нумары:");
                System.err.println("    " + n1);
                System.err.println("    " + n2);
                System.err.println("    " + s1);
                System.err.println("    " + s2);
            }
        }

        Ctf result = new Ctf();
        result.languages[0].label = data1.get(0);
        result.languages[0].title = data1.get(0) + " / " + data2.get(1);
        result.languages[0].headers = new String[] { "Назва:" + data1.get(0), "Выданне:" + data1.get(1) };
        result.languages[1].label = data2.get(0);
        result.languages[1].title = data2.get(0) + " / " + data2.get(1);
        result.languages[1].headers = new String[] { "Назва:" + data2.get(0), "Выданне:" + data2.get(1) };

        result.languages = new Ctf.Language[2];
        result.languages[0] = new Ctf.Language();
        result.languages[0].lang = "rus";
        result.languages[0].pages = new Ctf.Page[1];
        result.languages[0].pages[0] = new Ctf.Page();
        result.languages[0].pages[0].paragraphs = data1.toArray(new String[0]);
        result.languages[1] = new Ctf.Language();
        result.languages[1].lang = "bel";
        result.languages[1].pages = new Ctf.Page[1];
        result.languages[1].pages[0] = new Ctf.Page();
        result.languages[1].pages[0].paragraphs = data2.toArray(new String[0]);

        return result;
    }

    static final Pattern RE_NUMBERS = Pattern.compile("[0-9]+");

    static List<String> getNumbers(String s) {
        List<String> allMatches = new ArrayList<String>();
        Matcher m = RE_NUMBERS.matcher(s);
        while (m.find()) {
            allMatches.add(m.group());
        }
        return allMatches;
    }

    static void removeEmpty(List<String> data) {
        for (int i = 0; i < data.size(); i++) {
            String s = data.get(i);
            s = s.trim();
            if (s.isEmpty()) {
                data.remove(i);
                i--;
                continue;
            }
            data.set(i, s);
        }
    }
}
