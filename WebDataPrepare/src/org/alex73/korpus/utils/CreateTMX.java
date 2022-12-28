package org.alex73.korpus.utils;

import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamWriter;

public class CreateTMX {
    public static void main(String[] args) throws Exception {
        for (Path f1 : Files.list(Paths.get("")).sorted().filter(p -> p.toString().endsWith(".rus.txt")).toList()) {
            System.out.println("Processing " + f1 + "...");
            Path f2 = Paths.get(f1.toString().replaceAll("\\.rus\\.txt$", ".bel.txt"));

            List<String> data1 = Files.readAllLines(f1);
            List<String> data2 = Files.readAllLines(f2);
            removeEmpty(data1);
            removeEmpty(data2);
            if (data1.size() != data2.size()) {
                Files.write(Paths.get("/tmp/f1.txt"), data1);
                Files.write(Paths.get("/tmp/f2.txt"), data2);
                throw new Exception("Different size: " + f1 + " / " + f2);
            }

            Path fo = Paths.get(f1.toString().replaceAll("\\.rus\\.txt$", ".tmx"));
            XMLOutputFactory output = XMLOutputFactory.newInstance();
            try (Writer o = Files.newBufferedWriter(fo)) {
                XMLStreamWriter writer = output.createXMLStreamWriter(o);
                writer.writeStartDocument("UTF-8", "1.0");
                writer.writeCharacters("\n");
                writer.writeStartElement("tmx");
                writer.writeAttribute("version", "1.4");

                writer.writeCharacters("\n  ");
                writer.writeStartElement("body");
                for (int i = 0; i < data1.size(); i++) {
                    writeTu(writer, data1.get(i), data2.get(i));
                }
                writer.writeCharacters("\n  ");
                writer.writeEndElement();

                writer.writeCharacters("\n");
                writer.writeEndElement();
                writer.writeEndDocument();

                writer.flush();
                writer.close();
            }
        }
    }

    static void writeTu(XMLStreamWriter writer, String rus, String bel) throws Exception {
        writer.writeCharacters("\n    ");
        writer.writeStartElement("tu");
        writer.writeCharacters("\n      ");
        writer.writeStartElement("tuv");
        writer.writeAttribute("xml:lang", "rus");
        writer.writeCharacters("\n        ");
        writer.writeStartElement("seg");
        writer.writeCharacters(rus);
        writer.writeEndElement();
        writer.writeCharacters("\n      ");
        writer.writeEndElement();
        writer.writeCharacters("\n      ");
        writer.writeStartElement("tuv");
        writer.writeAttribute("xml:lang", "bel");
        writer.writeCharacters("\n        ");
        writer.writeStartElement("seg");
        writer.writeCharacters(bel);
        writer.writeEndElement();
        writer.writeCharacters("\n      ");
        writer.writeEndElement();
        writer.writeCharacters("\n    ");
        writer.writeEndElement();
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
