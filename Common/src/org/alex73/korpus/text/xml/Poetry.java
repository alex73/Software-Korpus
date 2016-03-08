package org.alex73.korpus.text.xml;

import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElements;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "", propOrder = {
    "pOrTag"
})
@XmlRootElement(name = "Poetry")
public class Poetry {

    @XmlElements({
        @XmlElement(name = "p", type = P.class),
        @XmlElement(name = "Tag", type = Tag.class)
    })
    protected List<Object> pOrTag;

    public List<Object> getPOrTag() {
        if (pOrTag == null) {
            pOrTag = new ArrayList<Object>();
        }
        return this.pOrTag;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof Poetry) {
            Poetry o = (Poetry) obj;
            return o.pOrTag.equals(pOrTag);
        } else {
            return false;
        }
    }
}
