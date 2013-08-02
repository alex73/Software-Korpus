
package alex73.corpus.paradigm;

import java.util.ArrayList;
import java.util.List;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElements;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Java class for Part complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="Part">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;choice maxOccurs="unbounded" minOccurs="0">
 *         &lt;element name="head">
 *           &lt;complexType>
 *             &lt;complexContent>
 *               &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *                 &lt;sequence>
 *                   &lt;element ref="{}s" maxOccurs="unbounded"/>
 *                 &lt;/sequence>
 *               &lt;/restriction>
 *             &lt;/complexContent>
 *           &lt;/complexType>
 *         &lt;/element>
 *         &lt;element ref="{}p" maxOccurs="unbounded"/>
 *         &lt;element name="div" type="{}Part"/>
 *       &lt;/choice>
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "Part", propOrder = {
    "headOrPOrDiv"
})
public class Part {

    @XmlElements({
        @XmlElement(name = "head", type = Part.Head.class),
        @XmlElement(name = "p", type = P.class),
        @XmlElement(name = "div", type = Part.class)
    })
    protected List<Object> headOrPOrDiv;

    /**
     * Gets the value of the headOrPOrDiv property.
     * 
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the headOrPOrDiv property.
     * 
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getHeadOrPOrDiv().add(newItem);
     * </pre>
     * 
     * 
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link Part.Head }
     * {@link P }
     * {@link Part }
     * 
     * 
     */
    public List<Object> getHeadOrPOrDiv() {
        if (headOrPOrDiv == null) {
            headOrPOrDiv = new ArrayList<Object>();
        }
        return this.headOrPOrDiv;
    }


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
     *         &lt;element ref="{}s" maxOccurs="unbounded"/>
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
        "s"
    })
    public static class Head {

        @XmlElement(required = true)
        protected List<S> s;

        /**
         * Gets the value of the s property.
         * 
         * <p>
         * This accessor method returns a reference to the live list,
         * not a snapshot. Therefore any modification you make to the
         * returned list will be present inside the JAXB object.
         * This is why there is not a <CODE>set</CODE> method for the s property.
         * 
         * <p>
         * For example, to add a new item, do as follows:
         * <pre>
         *    getS().add(newItem);
         * </pre>
         * 
         * 
         * <p>
         * Objects of the following type(s) are allowed in the list
         * {@link S }
         * 
         * 
         */
        public List<S> getS() {
            if (s == null) {
                s = new ArrayList<S>();
            }
            return this.s;
        }

    }

}
