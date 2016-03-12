
package org.alex73.korpus.text.xml;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;
import javax.xml.bind.annotation.XmlValue;

import org.apache.commons.lang.StringUtils;

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
 *       &lt;attribute name="cat" use="required" type="{http://www.w3.org/2001/XMLSchema}string" />
 *     &lt;/extension>
 *   &lt;/simpleContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "", propOrder = { "value" })
@XmlRootElement(name = "z")
public class Z implements ITextLineElement {

    public Z() {
    }

    public Z(String v) {
        value = v;
    }

    public Z(String v, String c) {
        value = v;
        cat = c;
    }

    @XmlValue
    protected String value;
    @XmlAttribute(name = "cat", required = true)
    protected String cat;

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

    /**
     * Gets the value of the cat property.
     * 
     * @return possible object is {@link String }
     * 
     */
    public String getCat() {
        return cat;
    }

    /**
     * Sets the value of the cat property.
     * 
     * @param value
     *            allowed object is {@link String }
     * 
     */
    public void setCat(String value) {
        this.cat = value;
    }

    @Override
    public String getText() {
        return value;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof Z) {
            Z o = (Z) obj;
            return StringUtils.equals(o.value, value) && StringUtils.equals(o.cat, cat);
        } else {
            return false;
        }
    }

    @Override
    public String toString() {
        return "Z(" + value + "/" + cat + ")";
    }
}