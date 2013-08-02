
package alex73.corpus.paradigm;

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
 *           &lt;element ref="{}tag"/>
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
    "wOrTag"
})
@XmlRootElement(name = "s")
public class S {

    @XmlElements({
        @XmlElement(name = "w", type = W.class),
        @XmlElement(name = "tag", type = Tag.class)
    })
    protected List<Object> wOrTag;

    /**
     * Gets the value of the wOrTag property.
     * 
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the wOrTag property.
     * 
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getWOrTag().add(newItem);
     * </pre>
     * 
     * 
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link W }
     * {@link Tag }
     * 
     * 
     */
    public List<Object> getWOrTag() {
        if (wOrTag == null) {
            wOrTag = new ArrayList<Object>();
        }
        return this.wOrTag;
    }

}
