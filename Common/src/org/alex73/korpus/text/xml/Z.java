
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
 * The following schema fragment specifies the expected content contained within
 * this class.
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
@XmlType(name = "")
@XmlRootElement(name = "z")
public class Z implements ITextLineElement {

    public Z() {
    }

    public Z(char s) {
        _char = Character.toString(s);
    }

    public Z(String s) {
        _char = s;
    }

    @XmlAttribute(name = "char")
    protected String _char;

    /**
     * Gets the value of the value property.
     * 
     * @return possible object is {@link String }
     * 
     */
    public String getChar() {
        return _char;
    }

    public void setChar(String c) {
        this._char = c;
    }

    @Override
    public String getText() {
        return _char;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof Z) {
            Z o = (Z) obj;
            return o._char == _char;
        } else {
            return false;
        }
    }

    @Override
    public String toString() {
        return "Z(" + _char + ")";
    }
}
