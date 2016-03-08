
package org.alex73.korpus.text.xml;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

/**
 * <p>
 * Java class for anonymous complex type.
 * 
 * <p>
 * The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType>
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;attribute name="char" type="{}S1" default=" " />
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "")
@XmlRootElement(name = "s")
public class S implements ITextLineElement {

    public S() {
    }

    public S(char s) {
        _char = Character.toString(s);
    }

    public S(String s) {
        _char = s;
    }

    @XmlAttribute(name = "char")
    protected String _char;

    /**
     * Gets the value of the char property.
     * 
     * @return possible object is {@link String }
     * 
     */
    public String getChar() {
        if (_char == null) {
            return " ";
        } else {
            return _char;
        }
    }

    /**
     * Sets the value of the char property.
     * 
     * @param value
     *            allowed object is {@link String }
     * 
     */
    public void setChar(String value) {
        this._char = value;
    }

    @Override
    public String getText() {
        return _char;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof S) {
            S o = (S) obj;
            return o._char.equals(_char);
        } else {
            return false;
        }
    }

    @Override
    public String toString() {
        return "S(" + _char + ")";
    }
}
