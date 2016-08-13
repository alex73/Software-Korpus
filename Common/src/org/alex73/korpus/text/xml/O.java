
package org.alex73.korpus.text.xml;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;
import javax.xml.bind.annotation.XmlValue;

/**
 * <p>
 * Java class for anonymous complex type.
 * 
 * <p>
 * The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType>
 *   &lt;simpleContent>
 *     &lt;extension base="&lt;http://www.w3.org/2001/XMLSchema>string">
 *     &lt;/extension>
 *   &lt;/simpleContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "", propOrder = { "value" })
@XmlRootElement(name = "o")
public class O implements ITextLineElement {

    public O() {
    }

    public O(OtherType type, String value) {
        this.type = type;
        this.value = value;
    }

    @XmlValue
    protected String value;

    @XmlAttribute(name = "type")
    protected OtherType type;

    /**
     * Gets the value of the value property.
     * 
     * @return possible object is {@link String }
     * 
     */
    public String getValue() {
        return value;
    }

    /**
     * Sets the value of the value property.
     * 
     * @param value
     *            allowed object is {@link String }
     * 
     */
    public void setValue(String value) {
        this.value = value;
    }

    public OtherType getType() {
        return type;
    }

    public void setType(OtherType type) {
        this.type = type;
    }

    @Override
    public String getText() {
        return value;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof O) {
            O o = (O) obj;
            return o.value.equals(value) && o.type.equals(type);
        } else {
            return false;
        }
    }

    @Override
    public String toString() {
        return "o(" + value + ")";
    }
}
