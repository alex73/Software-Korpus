
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
    "pOrTagOrPoetry"
})
@XmlRootElement(name = "Content")
public class Content {

    @XmlElements({
        @XmlElement(name = "p", type = P.class),
        @XmlElement(name = "Tag", type = Tag.class),
        @XmlElement(name = "Poetry", type = Poetry.class)
    })
    protected List<Object> pOrTagOrPoetry;

    public List<Object> getPOrTagOrPoetry() {
        if (pOrTagOrPoetry == null) {
            pOrTagOrPoetry = new ArrayList<Object>();
        }
        return this.pOrTagOrPoetry;
    }
}
