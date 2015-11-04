package org.alex73.korpus.text.xml;

import javax.xml.bind.annotation.XmlEnum;
import javax.xml.bind.annotation.XmlEnumValue;
import javax.xml.bind.annotation.XmlType;

@XmlType(name = "OtherType")
@XmlEnum
public enum OtherType {

    @XmlEnumValue("other_language") OTHER_LANGUAGE("other_language"),

    @XmlEnumValue("number") NUMBER("number"),

    @XmlEnumValue("trasianka") TRASIANKA("trasianka"),

    @XmlEnumValue("dyjalekt") DYJALEKT("dyjalekt");

    private final String value;

    OtherType(String v) {
        value = v;
    }

    public String value() {
        return value;
    }

    public static OtherType fromValue(String v) {
        for (OtherType c : OtherType.values()) {
            if (c.value.equals(v)) {
                return c;
            }
        }
        throw new IllegalArgumentException(v);
    }
}
