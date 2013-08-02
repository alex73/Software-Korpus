
package alex73.corpus.paradigm;

import javax.xml.bind.annotation.XmlEnum;
import javax.xml.bind.annotation.XmlEnumValue;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Java class for Type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * <p>
 * <pre>
 * &lt;simpleType name="Type">
 *   &lt;restriction base="{http://www.w3.org/2001/XMLSchema}string">
 *     &lt;enumeration value="nonstandard"/>
 *     &lt;enumeration value="potential"/>
 *     &lt;enumeration value="variant"/>
 *     &lt;enumeration value="numeral"/>
 *   &lt;/restriction>
 * &lt;/simpleType>
 * </pre>
 * 
 */
@XmlType(name = "Type")
@XmlEnum
public enum Type {


    /**
     * Нестандартная
     * 
     */
    @XmlEnumValue("nonstandard")
    NONSTANDARD("nonstandard"),

    /**
     * Патэнцыйная
     * 
     */
    @XmlEnumValue("potential")
    POTENTIAL("potential"),

    /**
     * Варыянт
     * 
     */
    @XmlEnumValue("variant")
    VARIANT("variant"),

    /**
     * з ліч. 2, 3, 
     * 
     */
    @XmlEnumValue("numeral")
    NUMERAL("numeral");
    private final String value;

    Type(String v) {
        value = v;
    }

    public String value() {
        return value;
    }

    public static Type fromValue(String v) {
        for (Type c: Type.values()) {
            if (c.value.equals(v)) {
                return c;
            }
        }
        throw new IllegalArgumentException(v);
    }

}
