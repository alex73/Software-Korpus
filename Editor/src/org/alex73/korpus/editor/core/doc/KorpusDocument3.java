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

package org.alex73.korpus.editor.core.doc;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.List;
import java.util.Vector;

import javax.swing.event.DocumentEvent;
import javax.swing.event.UndoableEditEvent;
import javax.swing.text.AbstractDocument;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.Element;
import javax.swing.text.GapContent;
import javax.swing.text.Position;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleContext;

import org.alex73.korpus.editor.Splitter;
import org.alex73.korpus.editor.UI;
import org.alex73.korpus.editor.core.structure.Line;
import org.alex73.korpus.editor.core.structure.LongTagItem;
import org.alex73.korpus.editor.core.structure.XML2Lines;
import org.alex73.korpus.text.xml.Header;
import org.alex73.korpus.text.xml.ITextLineElement;
import org.alex73.korpus.text.xml.InlineTag;
import org.alex73.korpus.text.xml.O;
import org.alex73.korpus.text.xml.OtherType;
import org.alex73.korpus.text.xml.W;
import org.alex73.korpus.text.xml.XMLText;

/**
 * Рэдактар дакумэнту корпуса.
 */
@SuppressWarnings("serial")
public class KorpusDocument3 extends AbstractDocument {
    public enum MARK_WORDS {
        UNK_LEMMA, AMAN_LEMMA, AMAN_GRAM
    };

    public static final SimpleAttributeSet ATTRS_OTHER_LANGUAGE;
    public static final SimpleAttributeSet ATTRS_DIGITS;
    public static final SimpleAttributeSet ATTRS_TRASIANKA;
    public static final SimpleAttributeSet ATTRS_DYJALEKT;

    static {
        ATTRS_OTHER_LANGUAGE = new SimpleAttributeSet();
        ATTRS_OTHER_LANGUAGE.addAttribute("OtherType", OtherType.OTHER_LANGUAGE);
        ATTRS_DIGITS = new SimpleAttributeSet();
        ATTRS_DIGITS.addAttribute("OtherType", OtherType.NUMBER);
        ATTRS_TRASIANKA = new SimpleAttributeSet();
        ATTRS_TRASIANKA.addAttribute("OtherType", OtherType.TRASIANKA);
        ATTRS_DYJALEKT = new SimpleAttributeSet();
        ATTRS_DYJALEKT.addAttribute("OtherType", OtherType.DYJALEKT);
    }

    MyRootElement rootElem;
    public MARK_WORDS markType = MARK_WORDS.UNK_LEMMA;

    private Header header;

    private StringBuilder text = new StringBuilder(100000);

    public KorpusDocument3(XMLText fs) throws Exception {
        super(new GapContent(65536), new StyleContext());

        header = fs.getHeader();

        rootElem = new MyRootElement(XML2Lines.convertToLines(fs.getContent()));

        Content c = getContent();
        c.insertString(0, text.toString());
        text = null;
        for (int i = 0; i < rootElem.getChildCount(); i++) {
            MyLineElement p = rootElem.getElement(i);
            for (int j = 0; j < p.getChildCount(); j++) {
                MyWordElement e = p.getElement(j);
                e.createPositions();
            }
        }

        addUndoableEditListener(UI.editorUndoManager);
    }

    public XMLText extractText() {
        List<Line> lines = new ArrayList<>();
        for (MyLineElement pd : rootElem.children) {
            Line line = new Line();
            for (MyWordElement el : pd.children) {
                line.add(el.item);
            }
            lines.add(line);
        }
        XMLText out = new XMLText();
        out.setHeader(header);
        out.setContent(XML2Lines.convertToXML(lines));
        return out;
    }

    @Override
    public Element getDefaultRootElement() {
        return rootElem;
    }

    @Override
    public Element getParagraphElement(int pos) {
        if (rootElem == null) {
            return null;
        } else {
            return rootElem.getElement(rootElem.getElementIndex(pos));
        }
    }

    @Override
    protected Element createBranchElement(Element parent, AttributeSet a) {
        // return new MyLineElement(parent);
        throw new RuntimeException("Not implemented");
    }

    @Override
    protected Element createLeafElement(Element parent, AttributeSet a, int p0, int p1) {
        throw new RuntimeException("Not implemented: createLeafElement");
    }

    @Override
    public void insertString(int offs, String str, AttributeSet a) throws BadLocationException {
        writeLock();
        try {
            DefaultDocumentEvent e = new DefaultDocumentEvent(offs, str.length(),
                    DocumentEvent.EventType.INSERT);

            String[] newStrs = splitInserted(str);
            List<Line> newLines = new ArrayList<>();
            for (int i = 0; i < newStrs.length; i++) {
                Line line;
                OtherType type = a == null ? null : (OtherType) a.getAttribute("OtherType");
                if (a==null) {
                    line = new Splitter(newStrs[i]).splitParagraph();
                }else {
                    line = Line.splitOther(newStrs[i], type);
                }
                newLines.add(line);
            }

            int pIndex = rootElem.getElementIndex(offs);
            int offsetInOldLine = offs - rootElem.getElement(pIndex).getStartOffset();

            MyLineElement[] oldParagraphs;
            if (offsetInOldLine == 0 && offs > 0) {
                // at the begin of paragraph - need change previous
                oldParagraphs = new MyLineElement[2];
                pIndex--;
            } else {
                oldParagraphs = new MyLineElement[1];
            }
            int pOffset = rootElem.getElement(pIndex).getStartOffset();
            rootElem.removeChildren(pIndex, oldParagraphs);
            Line oldLine = oldParagraphs[oldParagraphs.length - 1].extractItems();
            Line prevLine = oldParagraphs.length == 1 ? null : oldParagraphs[0].extractItems();

            Line lineLeft = oldLine.leftAt(offsetInOldLine);
            Line lineRight = oldLine.rightAt(offsetInOldLine);
            lineLeft.addAll(newLines.get(0));
            newLines.set(0, lineLeft);
            newLines.get(newLines.size() - 1).addAll(lineRight);
            if (prevLine != null) {
                newLines.add(0, prevLine);
            }

            e.addEdit(getContent().insertString(offs, str));

            MyLineElement[] newParagraphs = recreateParagraphs(newLines, pOffset, pIndex);

            e.addEdit(new MyUndo(rootElem, pIndex, oldParagraphs, newParagraphs));

            e.end();
            fireInsertUpdate(e);
            fireUndoableEditUpdate(new UndoableEditEvent(this, e));
        } finally {
            writeUnlock();
        }
    }

    String[] splitInserted(String insertedText) {
        List<String> result = new ArrayList<>();
        int pos = 0;
        int start = 0;
        while (true) {
            pos = insertedText.indexOf('\n', start);
            if (pos < 0) {
                break;
            }
            result.add(insertedText.substring(start, pos + 1));
            start = pos + 1;
        }
        result.add(insertedText.substring(start));

        return result.toArray(new String[result.size()]);
    }

    @Override
    public void remove(int offs, int len) throws BadLocationException {
        writeLock();
        try {
            DefaultDocumentEvent e = new DefaultDocumentEvent(offs, len, DocumentEvent.EventType.REMOVE);

            int pIndexStart = rootElem.getElementIndex(offs);
            int pIndexEnd = rootElem.getElementIndex(offs + len);

            MyLineElement[] oldParagraphs = new MyLineElement[pIndexEnd - pIndexStart + 1];
            int pOffset = rootElem.getElement(pIndexStart).getStartOffset();
            rootElem.removeChildren(pIndexStart, oldParagraphs);

            List<Line> lines = new ArrayList<>();
            for (MyLineElement p : oldParagraphs) {
                lines.add(p.extractItems());
            }
            Line oldLine = lines.get(0).leftAt(offs - pOffset);
            lines.set(0, lines.get(0).rightAt(offs - pOffset));
            int removeCount = len;
            for (int i = 0; i < lines.size() - 1; i++) {
                removeCount -= lines.get(i).length();
            }
            oldLine.addAll(lines.get(lines.size() - 1).rightAt(removeCount));
            lines.clear();
            lines.add(oldLine);

            e.addEdit(getContent().remove(offs, len));

            MyLineElement[] newParagraphs = recreateParagraphs(lines, pOffset, pIndexStart);

            e.addEdit(new MyUndo(rootElem, pIndexStart, oldParagraphs, newParagraphs));

            e.end();
            fireRemoveUpdate(e);
            fireUndoableEditUpdate(new UndoableEditEvent(this, e));
        } finally {
            writeUnlock();
        }
    }

    MyLineElement[] recreateParagraphs(List<Line> newLines, int pOffset, int pIndex) {
        int startOffset = 0;

        MyLineElement[] newParagraphs = new MyLineElement[newLines.size()];
        for (int i = 0; i < newParagraphs.length; i++) {
            Line newLine = newLines.get(i);
            newLine.normalize();

            MyLineElement p = newParagraphs[i] = new MyLineElement(rootElem);

            for (ITextLineElement item : newLine) {
                p.children.add(new MyWordElement(p, pOffset + startOffset, item));
                startOffset += item.getText().length();
            }
        }
        for (int i = 0; i < newParagraphs.length; i++) {
            MyLineElement p = newParagraphs[i];
            for (int j = 0; j < p.getChildCount(); j++) {
                p.getElement(j).createPositions();
            }
            rootElem.children.add(pIndex + i, p);
            // pOffset = p.getEndOffset();
        }
        return newParagraphs;
    }

    public interface ElementChanger {
        public void replace(int offset, int length, Element[] elems);
    }

    public abstract class MyGroupElement<T extends AbstractElement> extends AbstractElement
            implements ElementChanger {
        List<T> children = new ArrayList<>();

        public MyGroupElement(Element parent) {
            super(parent, null);
        }

        @Override
        public String getName() {
            return getClass().getSimpleName();
        }

        @Override
        public boolean isLeaf() {
            return false;
        }

        public void removeChildren(int from, T[] result) {
            for (int i = 0; i < result.length; i++) {
                result[i] = children.remove(from);
            }
        }

        @SuppressWarnings("unchecked")
        @Override
        public void replace(int offset, int length, Element[] elems) {
            for (int i = 0; i < length; i++) {
                children.remove(offset);
            }
            children.addAll(offset, Arrays.asList((T[]) elems));
        }

        @Override
        public int getElementIndex(int offset) {
            for (int i = children.size() - 1; i >= 0; i--) {
                AbstractElement e = children.get(i);
                if (offset >= e.getStartOffset()) {
                    return i;
                }
            }
            return 0;
        }

        @Override
        public T getElement(int index) {
            return children.get(index);
        }

        @Override
        public int getElementCount() {
            return children.size();
        }

        @Override
        public Enumeration<T> children() {
            if (children.size() == 0)
                return null;

            return new Vector<T>(children).elements();
        }

        @Override
        public boolean getAllowsChildren() {
            return true;
        }

        @Override
        public int getStartOffset() {
            return children.get(0).getStartOffset();
        }

        @Override
        public int getEndOffset() {
            if (children.isEmpty()) {
                throw new RuntimeException("childempty");
            }
            return children.get(children.size() - 1).getEndOffset();
        }

        @Override
        public String toString() {
            return getClass().getSimpleName() + "(" + getName() + ") " + getStartOffset() + "-"
                    + getEndOffset() + "\n";
        }
    }

    protected class MyRootElement extends MyGroupElement<MyLineElement> {
        public MyRootElement(List<Line> lines) {
            super(null);
            for (Line line : lines) {
                children.add(new MyLineElement(this, line));
            }
        }
    }

    public class MyLineElement extends MyGroupElement<MyWordElement> {
        public MyLineElement(Element parent) {
            super(parent);
        }

        public MyLineElement(Element parent, Line chs) {
            super(parent);
            for (ITextLineElement el : chs) {
                children.add(new MyWordElement(this, el));
            }
        }

        public Line extractItems() {
            Line result = new Line();
            for (int i = 0; i < children.size(); i++) {
                result.add(children.get(i).item);
            }
            return result;
        }
    }

    public class MyWordElement extends AbstractElement {
        public final ITextLineElement item;

        private transient Position p0;
        private transient Position p1;
        int p0v, p1v;

        public MyWordElement(Element parent, ITextLineElement item) {
            super(parent, null);
            this.item = item;
            p0v = text.length();
            text.append(item.getText());
            p1v = text.length();
        }

        public MyWordElement(Element parent, int startOffset, ITextLineElement item) {
            super(parent, null);
            this.item = item;
            p0v = startOffset;
            p1v = startOffset + item.getText().length();
        }

        public boolean isTag() {
            return (item instanceof InlineTag) || (item instanceof LongTagItem);
        }

        public boolean isOther() {
            return item instanceof O;
        }

        public W getWordInfo() {
            if (item instanceof W) {
                return (W) item;
            } else {
                return null;
            }
        }

        public void setWordInfo(W otherW) {
            if (item instanceof W) {
                W w = (W) item;
                w.setCat(otherW.getCat());
                w.setLemma(otherW.getLemma());
                w.setManual(otherW.getManual());
            } else {
                throw new ClassCastException(
                        "Trying to set word info into " + item.getClass().getSimpleName());
            }
        }

        public boolean isMarked() {
            W w = getWordInfo();
            if (w == null) {
                return false;
            }
            boolean marked = false;
            switch (markType) {
            case UNK_LEMMA:
                marked = w.getLemma() == null;
                break;
            case AMAN_LEMMA:
                marked = w.getLemma() != null && w.getLemma().contains("_");
                break;
            case AMAN_GRAM:
                marked = w.getCat() != null && w.getCat().contains("_");
                break;
            }
            return marked;
        }

        public void createPositions() {
            try {
                p0 = createPosition(p0v);
                p1 = createPosition(p1v);
            } catch (BadLocationException e) {
                throw new RuntimeException("Can't create Position references");
            }
        }

        public String toString() {
            try {
                return getClass().getSimpleName() + "(" + getName() + ") " + p0 + "-" + p1 + ": "
                        + getText(p0.getOffset(), p1.getOffset() - p0.getOffset());
            } catch (BadLocationException ex) {
                throw new RuntimeException(ex);
            }
        }

        public int getStartOffset() {
            return p0.getOffset();
        }

        public int getEndOffset() {
            return p1.getOffset();
        }

        public String getName() {
            return getClass().getSimpleName();
        }

        public int getElementIndex(int pos) {
            return -1;
        }

        public Element getElement(int index) {
            return null;
        }

        public int getElementCount() {
            return 0;
        }

        public boolean isLeaf() {
            return true;
        }

        public boolean getAllowsChildren() {
            return false;
        }

        public Enumeration<AbstractElement> children() {
            return null;
        }
    }
}