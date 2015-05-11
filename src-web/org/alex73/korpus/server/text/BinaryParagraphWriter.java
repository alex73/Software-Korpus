package org.alex73.korpus.server.text;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import alex73.corpus.text.InlineTag;
import alex73.corpus.text.O;
import alex73.corpus.text.P;
import alex73.corpus.text.S;
import alex73.corpus.text.Se;
import alex73.corpus.text.W;
import alex73.corpus.text.Z;

public class BinaryParagraphWriter {
    private ByteArrayOutputStream bytes = new ByteArrayOutputStream(8192);
    private DataOutputStream out = new DataOutputStream(bytes);
    private List<Object> words = new ArrayList<>();

    public byte[] write(P paragraph) {
        try {
            bytes.reset();

            if (paragraph.getSe().size() > Short.MAX_VALUE) {
                throw new RuntimeException("Too many sentences in paragraph: " + paragraph.getSe().size());
            }

            out.writeShort(paragraph.getSe().size());

            for (Se se : paragraph.getSe()) {
                words.clear();
                for (Object o : se.getWOrSOrZ()) {
                    if (o instanceof W) {
                        words.add(o);
                    } else if (o instanceof S) {
                        words.add(o);
                    } else if (o instanceof Z) {
                        words.add(o);
                    } else if (o instanceof O) {
                        words.add(o);
                    } else if (o instanceof InlineTag) {
                        // do not add
                    } else {
                        throw new RuntimeException("Unknown type:" + o.getClass());
                    }
                }
                if (words.size() > Short.MAX_VALUE) {
                    throw new RuntimeException("Too many words in sentence: " + words.size());
                }
                out.writeShort(words.size());
                for (Object o : words) {
                    if (o instanceof W) {
                        writeW((W) o);
                    } else if (o instanceof S) {
                        writeS((S) o);
                    } else if (o instanceof Z) {
                        writeZ((Z) o);
                    } else if (o instanceof O) {
                        writeO((O) o);
                    } else {
                        throw new RuntimeException("Unknown type:" + o.getClass());
                    }
                }
            }
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
        return bytes.toByteArray();
    }

    private void writeW(W w) throws IOException {
        writeString(w.getValue());
        writeString(w.getCat());
        writeString(w.getLemma());
    }

    private void writeS(S s) throws IOException {
        writeString(s.getChar());
    }

    private void writeZ(Z z) throws IOException {
        writeString(z.getValue());
        writeString(z.getCat());
    }
    private void writeO(O o) throws IOException {
        writeString(o.getValue());
    }

    private void writeString(String str) throws IOException {
        if (str == null) {
            str = "";
        }
        out.writeUTF(str);
    }
}
