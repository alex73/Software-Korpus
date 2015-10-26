
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
 *           &lt;element ref="{}w"/>
 *           &lt;element ref="{}s"/>
 *           &lt;element ref="{}z"/>
 *           &lt;element ref="{}o"/>
 *           &lt;element ref="{}inlineTag"/>
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
    "wOrSOrZ"
})
@XmlRootElement(name = "se")
public class Se {

    @XmlElements({
        @XmlElement(name = "w", type = W.class),
        @XmlElement(name = "s", type = S.class),
        @XmlElement(name = "z", type = Z.class),
        @XmlElement(name = "o", type = O.class),
        @XmlElement(name = "inlineTag", type = InlineTag.class)
    })
    protected List<ITextLineElement> wOrSOrZ;

    /**
     * Gets the value of the wOrSOrZ property.
     * 
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the wOrSOrZ property.
     * 
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getWOrSOrZ().add(newItem);
     * </pre>
     * 
     * 
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link W }
     * {@link S }
     * {@link Z }
     * {@link O }
     * {@link InlineTag }
     * 
     * 
     */
    public List<ITextLineElement> getWOrSOrZ() {
        if (wOrSOrZ == null) {
            wOrSOrZ = new ArrayList<ITextLineElement>();
        }
        return this.wOrSOrZ;
    }

}
