package org.alex73.korpus.compiler;

import org.alex73.korpus.base.TextInfo;
import org.alex73.korpus.text.xml.Tag;
import org.alex73.korpus.text.xml.XMLText;
import org.alex73.korpus.utils.KorpusDateTime;

public class TextUtils {
    public static void fillFromXml(TextInfo info, XMLText text) {
        info.url = getTag(text, "URL");
        String authors = getTag(text, "Authors");
        if (authors != null) {
            info.authors = authors.split(",");
        } else {
            info.authors = null;
        }
        info.title = getTag(text, "Title");
        String translation = getTag(text, "Translation");
        if (translation != null) {
            info.translators = translation.split(",");
        } else {
            info.translators = null;
        }
        info.langOrig = getTag(text, "LangOrig");
        info.publicationTime = getTag(text, "PublicationYear");
        if (info.publicationTime != null) {
            new KorpusDateTime(info.publicationTime);
        }
        info.creationTime = getTag(text, "CreationYear");
        if (info.creationTime != null) {
            new KorpusDateTime(info.creationTime);
        }
        info.edition = getTag(text, "Edition");
        if (info.edition == null) {
            info.edition = getTag(text, "HiddenEdition");
        }
        String styleGenres = getTag(text, "StyleGenre");
        if (styleGenres != null) {
            info.styleGenres = styleGenres.split(",");
        } else {
            info.styleGenres = null;
        }
    }

    private static String getTag(XMLText text, String name) {
        for (Tag tag : text.getHeader().getTag()) {
            if (name.equals(tag.getName())) {
                return tag.getValue().isEmpty() ? null : tag.getValue();
            }
        }
        return null;
    }
}
