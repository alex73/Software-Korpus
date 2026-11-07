package org.alex73.korpus.parsers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.BufferedOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.alex73.korpus.base.Ctf;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.github.luben.zstd.ZstdOutputStream;
import com.google.gson.Gson;

public class HpltParserTest {

    @Test
    public void testFixText() {
        assertEquals("Тэкст &amp; &lt;тэг&gt;", HpltParser.fixText("  Тэкст & <тэг>  "));
        assertEquals("Слова з прабелам", HpltParser.fixText("Слова\u00A0з прабелам"));
        assertEquals("", HpltParser.fixText("   "));
        assertEquals("", HpltParser.fixText(null));
    }

    @Test
    public void testExtractParagraphs() {
        String text = "Першы абзац\n\nДругі абзац з <інфармацыяй>\n  \nТрэці абзац\n";
        List<String> paragraphs = HpltParser.extractParagraphs(text);
        assertEquals(3, paragraphs.size());
        assertEquals("Першы абзац", paragraphs.get(0));
        assertEquals("Другі абзац з &lt;інфармацыяй&gt;", paragraphs.get(1));
        assertEquals("Трэці абзац", paragraphs.get(2));
    }

    @Test
    public void testDetermineTitle() {
        assertEquals("Кароткі загаловак", HpltParser.determineTitle("Кароткі загаловак", "https://example.com/p1", "id1"));

        String longText = "Гэта вельмі доўгі загаловак, які павінен быць скарочаны, таму што ён перавышае максімальна дапушчальную даўжыню ў сто дваццаць сімвалаў і патрабуе шматкроп'я на канцы.";
        String title = HpltParser.determineTitle(longText, "https://example.com/p1", "id1");
        assertTrue(title.length() <= 125);
        assertTrue(title.endsWith("..."));

        assertEquals("https://example.com/p1", HpltParser.determineTitle("", "https://example.com/p1", "id1"));
        assertEquals("id1", HpltParser.determineTitle(null, null, "id1"));
    }

    @Test
    public void testExtractHost() {
        assertEquals("example.com", HpltParser.extractHost("https://www.example.com/be/article/123"));
        assertEquals("news.by", HpltParser.extractHost("http://news.by/page"));
        assertEquals("hplt", HpltParser.extractHost("not-a-valid-url:::"));
        assertEquals("hplt", HpltParser.extractHost(null));
    }

    @Test
    public void testExtractDate() {
        assertEquals("2019-10-25", HpltParser.extractDate("2019-10-25T08:07:02Z"));
        assertEquals("2022-05-29", HpltParser.extractDate("2022-05-29"));
        assertEquals("2024", HpltParser.extractDate("2024"));
        assertNull(HpltParser.extractDate(null));
        assertNull(HpltParser.extractDate("няма даты"));
    }

    @Test
    public void testParseDoc() {
        HpltParser parser = new HpltParser();
        HpltParser.HpltDoc doc = new HpltParser.HpltDoc();
        doc.id = "doc123";
        doc.u = "https://kresy.pl/be/art1";
        doc.ts = "2020-04-15T12:00:00Z";
        doc.text = "Галоўны загаловак навіны\nТэкст навіны з падрабязнасцямі.\nЯшчэ адзін сказ.";

        Ctf ctf = parser.parseDoc(doc);
        assertNotNull(ctf);
        assertEquals(1, ctf.languages.length);

        Ctf.Language la = ctf.languages[0];
        assertEquals("bel", la.lang);
        assertEquals("Галоўны загаловак навіны", la.title);
        assertEquals("kresy.pl", la.label);
        assertEquals("kresy.pl", la.sourceName);
        assertEquals("2020-04-15", la.publicationTime);

        assertNotNull(la.headers);
        assertEquals("Крыніца:HPLT v3", la.headers[0]);
        assertEquals("URL:https://kresy.pl/be/art1", la.headers[1]);
        assertEquals("Назва:Галоўны загаловак навіны", la.headers[2]);
        assertEquals("Час публікацыі:2020-04-15", la.headers[3]);
        assertEquals("ID:doc123", la.headers[4]);

        assertEquals(1, la.pages.length);
        assertEquals(3, la.pages[0].paragraphs.length);
        assertEquals("Галоўны загаловак навіны", la.pages[0].paragraphs[0]);
        assertEquals("Тэкст навіны з падрабязнасцямі.", la.pages[0].paragraphs[1]);
        assertEquals("Яшчэ адзін сказ.", la.pages[0].paragraphs[2]);
    }

    @Test
    public void testFullRunOnZstFile(@TempDir Path tempDir) throws Exception {
        Path inputDir = tempDir.resolve("in");
        Files.createDirectories(inputDir);
        Path zstFile = inputDir.resolve("10_1.jsonl.zst");

        String json1 = "{\"id\":\"id1\",\"u\":\"https://site1.by/p1\",\"ts\":\"2021-01-01T00:00:00Z\",\"text\":\"Артыкул 1\\nЗмест артыкула 1.\"}";
        String json2 = "{\"id\":\"id2\",\"u\":\"https://site2.com/p2\",\"ts\":\"2022-02-02T00:00:00Z\",\"text\":\"Артыкул 2\\nЗмест артыкула 2.\"}";

        try (OutputStream fos = Files.newOutputStream(zstFile);
             BufferedOutputStream bos = new BufferedOutputStream(fos);
             ZstdOutputStream zos = new ZstdOutputStream(bos)) {
            zos.write((json1 + "\n" + json2 + "\n").getBytes(StandardCharsets.UTF_8));
        }

        Path outputDir = tempDir.resolve("out");
        Path zipFile = outputDir.resolve("09.hplt.zip");

        HpltParser parser = new HpltParser();
        parser.run(inputDir, zipFile);

        assertEquals(2, parser.getArticlesCount());
        assertTrue(Files.exists(zipFile));

        try (ZipFile zf = new ZipFile(zipFile.toFile())) {
            assertEquals(2, zf.size());
            ZipEntry entry1 = zf.getEntry("hplt-00000/00000001.ctf");
            assertNotNull(entry1);
            ZipEntry entry2 = zf.getEntry("hplt-00000/00000002.ctf");
            assertNotNull(entry2);

            try (InputStream is = zf.getInputStream(entry1)) {
                String content = new String(is.readAllBytes(), StandardCharsets.UTF_8);
                Ctf ctf = new Gson().fromJson(content, Ctf.class);
                assertEquals("Артыкул 1", ctf.languages[0].title);
                assertEquals("site1.by", ctf.languages[0].label);
                assertEquals(2, ctf.languages[0].pages[0].paragraphs.length);
            }
        }
    }
}
