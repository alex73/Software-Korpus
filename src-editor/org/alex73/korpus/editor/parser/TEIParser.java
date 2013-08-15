/**************************************************************************
 Korpus - Corpus Linguistics Software.

 Copyright (C) 2013 Aleś Bułojčyk (alex73mail@gmail.com)
               Home page: https://sourceforge.net/projects/korpus/

 This file is part of Korpus.

 Korpus is free software: you can redistribute it and/or modify
 it under the terms of the GNU General Public License as published by
 the Free Software Foundation, either version 3 of the License, or
 (at your option) any later version.

 Korpus is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU General Public License for more details.

 You should have received a copy of the GNU General Public License
 along with this program.  If not, see <http://www.gnu.org/licenses/>.
 **************************************************************************/

package org.alex73.korpus.editor.parser;

import java.io.File;
import java.io.InputStream;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;

import org.alex73.korpus.editor.core.structure.BaseItem;
import org.alex73.korpus.editor.core.structure.KorpusDocument;
import org.alex73.korpus.editor.core.structure.Line;
import org.alex73.korpus.editor.core.structure.SentenceSeparatorItem;
import org.alex73.korpus.editor.core.structure.SpaceItem;
import org.alex73.korpus.editor.core.structure.TagLongItem;
import org.alex73.korpus.editor.core.structure.TagShortItem;
import org.alex73.korpus.editor.core.structure.WordItem;
import org.alex73.korpus.editor.core.structure.ZnakItem;

import alex73.corpus.paradigm.P;
import alex73.corpus.paradigm.Part;
import alex73.corpus.paradigm.S;
import alex73.corpus.paradigm.TEI;
import alex73.corpus.paradigm.Tag;
import alex73.corpus.paradigm.Text;
import alex73.corpus.paradigm.W;

/**
 * Чытаньне й запіс дакумэнту корпуса ў XML.
 */
public class TEIParser {
    public static JAXBContext CONTEXT;
    static {
        try {
            CONTEXT = JAXBContext.newInstance(TEI.class.getPackage().getName());
        } catch (Exception ex) {
            throw new ExceptionInInitializerError(ex);
        }
    }

    public static KorpusDocument parseXML(InputStream in) throws Exception {
        Unmarshaller unm = CONTEXT.createUnmarshaller();

        TEI data = (TEI) unm.unmarshal(in);

        KorpusDocument doc = new KorpusDocument();
        boolean poetryMode = false;

        for (Object o : data.getText().getBody().getHeadOrPOrDiv()) {
            if (o instanceof P) {
                P p = (P) o;
                Line line = new Line();
                doc.add(line);
                for (int j = 0; j < p.getSOrTag().size(); j++) {
                    if (p.getSOrTag().get(j) instanceof S) {
                        S s = (S) p.getSOrTag().get(j);
                        for (int k = 0; k < s.getWOrTag().size(); k++) {
                            if (s.getWOrTag().get(k) instanceof W) {
                                W w = (W) s.getWOrTag().get(k);
                                line.add(new WordItem(w));
                                line.add(new SpaceItem("  "));
                            } else if (s.getWOrTag().get(k) instanceof Tag) {
                                Tag tag = (Tag) s.getWOrTag().get(k);
                                if ("\n".equals(tag.getName())) {
                                    line.add(new SpaceItem("\n"));
                                    line = new Line();
                                    doc.add(line);
                                } else {
                                    line.add(new TagShortItem(tag.getName()));
                                    line.add(new SpaceItem("  "));
                                }
                            } else {
                                throw new RuntimeException("Unknown item: "
                                        + s.getWOrTag().get(k).getClass().getSimpleName());
                            }
                        }
                        line.add(new SentenceSeparatorItem());
                        line.add(new SpaceItem("  "));
                    } else if (p.getSOrTag().get(j) instanceof Tag) {
                        Tag tag = (Tag) p.getSOrTag().get(j);
                        line.add(new TagLongItem(tag.getName()));
                        line.add(new SpaceItem("  "));
                        if ("##Poetry:begin".equals(tag.getName())) {
                            poetryMode = true;
                        } else if ("##Poetry:end".equals(tag.getName())) {
                            poetryMode = false;
                            if (doc.size()>1 && doc.get(doc.size() - 2).isEmpty()) {
                                doc.remove(doc.size() - 2);
                            }
                        }
                    } else {
                        throw new RuntimeException("Unknown item: " + p.getSOrTag().get(j).getClass().getSimpleName());
                    }
                }
                if (poetryMode) {
                    doc.add(new Line());
                }
            }
        }
        for (int i = 0; i < doc.size(); i++) {
            Line line = doc.get(i);
            if (!line.isEmpty() && line.get(line.size() - 1) instanceof SpaceItem) {
                line.remove(line.size() - 1);
            }
            if (!line.isEmpty() && line.get(line.size() - 1) instanceof SentenceSeparatorItem) {
                line.remove(line.size() - 1);
            }
        }

        return doc;
    }

    public static Text constructXML(KorpusDocument doc) {
        Text text = new Text();
        text.setBody(new Part());
        boolean poetryMode = false;

        P p = null;
        S s = null;
        for (Line line : doc) {
            if (!poetryMode) {
                p = new P();
                s = new S();
            }
            if (poetryMode) {
                boolean empty = true;
                for (BaseItem item : line) {
                    if (item instanceof SpaceItem) {
                        continue;
                    }
                    empty = false;
                    break;
                }
                if (empty) {
                    // empty line in poetry - end of P
                    p.getSOrTag().add(s);
                    text.getBody().getHeadOrPOrDiv().add(p);
                    p = new P();
                    s = new S();
                    continue;
                }
            }
            for (BaseItem item : line) {
                if (item instanceof SentenceSeparatorItem) {
                    p.getSOrTag().add(s);
                    s = new S();
                } else if (item instanceof TagLongItem) {
                    p.getSOrTag().add(s);
                    text.getBody().getHeadOrPOrDiv().add(p);
                    p = new P();
                    TagLongItem it = (TagLongItem) item;
                    Tag tag = new Tag();
                    tag.setName(it.getText());
                    p.getSOrTag().add(tag);
                    if ("##Poetry:begin".equals(it.getText())) {
                        poetryMode = true;
                    } else if ("##Poetry:end".equals(it.getText())) {
                        poetryMode = false;
                    }
                    s = new S();
                } else {
                    eventSimpleItem(item, s);
                }
            }
            if (!poetryMode) {
                p.getSOrTag().add(s);
                text.getBody().getHeadOrPOrDiv().add(p);
            } else {
                Tag eol = new Tag();
                eol.setName("\n");
                s.getWOrTag().add(eol);
            }
        }
        // remove empty
        for (int i = 0; i < text.getBody().getHeadOrPOrDiv().size(); i++) {
            if (text.getBody().getHeadOrPOrDiv().get(i) instanceof P) {
                p = (P) text.getBody().getHeadOrPOrDiv().get(i);
                for (int j = 0; j < p.getSOrTag().size(); j++) {
                    if (p.getSOrTag().get(j) instanceof S) {
                        s = (S) p.getSOrTag().get(j);
                        if (s.getWOrTag().isEmpty()) {
                            p.getSOrTag().remove(j);
                            j--;
                        } else if (j == p.getSOrTag().size() - 1) {
                            // last S in P
                            if (s.getWOrTag().get(s.getWOrTag().size() - 1) instanceof Tag) {
                                Tag t = (Tag) s.getWOrTag().get(s.getWOrTag().size() - 1);
                                if ("\n".equals(t.getName())) {
                                    s.getWOrTag().remove(s.getWOrTag().size() - 1);
                                }
                            }
                        }
                    }
                }
                if (!p.getSOrTag().isEmpty()) {

                }
                if (p.getSOrTag().isEmpty()) {
                    text.getBody().getHeadOrPOrDiv().remove(i);
                    i--;
                }
            }
        }
        return text;
    }

    static void eventSimpleItem(BaseItem item, S s) {
        if (item instanceof WordItem) {
            WordItem it = (WordItem) item;
            s.getWOrTag().add(it.w);
        } else if (item instanceof ZnakItem) {
            ZnakItem it = (ZnakItem) item;
            s.getWOrTag().add(it.w);
        } else if (item instanceof TagShortItem) {
            TagShortItem it = (TagShortItem) item;
            Tag tag = new Tag();
            tag.setName(it.getText());
            s.getWOrTag().add(tag);
        } else if (item instanceof SpaceItem) {
        } else {
            throw new RuntimeException("Unknown item: " + item.getClass().getSimpleName());
        }
    }

    public static void saveXML(File f, TEI tei) throws Exception {
        Marshaller m = CONTEXT.createMarshaller();
        m.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
        m.marshal(tei, f);
    }
}
