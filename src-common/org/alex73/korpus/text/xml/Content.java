
package org.alex73.korpus.text.xml;

import java.util.ArrayList;
import java.util.List;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElements;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Java class for anonymous complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType>
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence maxOccurs="unbounded" minOccurs="0">
 *         &lt;choice>
 *           &lt;element ref="{}p"/>
 *           &lt;element ref="{}Tag"/>
 *         &lt;/choice>
 *       &lt;/sequence>
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "", propOrder = {
    "pOrTag"
})
@XmlRootElement(name = "Content")
public class Content {

    @XmlElements({
        @XmlElement(name = "p", type = P.class),
        @XmlElement(name = "Tag", type = Tag.class)
    })
    protected List<Object> pOrTag;

    /**
     * Gets the value of the pOrTag property.
     * 
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the pOrTag property.
     * 
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getPOrTag().add(newItem);
     * </pre>
     * 
     * 
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link P }
     * {@link Tag }
     * 
     * 
     */
    public List<Object> getPOrTag() {
        if (pOrTag == null) {
            pOrTag = new ArrayList<Object>();
        }
        return this.pOrTag;
    }

}
