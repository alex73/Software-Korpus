
package org.alex73.korpus.text.xml;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;
import javax.xml.bind.annotation.XmlValue;


/**
 * <p>Java class for anonymous complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType>
 *   &lt;simpleContent>
 *     &lt;extension base="&lt;http://www.w3.org/2001/XMLSchema>string">
 *       &lt;attribute name="cat" use="required" type="{http://www.w3.org/2001/XMLSchema}string" />
 *       &lt;attribute name="lemma" use="required" type="{http://www.w3.org/2001/XMLSchema}string" />
 *       &lt;attribute name="manual" type="{http://www.w3.org/2001/XMLSchema}boolean" default="false" />
 *     &lt;/extension>
 *   &lt;/simpleContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "", propOrder = {
    "value"
})
@XmlRootElement(name = "w")
public class W implements ITextLineElement {

    @XmlValue
    protected String value;
    @XmlAttribute(name = "cat", required = true)
    protected String cat;
    @XmlAttribute(name = "lemma", required = true)
    protected String lemma;
    @XmlAttribute(name = "manual")
    protected Boolean manual;

    /**
     * Gets the value of the value property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getValue() {
        return value;
    }

    /**
     * Sets the value of the value property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setValue(String value) {
        this.value = value;
    }

    /**
     * Gets the value of the cat property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getCat() {
        return cat;
    }

    /**
     * Sets the value of the cat property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setCat(String value) {
        this.cat = value;
    }

    /**
     * Gets the value of the lemma property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getLemma() {
        return lemma;
    }

    /**
     * Sets the value of the lemma property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setLemma(String value) {
        this.lemma = value;
    }

    /**
     * Gets the value of the manual property.
     * 
     * @return
     *     possible object is
     *     {@link Boolean }
     *     
     */
    public boolean isManual() {
        if (manual == null) {
            return false;
        } else {
            return manual;
        }
    }

    /**
     * Sets the value of the manual property.
     * 
     * @param value
     *     allowed object is
     *     {@link Boolean }
     *     
     */
    public void setManual(Boolean value) {
        this.manual = value;
    }

    @Override
    public String getText() {
        return value;
    }
}
