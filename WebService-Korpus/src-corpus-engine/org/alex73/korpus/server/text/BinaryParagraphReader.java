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

    public Paragraph[] read() throws IOException {
        Paragraph[] ps = new Paragraph[in.readShort()];
        for (int pi = 0; pi < ps.length; pi++) {
            Paragraph p = new Paragraph();
            ps[pi] = p;
            p.lang = in.readUTF();
            p.page = in.readUTF();
            if (p.page != null && p.page.isEmpty()) {
                p.page = null;
            }
            p.audioPreview = in.readUTF();
            if (p.audioPreview != null && p.audioPreview.isEmpty()) {
                p.audioPreview = null;
            }
            p.sentences = new Sentence[in.readShort()];
            for (int i = 0; i < p.sentences.length; i++) {
                p.sentences[i] = new Sentence();
                p.sentences[i].words = new Word[in.readShort()];
                for (int j = 0; j < p.sentences[i].words.length; j++) {
                    Word w = new Word();
                    w.word = readString();
                    w.tail = readString();
                    w.type = readWordOtherType();
                    p.sentences[i].words[j] = w;
                }
            }
        }
        return ps;
    }

    private Word.OtherType readWordOtherType() throws IOException {
        byte v = in.readByte();
        if (v == 0) {
            return null;
        } else {
            return Word.OtherType.values()[v - 1];
        }
    }

    private String readString() throws IOException {
        String r = in.readUTF();
        if (r.isEmpty()) {
            r = null;
        }
        return r;
    }
}
