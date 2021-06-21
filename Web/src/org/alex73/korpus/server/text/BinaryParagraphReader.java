package org.alex73.korpus.server.text;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;

import org.alex73.korpus.text.structure.corpus.Paragraph;
import org.alex73.korpus.text.structure.corpus.Sentence;
import org.alex73.korpus.text.structure.corpus.Word;

public class BinaryParagraphReader {
    private ByteArrayInputStream bytes;
    private DataInputStream in;

    public BinaryParagraphReader(byte[] data) {
        bytes = new ByteArrayInputStream(data);
        in = new DataInputStream(bytes);
    }

    public Paragraph read() throws IOException {
        Paragraph p = new Paragraph();
        p.sentences = new Sentence[in.readShort()];
        for (int i = 0; i < p.sentences.length; i++) {
            p.sentences[i] = new Sentence();
            p.sentences[i].words = new Word[in.readShort()];
            for (int j = 0; j < p.sentences[i].words.length; j++) {
                Word w = new Word();
                w.source = readString();
                w.normalized = readString();
                w.lemmas = readString();
                w.tags = readString();
                w.tail = readString();
                p.sentences[i].words[j] = w;
            }
        }
        return p;
    }

    private String readString() throws IOException {
        String r = in.readUTF();
        if (r.isEmpty()) {
            r = null;
        }
        return r;
    }
}
