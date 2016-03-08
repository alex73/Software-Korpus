package org.alex73.korpus.server.text;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;

import org.alex73.korpus.text.xml.O;
import org.alex73.korpus.text.xml.P;
import org.alex73.korpus.text.xml.S;
import org.alex73.korpus.text.xml.Se;
import org.alex73.korpus.text.xml.W;
import org.alex73.korpus.text.xml.Z;

public class BinaryParagraphReader {
    private ByteArrayInputStream bytes;
    private DataInputStream in;
    private P result;
    private Se se;

    public BinaryParagraphReader(byte[] data) {
        bytes = new ByteArrayInputStream(data);
        in = new DataInputStream(bytes);
    }

    public P read() throws IOException {
        result = new P();
        int pCount = in.readShort();
        for (int i = 0; i < pCount; i++) {
            se = new Se();
            int seCount = in.readShort();
            for (int j = 0; j < seCount; j++) {
                byte wt = in.readByte();
                switch (wt) {
                case 1:
                    readW();
                    break;
                case 2:
                    readS();
                    break;
                case 3:
                    readZ();
                    break;
                case 4:
                    readO();
                    break;
                default:
                    throw new RuntimeException("Unknown word type: " + wt);
                }
            }
            result.getSe().add(se);
        }
        if (bytes.available() != 0) {
            throw new RuntimeException("Wrong data");
        }
        return result;
    }

    private void readW() throws IOException {
        W w = new W();
        w.setValue(readString());
        w.setCat(readString());
        w.setLemma(readString());
        se.getWOrSOrZ().add(w);
    }

    private void readS() throws IOException {
        S s = new S();
        s.setChar(readString());
        se.getWOrSOrZ().add(s);
    }

    private void readZ() throws IOException {
        Z z = new Z();
        z.setValue(readString());
        z.setCat(readString());
        se.getWOrSOrZ().add(z);
    }

    private void readO() throws IOException {
        O o = new O();
        o.setValue(readString());
        se.getWOrSOrZ().add(o);
    }

    private String readString() throws IOException {
        String r = in.readUTF();
        if (r.isEmpty()) {
            r = null;
        }
        return r;
    }
}
