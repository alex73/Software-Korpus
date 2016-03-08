
package org.alex73.korpus.text.xml;

import java.util.ArrayList;
import java.util.List;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
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
 *       &lt;sequence>
 *         &lt;element ref="{}se" maxOccurs="unbounded" minOccurs="0"/>
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
    "se"
})
@XmlRootElement(name = "p")
public class P {

    protected List<Se> se;

    /**
     * Gets the value of the se property.
     * 
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the se property.
     * 
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getSe().add(newItem);
     * </pre>
     * 
     * 
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link Se }
     * 
     * 
     */
    public List<Se> getSe() {
        if (se == null) {
            se = new ArrayList<Se>();
        }
        return this.se;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof P) {
            P o = (P) obj;
            return o.se.equals(se);
        } else {
            return false;
        }
    }
}
