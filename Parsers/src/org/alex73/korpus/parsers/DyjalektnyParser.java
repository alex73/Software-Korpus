package org.alex73.korpus.parsers;

import java.io.ByteArrayInputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.Collator;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.alex73.korpus.base.Ctf;
import org.alex73.korpus.parsers.utils.Output;
import org.alex73.korpus.parsers.utils.TextFileHeaders;
import org.alex73.korpus.parsers.utils.TextFileParser;

public class DyjalektnyParser {
    static final Collator BE = Collator.getInstance(Locale.of("be"));
    static List<TF> texts = new ArrayList<>();

    public static void main(String[] args) throws Exception {
        if (args.length != 2) {
            System.err.println("DyjalektnyParser <каталог з зыходнымі файламі> <каталог каб захаваць падкорпус>");
            System.exit(1);
        }
        Path in = Path.of(args[0]);
        for (Path f1 : Files.list(in).sorted().filter(p -> p.toString().endsWith(".text")).toList()) {
            parse(f1);
        }

        Collections.sort(texts, (o1, o2) -> {
            int c = BE.compare(o1.rajon, o2.rajon);
            if (c == 0) {
                c = BE.compare(o1.krynica, o2.krynica);
            }
            if (c == 0) {
                c = Integer.compare(o1.indexInFile, o2.indexInFile);
            }
            return c;
        });
        Path fo = Path.of(args[1], "08.dyjalektny.zip");
        try (Output os = new Output(fo)) {
            int idx = 0;
            for (TF tf : texts) {
                String file = tf.filePrefix + "/" + new DecimalFormat("0000").format(++idx) + ".ctf";
                os.write(file, tf.text);
            }
        }
    }

    public static void parse(Path file) throws Exception {
        int indexInFile = 0;
        System.out.println("Чытаем " + file + "...");
        byte[] data = Files.readAllBytes(file);
        try {
            TextFileParser allDocs = new TextFileParser(new ByteArrayInputStream(data));
            for (TextFileParser.OneText doc : allDocs.texts) {
                TF result = new TF();
                result.text.setPages("bel", doc.pages);
                result.filePrefix = file.getFileName().toString().replaceAll("\\.text$", "");
                result.indexInFile = ++indexInFile;
                result.rajon = doc.headers.get("Раён");
                result.krynica = doc.headers.get("Крыніца");
                if (result.rajon == null) {
                    throw new RuntimeException("Нявызначаны загаловак 'Раён' у " + file);
                }
                if (result.krynica == null) {
                    throw new RuntimeException("Нявызначаны загаловак 'Крыніца' у " + file);
                }

                String s;
                if ((s = doc.headers.get("Збіральнік")) != null) {
                    result.text.languages[0].authors = TextFileHeaders.splitAndTrim(s);
                }
                result.text.languages[0].creationTime = doc.headers.get("Год запісу");
                addHeaders(result.text, doc.headers.getAll());
                texts.add(result);
            }
        } catch (Exception ex) {
            throw new RuntimeException("Error in " + file, ex);
        }
    }

    static class TF {
        String filePrefix;
        int indexInFile;
        String rajon;
        String krynica;
        Ctf text = new Ctf();
    }

    static void addHeaders(Ctf text, Map<String, String> headers) {
        text.languages[0].label = headers.get("Раён");
        text.languages[0].title = headers.get("Раён");
        List<String> s = new ArrayList<>();
        TextFileHeaders.addHeader(s, "Вобласць", headers.get("Вобласць"));
        TextFileHeaders.addHeader(s, "Раён", headers.get("Раён"));
        TextFileHeaders.addHeader(s, "Месца", headers.get("Месца"));
        TextFileHeaders.addHeader(s, "Дыялект", headers.get("Дыялект, група гаворак"));
        TextFileHeaders.addHeader(s, "Збіральнік", headers.get("Збіральнік"));
        TextFileHeaders.addHeader(s, "Год запісу", headers.get("Год запісу"));
        TextFileHeaders.addHeader(s, "Тэкст расшыфраваў", headers.get("Тэкст расшыфраваў"));
        TextFileHeaders.addHeader(s, "Год нараджэння інфарманта", headers.get("Інфармант-год нараджэння"));
        TextFileHeaders.addHeader(s, "Месца нараджэння інфарманта", headers.get("Інфармант-месца нараджэння"));
        TextFileHeaders.addHeader(s, "Дзе жыў інфармант", headers.get("Інфармант-дзе жыў"));
        TextFileHeaders.addHeader(s, "Пол інфарманта", headers.get("Інфармант-пол"));
        TextFileHeaders.addHeader(s, "Нацыянальнасць інфарманта", headers.get("Інфармант-нацыянальнасць"));
        TextFileHeaders.addHeader(s, "Веравызнанне інфарманта", headers.get("Інфармант-веравызнанне"));
        TextFileHeaders.addHeader(s, "Адукацыя інфарманта", headers.get("Інфармант-адукацыя"));
        TextFileHeaders.addHeader(s, "Паходжанне бацькоў інфарманта", headers.get("Інфармант-паходжанне бацькоў"));
        TextFileHeaders.addHeader(s, "Тып тэксту інфарманта", headers.get("Тып тэксту"));
        TextFileHeaders.addHeader(s, "Крыніца", headers.get("Крыніца"));
        text.languages[0].headers = s.toArray(new String[0]);
    }
}
