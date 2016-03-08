package org.alex73.korpus.editor.core.structure;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.alex73.korpus.text.xml.Content;
import org.alex73.korpus.text.xml.ITextLineElement;
import org.alex73.korpus.text.xml.InlineTag;
import org.alex73.korpus.text.xml.O;
import org.alex73.korpus.text.xml.P;
import org.alex73.korpus.text.xml.Poetry;
import org.alex73.korpus.text.xml.S;
import org.alex73.korpus.text.xml.Se;
import org.alex73.korpus.text.xml.Tag;
import org.alex73.korpus.text.xml.W;
import org.alex73.korpus.text.xml.Z;

/**
 * Converts XMLText to list of lines for display in KorpusDocument.
 */
public class XML2Lines {

    static final Pattern RE_TAG = Pattern.compile("##(.+?):(.+)");

    /**
     * Converts XML to UI lines.
     */
    public static List<Line> convertToLines(Content content) {
        List<Line> result = new ArrayList<>();

        for (Object line : content.getPOrTagOrPoetry()) {
            if (line instanceof P) {
                Line currentLine = new Line();
                P p = (P) line;
                for (int s = 0; s < p.getSe().size(); s++) {
                    Se sentence = p.getSe().get(s);
                    for (ITextLineElement inc : sentence.getWOrSOrZ()) {
                        currentLine.add(inc);
                    }
                    if (s < p.getSe().size() - 1) {
                        currentLine.add(new SentenceSeparatorItem());
                    }
                }
                currentLine.add(new S('\n'));
                result.add(currentLine);
            } else if (line instanceof Poetry) {
                parseTag(new Tag("Poetry", "begin"), result);
                parsePoetryParagraph(((Poetry) line).getPOrTag(), result);
                parseTag(new Tag("Poetry", "end"), result);
            } else if (line instanceof Tag) {
                parseTag((Tag) line, result);
            } else {
                throw new RuntimeException("Wrong tag");
            }
        }
        {
            // Ctrl+End hack
            Line currentLine = new Line();
            currentLine.add(new S(' '));
            result.add(currentLine);
        }
        return result;
    }

    /**
     * Converts poetry to lines.
     */
    private static void parsePoetryParagraph(List<Object> pOrTag, List<Line> result) {
        for (Object pt : pOrTag) {
            if (pt instanceof Tag) {
                parseTag((Tag) pt, result);
            } else if (pt instanceof P) {
                P p = (P) pt;
                Line currentLine = new Line();
                for (int s = 0; s < p.getSe().size(); s++) {
                    Se sentence = p.getSe().get(s);
                    for (ITextLineElement inc : sentence.getWOrSOrZ()) {
                        currentLine.add(inc);
                        if (inc instanceof S && "\n".equals(((S) inc).getChar())) {
                            // new line in poetry
                            result.add(currentLine);
                            currentLine = new Line();
                        }
                    }
                }
                if (!currentLine.isEmpty()) {
                    result.add(currentLine);
                }
                currentLine = new Line();
                currentLine.add(new S('\n'));
                result.add(currentLine);
            } else {
                throw new RuntimeException("Wrong tag");
            }
        }
    }

    /**
     * Converts tag to line.
     */
    private static void parseTag(Tag tag, List<Line> result) {
        Line currentLine = new Line();
        currentLine.add(new LongTagItem("##" + tag.getName() + ": " + tag.getValue()));
        currentLine.add(new S('\n'));
        result.add(currentLine);
    }

    /**
     * Converts UI lines to XML.
     */
    public static Content convertToXML(List<Line> lines) {
        Content result = new Content();

        Poetry poetry = null;

        P poetryP = null;
        Se poetrySe = null;

        // Ctrl+End hack
        if (lines.size() > 0 && lines.get(lines.size() - 1).size() == 1
                && lines.get(lines.size() - 1).get(0) instanceof S) {
            lines.remove(lines.size() - 1);
        }

        for (Line li : lines) {
            if (li.size() > 0 && li.get(li.size() - 1) instanceof S
                    && "\n".equals(li.get(li.size() - 1).getText())) {
                // remove newline at the end
                li.remove(li.size() - 1);
            }
            if (poetry != null) {
                if (isPoetryEnd(li)) {
                    // last P can contains only EOL
                    if (poetrySe.getWOrSOrZ().size() == 1 && poetrySe.getWOrSOrZ().get(0) instanceof S
                            && poetryP.getSe().size() == 1) {
                        poetry.getPOrTag().remove(poetryP);
                    }
                    // last P can contains only empty Se
                    if (poetrySe.getWOrSOrZ().isEmpty() && poetryP.getSe().size() == 1) {
                        poetry.getPOrTag().remove(poetryP);
                    }
                    poetry = null;
                    poetryP = null;
                    poetrySe = null;
                } else {
                    if (li.isEmpty()) {
                        // empty line
                        poetryP = new P();
                        poetry.getPOrTag().add(poetryP);
                        poetrySe = new Se();
                        poetryP.getSe().add(poetrySe);
                    } else {
                        for (int i = 0; i < li.size(); i++) {
                            ITextLineElement el = li.get(i);
                            if (el instanceof SentenceSeparatorItem) {
                                poetrySe = new Se();
                                poetryP.getSe().add(poetrySe);
                            } else if (el instanceof LongTagItem) {
                                Matcher m = RE_TAG.matcher(el.getText());
                                if (!m.matches()) {
                                    throw new RuntimeException("Няправільны тэг: " + el);
                                }
                                if (poetrySe.getWOrSOrZ().isEmpty()) {
                                    poetryP.getSe().remove(poetrySe);
                                }
                                if (poetryP.getSe().isEmpty()) {
                                    poetry.getPOrTag().remove(poetryP);
                                }
                                poetry.getPOrTag().add(new Tag(m.group(1), m.group(2).trim()));
                                i++; // skip next EOL
                                poetryP = new P();
                                poetry.getPOrTag().add(poetryP);
                                poetrySe = new Se();
                                poetryP.getSe().add(poetrySe);
                            } else {
                                poetrySe.getWOrSOrZ().add(el);
                            }
                        }
                        poetrySe.getWOrSOrZ().add(new S('\n'));
                    }
                }
            } else {
                if (isPoetryStart(li)) {
                    poetry = new Poetry();
                    result.getPOrTagOrPoetry().add(poetry);
                    poetryP = new P();
                    poetry.getPOrTag().add(poetryP);
                    poetrySe = new Se();
                    poetryP.getSe().add(poetrySe);
                } else {
                    if (li.size() == 1 && li.get(0) instanceof LongTagItem) {
                        Matcher m = RE_TAG.matcher(li.get(0).getText());
                        if (!m.matches()) {
                            throw new RuntimeException("Няправільны тэг: " + li.get(0));
                        }
                        result.getPOrTagOrPoetry().add(new Tag(m.group(1), m.group(2).trim()));
                    } else {
                        result.getPOrTagOrPoetry().add(line2text(li));
                    }
                }
            }
        }

        return result;
    }

    /**
     * Check is poetry tag starts.
     */
    static boolean isPoetryStart(Line li) {
        if (li.size() == 1 && li.get(0) instanceof LongTagItem) {
            String t = li.get(0).getText();
            Matcher m = RE_TAG.matcher(t);
            if (!m.matches()) {
                throw new RuntimeException("Няправільны тэг: " + t);
            }
            return "Poetry".equals(m.group(1)) && "begin".equals(m.group(2).trim());
        } else {
            return false;
        }
    }

    /**
     * Check is poetry tag ends.
     */
    static boolean isPoetryEnd(Line li) {
        if (li.size() == 1 && li.get(0) instanceof LongTagItem) {
            String t = li.get(0).getText();
            Matcher m = RE_TAG.matcher(t);
            if (!m.matches()) {
                throw new RuntimeException("Няправільны тэг: " + t);
            }
            return "Poetry".equals(m.group(1)) && "end".equals(m.group(2).trim());
        } else {
            return false;
        }
    }

    /**
     * Converts text line to P.
     */
    static P line2text(Line li) {
        P r = new P();
        Se se = new Se();
        for (ITextLineElement item : li) {
            if (item instanceof SentenceSeparatorItem) {
                r.getSe().add(se);
                se = new Se();
            } else if (item instanceof W) {
                se.getWOrSOrZ().add(item);
            } else if (item instanceof O) {
                se.getWOrSOrZ().add(item);
            } else if (item instanceof S) {
                se.getWOrSOrZ().add(item);
            } else if (item instanceof Z) {
                se.getWOrSOrZ().add(item);
            } else if (item instanceof InlineTag) {
                se.getWOrSOrZ().add(item);
            } else {
                throw new RuntimeException("Няправільны item: " + item);
            }
        }
        if (!se.getWOrSOrZ().isEmpty()) {
            r.getSe().add(se);
        }
        return r;
    }
}
