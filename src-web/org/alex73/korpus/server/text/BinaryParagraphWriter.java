package org.alex73.korpus.server.text;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;

import alex73.corpus.text.InlineTag;
import alex73.corpus.text.P;
import alex73.corpus.text.S;
import alex73.corpus.text.Se;
import alex73.corpus.text.W;
import alex73.corpus.text.Z;

public class BinaryParagraphWriter {
    private static Charset UTF8 = Charset.forName("UTF-8");
    private ByteArrayOutputStream bytes = new ByteArrayOutputStream(8192);
    private ByteArrayOutputStream wordBytes = new ByteArrayOutputStream(8192);
    private List<Object> words = new ArrayList<>();

    public void write(P paragraph) {
        try {
            bytes.reset();

            if (paragraph.getSe().size() > 255) {
                throw new RuntimeException("Too many sentences in paragraph: " + paragraph.getSe().size());
            }

            bytes.write(paragraph.getSe().size());
            for (Se se : paragraph.getSe()) {
                words.clear();
                for (Object o : se.getWOrSOrZ()) {
                    if (o instanceof W) {
                        words.add(o);
                    } else if (o instanceof S) {
                        words.add(o);
                    } else if (o instanceof Z) {
                        words.add(o);
                    } else if (o instanceof InlineTag) {
                        // do not add
                    } else {
                        throw new RuntimeException("Unknown type:" + o.getClass());
                    }
                }
                if (words.size() > 255) {
                    throw new RuntimeException("Too many words in sentence: " + words.size());
                }
                bytes.write(words.size());
                for (Object o : words) {
                    if (o instanceof W) {
                        writeW((W) o);
                    } else if (o instanceof S) {
                        writeS((S) o);
                    } else if (o instanceof Z) {
                        writeZ((Z) o);
                    } else {
                        throw new RuntimeException("Unknown type:" + o.getClass());
                    }
                }
            }
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }

    private void writeW(W w) throws IOException {
        wordBytes.reset();
        wordBytes.write(w.getValue().getBytes(UTF8));
    }

    private void writeS(S s) throws IOException {

    }

    private void writeZ(Z z) throws IOException {

    }
}
