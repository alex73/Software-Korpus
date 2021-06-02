package org.alex73.korpus.server.text;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import org.alex73.korpus.text.structure.corpus.Paragraph;
import org.alex73.korpus.text.structure.corpus.Sentence;
import org.alex73.korpus.text.structure.corpus.Word;

public class BinaryParagraphWriter {
    private ByteArrayOutputStream bytes = new ByteArrayOutputStream(1024);
    private DataOutputStream out = new DataOutputStream(bytes);

    public byte[] write(Paragraph p) {
        try {
            bytes.reset();

            checkWriteShort(p.sentences.length, "Too many sentences in paragraph: ");
            for (Sentence se : p.sentences) {
                checkWriteShort(se.words.length, "Too many words in sentence: ");
                for (Word w : se.words) {
                    writeString(w.lightNormalized);
                    writeString(w.lemmas);
                    writeString(w.tags);
                    writeString(w.tail);
                }
            }

        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
        return bytes.toByteArray();
    }

    private void checkWriteShort(int value, String error) throws IOException {
        if (value > Short.MAX_VALUE) {
            throw new RuntimeException(error + value);
        }
        out.writeShort(value);
    }

    private void writeString(String str) throws IOException {
        if (str == null) {
            str = "";
        }
        out.writeUTF(str);
    }
}
