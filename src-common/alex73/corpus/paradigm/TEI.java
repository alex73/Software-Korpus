
package alex73.corpus.paradigm;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
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
 *         &lt;element ref="{}teiHeader"/>
 *         &lt;element ref="{}text"/>
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
    "teiHeader",
    "text"
})
@XmlRootElement(name = "TEI")
public class TEI {

    @XmlElement(required = true)
    protected TeiHeader teiHeader;
    @XmlElement(required = true)
    protected Text text;

    /**
     * Gets the value of the teiHeader property.
     * 
     * @return
     *     possible object is
     *     {@link TeiHeader }
     *     
     */
    public TeiHeader getTeiHeader() {
        return teiHeader;
    }

    /**
     * Sets the value of the teiHeader property.
     * 
     * @param value
     *     allowed object is
     *     {@link TeiHeader }
     *     
     */
    public void setTeiHeader(TeiHeader value) {
        this.teiHeader = value;
    }

    /**
     * Gets the value of the text property.
     * 
     * @return
     *     possible object is
     *     {@link Text }
     *     
     */
    public Text getText() {
        return text;
    }

    /**
     * Sets the value of the text property.
     * 
     * @param value
     *     allowed object is
     *     {@link Text }
     *     
     */
    public void setText(Text value) {
        this.text = value;
    }

}
