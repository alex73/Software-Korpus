
package alex73.corpus.paradigm;

import java.util.ArrayList;
import java.util.List;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
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
 *         &lt;element name="Form" maxOccurs="unbounded" minOccurs="0">
 *           &lt;complexType>
 *             &lt;complexContent>
 *               &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *                 &lt;attribute name="Tag" use="required" type="{http://www.w3.org/2001/XMLSchema}string" />
 *                 &lt;attribute name="pdgId" use="required" type="{http://www.w3.org/2001/XMLSchema}string" />
 *               &lt;/restriction>
 *             &lt;/complexContent>
 *           &lt;/complexType>
 *         &lt;/element>
 *       &lt;/sequence>
 *       &lt;attribute name="pdgId" type="{http://www.w3.org/2001/XMLSchema}string" />
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "", propOrder = {
    "form"
})
@XmlRootElement(name = "Cluster")
public class Cluster {

    @XmlElement(name = "Form")
    protected List<Cluster.Form> form;
    @XmlAttribute(name = "pdgId")
    protected String pdgId;

    /**
     * Gets the value of the form property.
     * 
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the form property.
     * 
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getForm().add(newItem);
     * </pre>
     * 
     * 
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link Cluster.Form }
     * 
     * 
     */
    public List<Cluster.Form> getForm() {
        if (form == null) {
            form = new ArrayList<Cluster.Form>();
        }
        return this.form;
    }

    /**
     * Gets the value of the pdgId property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getPdgId() {
        return pdgId;
    }

    /**
     * Sets the value of the pdgId property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setPdgId(String value) {
        this.pdgId = value;
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
     *       &lt;attribute name="Tag" use="required" type="{http://www.w3.org/2001/XMLSchema}string" />
     *       &lt;attribute name="pdgId" use="required" type="{http://www.w3.org/2001/XMLSchema}string" />
     *     &lt;/restriction>
     *   &lt;/complexContent>
     * &lt;/complexType>
     * </pre>
     * 
     * 
     */
    @XmlAccessorType(XmlAccessType.FIELD)
    @XmlType(name = "")
    public static class Form {

        @XmlAttribute(name = "Tag", required = true)
        protected String tag;
        @XmlAttribute(name = "pdgId", required = true)
        protected String pdgId;

        /**
         * Gets the value of the tag property.
         * 
         * @return
         *     possible object is
         *     {@link String }
         *     
         */
        public String getTag() {
            return tag;
        }

        /**
         * Sets the value of the tag property.
         * 
         * @param value
         *     allowed object is
         *     {@link String }
         *     
         */
        public void setTag(String value) {
            this.tag = value;
        }

        /**
         * Gets the value of the pdgId property.
         * 
         * @return
         *     possible object is
         *     {@link String }
         *     
         */
        public String getPdgId() {
            return pdgId;
        }

        /**
         * Sets the value of the pdgId property.
         * 
         * @param value
         *     allowed object is
         *     {@link String }
         *     
         */
        public void setPdgId(String value) {
            this.pdgId = value;
        }

    }

}
