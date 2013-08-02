
package alex73.corpus.paradigm;

import javax.xml.bind.annotation.XmlEnum;
import javax.xml.bind.annotation.XmlEnumValue;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Java class for Options.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * <p>
 * <pre>
 * &lt;simpleType name="Options">
 *   &lt;restriction base="{http://www.w3.org/2001/XMLSchema}string">
 *     &lt;enumeration value="usually_plurals"/>
 *   &lt;/restriction>
 * &lt;/simpleType>
 * </pre>
 * 
 */
@XmlType(name = "Options")
@XmlEnum
public enum Options {


    /**
     * Звычайна у множным ліку
     * 
     */
    @XmlEnumValue("usually_plurals")
    USUALLY_PLURALS("usually_plurals");
    private final String value;

    Options(String v) {
        value = v;
    }

    public String value() {
        return value;
    }

    public static Options fromValue(String v) {
        for (Options c: Options.values()) {
            if (c.value.equals(v)) {
                return c;
            }
        }
        throw new IllegalArgumentException(v);
    }

}
