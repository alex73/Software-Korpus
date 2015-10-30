package org.alex73.korpus.text;

import java.io.File;
import java.io.InputStream;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;

import org.alex73.korpus.text.xml.XMLText;

public class TextIO {
    public static JAXBContext CONTEXT;

    static {
        try {
            CONTEXT = JAXBContext.newInstance(XMLText.class);
        } catch (Exception ex) {
            throw new ExceptionInInitializerError(ex);
        }
    }
    

    public static XMLText parseXML(InputStream in) throws Exception {
        Unmarshaller unm = CONTEXT.createUnmarshaller();

        XMLText doc = (XMLText) unm.unmarshal(in);
        return doc;
    }

    public static void saveXML(File outFile, XMLText xml) throws Exception {
        Marshaller m = CONTEXT.createMarshaller();
        m.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
        m.marshal(xml, outFile);
    }
}
