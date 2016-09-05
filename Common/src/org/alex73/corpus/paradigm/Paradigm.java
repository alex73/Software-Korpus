
package org.alex73.corpus.paradigm;

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
 *         &lt;element name="e" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/>
 *         &lt;element name="Note" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/>
 *         &lt;element ref="{}Slounik" maxOccurs="unbounded" minOccurs="0"/>
 *         &lt;element ref="{}Variant" maxOccurs="unbounded" minOccurs="0"/>
 *       &lt;/sequence>
 *       &lt;attribute name="pdgId" use="required" type="{http://www.w3.org/2001/XMLSchema}int" />
 *       &lt;attribute name="lemma" use="required" type="{http://www.w3.org/2001/XMLSchema}string" />
 *       &lt;attribute name="tag" use="required" type="{http://www.w3.org/2001/XMLSchema}string" />
 *       &lt;attribute name="theme" type="{http://www.w3.org/2001/XMLSchema}string" />
 *       &lt;attribute name="govern" type="{http://www.w3.org/2001/XMLSchema}string" />
 *       &lt;attribute name="type" type="{}Type" />
 *       &lt;attribute name="marked" type="{http://www.w3.org/2001/XMLSchema}string" />
 *       &lt;attribute name="options" type="{}ParadigmOptions" />
 *       &lt;attribute name="todo" type="{http://www.w3.org/2001/XMLSchema}string" />
 *       &lt;attribute name="comment" type="{http://www.w3.org/2001/XMLSchema}string" />
 *       &lt;attribute name="meaning" type="{http://www.w3.org/2001/XMLSchema}string" />
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "", propOrder = {
    "e",
    "note",
    "slounik",
    "variant"
})
@XmlRootElement(name = "Paradigm")
public class Paradigm {

    protected String e;
    @XmlElement(name = "Note")
    protected String note;
    @XmlElement(name = "Slounik")
    protected List<Slounik> slounik;
    @XmlElement(name = "Variant")
    protected List<Variant> variant;
    @XmlAttribute(name = "pdgId", required = true)
    protected int pdgId;
    @XmlAttribute(name = "lemma", required = true)
    protected String lemma;
    @XmlAttribute(name = "tag", required = true)
    protected String tag;
    @XmlAttribute(name = "theme")
    protected String theme;
    @XmlAttribute(name = "govern")
    protected String govern;
    @XmlAttribute(name = "type")
    protected Type type;
    @XmlAttribute(name = "marked")
    protected String marked;
    @XmlAttribute(name = "options")
    protected ParadigmOptions options;
    @XmlAttribute(name = "todo")
    protected String todo;
    @XmlAttribute(name = "comment")
    protected String comment;
    @XmlAttribute(name = "meaning")
    protected String meaning;

    /**
     * Gets the value of the e property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getE() {
        return e;
    }

    /**
     * Sets the value of the e property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setE(String value) {
        this.e = value;
    }

    /**
     * Gets the value of the note property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getNote() {
        return note;
    }

    /**
     * Sets the value of the note property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setNote(String value) {
        this.note = value;
    }

    /**
     * Gets the value of the slounik property.
     * 
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the slounik property.
     * 
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getSlounik().add(newItem);
     * </pre>
     * 
     * 
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link Slounik }
     * 
     * 
     */
    public List<Slounik> getSlounik() {
        if (slounik == null) {
            slounik = new ArrayList<Slounik>();
        }
        return this.slounik;
    }

    /**
     * Gets the value of the variant property.
     * 
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the variant property.
     * 
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getVariant().add(newItem);
     * </pre>
     * 
     * 
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link Variant }
     * 
     * 
     */
    public List<Variant> getVariant() {
        if (variant == null) {
            variant = new ArrayList<Variant>();
        }
        return this.variant;
    }

    /**
     * Gets the value of the pdgId property.
     * 
     */
    public int getPdgId() {
        return pdgId;
    }

    /**
     * Sets the value of the pdgId property.
     * 
     */
    public void setPdgId(int value) {
        this.pdgId = value;
    }

    /**
     * Gets the value of the lemma property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getLemma() {
        return lemma;
    }

    /**
     * Sets the value of the lemma property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setLemma(String value) {
        this.lemma = value;
    }

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
     * Gets the value of the theme property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getTheme() {
        return theme;
    }

    /**
     * Sets the value of the theme property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setTheme(String value) {
        this.theme = value;
    }

    /**
     * Gets the value of the govern property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getGovern() {
        return govern;
    }

    /**
     * Sets the value of the govern property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setGovern(String value) {
        this.govern = value;
    }

    /**
     * Gets the value of the type property.
     * 
     * @return
     *     possible object is
     *     {@link Type }
     *     
     */
    public Type getType() {
        return type;
    }

    /**
     * Sets the value of the type property.
     * 
     * @param value
     *     allowed object is
     *     {@link Type }
     *     
     */
    public void setType(Type value) {
        this.type = value;
    }

    /**
     * Gets the value of the marked property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getMarked() {
        return marked;
    }

    /**
     * Sets the value of the marked property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setMarked(String value) {
        this.marked = value;
    }

    /**
     * Gets the value of the options property.
     * 
     * @return
     *     possible object is
     *     {@link ParadigmOptions }
     *     
     */
    public ParadigmOptions getOptions() {
        return options;
    }

    /**
     * Sets the value of the options property.
     * 
     * @param value
     *     allowed object is
     *     {@link ParadigmOptions }
     *     
     */
    public void setOptions(ParadigmOptions value) {
        this.options = value;
    }

    /**
     * Gets the value of the todo property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getTodo() {
        return todo;
    }

    /**
     * Sets the value of the todo property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setTodo(String value) {
        this.todo = value;
    }

    /**
     * Gets the value of the comment property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getComment() {
        return comment;
    }

    /**
     * Sets the value of the comment property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setComment(String value) {
        this.comment = value;
    }

    /**
     * Gets the value of the meaning property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getMeaning() {
        return meaning;
    }

    /**
     * Sets the value of the meaning property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setMeaning(String value) {
        this.meaning = value;
    }

}
