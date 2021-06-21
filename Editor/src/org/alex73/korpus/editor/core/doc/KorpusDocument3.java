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
import java.util.stream.Collectors;

import javax.swing.event.DocumentEvent;
import javax.swing.event.UndoableEditEvent;
import javax.swing.text.AbstractDocument;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.Element;
import javax.swing.text.GapContent;
import javax.swing.text.Position;
import javax.swing.text.StyleContext;

import org.alex73.korpus.editor.UI;
import org.alex73.korpus.editor.core.doc.structure.Line;
import org.alex73.korpus.editor.core.doc.structure.XML2Lines;
import org.alex73.korpus.text.parser.IProcess;
import org.alex73.korpus.text.parser.Splitter3;
import org.alex73.korpus.text.structure.files.ITextLineElement;
import org.alex73.korpus.text.structure.files.TextLine;
import org.alex73.korpus.text.structure.files.WordItem;

/**
 * Рэдактар дакумэнту корпуса.
 */
@SuppressWarnings("serial")
public class KorpusDocument3 extends AbstractDocument {
    public enum MARK_WORDS {
        UNK_LEMMA, AMAN_LEMMA, AMAN_GRAM
    };

    public MyRootElement rootElem;
    public MARK_WORDS markType = MARK_WORDS.UNK_LEMMA;

    //private Header header;

    private StringBuilder text = new StringBuilder(100000);

    public KorpusDocument3(List<TextLine> lines) throws Exception {
        super(new GapContent(65536), new StyleContext());

        //header = fs.getHeader();

        rootElem = new XML2Lines(this).xml2ui(lines);

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

    public List<TextLine> extractText() {
        return new XML2Lines(this).extract();
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

    public List<MyLineElement> getParagraphs(int start, int end) {
        if (rootElem == null) {
            return null;
        } else {
            List<MyLineElement> r = new ArrayList<>();
            int f = rootElem.getElementIndex(start);
            int t = rootElem.getElementIndex(end);
            for (int i = f; i <= t; i++) {
                r.add(rootElem.getElement(i));
            }
            return r;
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
            List<TextLine> newLines = new ArrayList<>();
            for (int i = 0; i < newStrs.length; i++) {
                TextLine line = new Splitter3(false, new IProcess() {
                    @Override
                    public void showStatus(String status) {
                    }

                    @Override
                    public void reportError(String error, Throwable ex) {
                    }
                }).parse(newStrs[i]);
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
            TextLine oldLine = oldParagraphs[oldParagraphs.length - 1].extractItems();
            TextLine prevLine = oldParagraphs.length == 1 ? null : oldParagraphs[0].extractItems();

            TextLine lineLeft = Line.leftAt(oldLine, offsetInOldLine);
            TextLine lineRight = Line.rightAt(oldLine, offsetInOldLine);
            lineLeft.addAll(newLines.get(0));
            newLines.set(0, lineLeft);
            newLines.get(newLines.size() - 1).addAll(lineRight);
            if (prevLine != null) {
                newLines.add(0, prevLine);
            }

            e.addEdit(getContent().insertString(offs, str));
            normalize(newLines, offs - pOffset, str.length());
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

            List<TextLine> lines = new ArrayList<>();
            for (MyLineElement p : oldParagraphs) {
                lines.add(p.extractItems());
            }
            TextLine oldLine = Line.leftAt(lines.get(0), offs - pOffset);
            lines.set(0, Line.rightAt(lines.get(0), offs - pOffset));
            int removeCount = len;
            for (int i = 0; i < lines.size() - 1; i++) {
                removeCount -= lines.get(i).size();
            }
            oldLine.addAll(Line.rightAt(lines.get(lines.size() - 1), removeCount));
            lines.clear();
            lines.add(oldLine);

            e.addEdit(getContent().remove(offs, len));
            normalize(lines, offs - pOffset, 0);
            MyLineElement[] newParagraphs = recreateParagraphs(lines, pOffset, pIndexStart);

            e.addEdit(new MyUndo(rootElem, pIndexStart, oldParagraphs, newParagraphs));

            e.end();
            fireRemoveUpdate(e);
            fireUndoableEditUpdate(new UndoableEditEvent(this, e));
        } finally {
            writeUnlock();
        }
    }

    public void markOneWord(int offs, int len) throws Exception {
        if (len<0) {
            throw new Exception("Няправільная пазнака");
        }
        writeLock();
        try {
            DefaultDocumentEvent e = new DefaultDocumentEvent(offs, len, DocumentEvent.EventType.CHANGE);

            int pIndexStart = rootElem.getElementIndex(offs);
            int pIndexEnd = rootElem.getElementIndex(offs + len);
            if (pIndexStart!=pIndexEnd) {
                throw new Exception("Пазначана некалькі радкоў - слова можна вызначыць толькі ў адным радку");
            }

            MyLineElement[] oldParagraph = new MyLineElement[1];
            int pOffset = rootElem.getElement(pIndexStart).getStartOffset();
            rootElem.removeChildren(pIndexStart, oldParagraph);

            TextLine line = oldParagraph[0].extractItems();

            TextLine oldLeftLine = Line.leftAt(line, offs - pOffset);
            line = Line.rightAt(line, offs - pOffset);
            TextLine oldRightLine = Line.rightAt(line, len);
            line = Line.leftAt(line, len);
            String word = line.stream().map(l -> l.getText()).collect(Collectors.joining());
            line = new TextLine();
            line.addAll(oldLeftLine);
            line.add(new WordItem(word));
            line.addAll(oldRightLine);

            List<TextLine> lines = new ArrayList<>();
            lines.add(line);

            MyLineElement[] newParagraph = recreateParagraphs(lines, pOffset, pIndexStart);

            e.addEdit(new MyUndo(rootElem, pIndexStart, oldParagraph, newParagraph));

            e.end();
            fireChangedUpdate(e);
            fireUndoableEditUpdate(new UndoableEditEvent(this, e));
        } finally {
            writeUnlock();
        }
    }

    public void markAuto(int offs, int len) throws Exception {
        if (len<0) {
            throw new Exception("Няправільная пазнака");
        }
        writeLock();
        try {
            DefaultDocumentEvent e = new DefaultDocumentEvent(offs, len, DocumentEvent.EventType.CHANGE);

            int pIndexStart = rootElem.getElementIndex(offs);
            int pIndexEnd = rootElem.getElementIndex(offs + len);

            MyLineElement[] oldParagraphs = new MyLineElement[pIndexEnd - pIndexStart + 1];
            int pOffset = rootElem.getElement(pIndexStart).getStartOffset();
            rootElem.removeChildren(pIndexStart, oldParagraphs);

            List<TextLine> lines = new ArrayList<>();
            for (MyLineElement p : oldParagraphs) {
                lines.add(p.extractItems());
            }

            normalize(lines, offs - pOffset, len);
            MyLineElement[] newParagraphs = recreateParagraphs(lines, pOffset, pIndexStart);

            e.addEdit(new MyUndo(rootElem, pIndexStart, oldParagraphs, newParagraphs));

            e.end();
            fireChangedUpdate(e);
            fireUndoableEditUpdate(new UndoableEditEvent(this, e));
        } finally {
            writeUnlock();
        }
    }

    void normalize(List<TextLine> newLines, int offset, int length) {
        // check left border
        if (Line.length(newLines.get(0)) < offset) {
            throw new RuntimeException("Wrong lines");
        }
        int withoutLast = 0;
        for (int i = 0; i < newLines.size() - 1; i++) {
            withoutLast += Line.length(newLines.get(i));
        }
        // check right border
        int posLast = offset + length - withoutLast;
        if (posLast < 0) {
            throw new RuntimeException("Wrong lines");
        }
        if (Line.length(newLines.get(newLines.size() - 1)) < posLast) {
            throw new RuntimeException("Wrong lines");
        }
        if (newLines.size() > 1) {
            // normalize first line
            TextLine b = Line.leftAt(newLines.get(0), offset);
            TextLine a = Line.rightAt(newLines.get(0), offset);
            if (!b.isEmpty()) {
                a.add(0, b.remove(b.size() - 1));
            }
            Line.normalize(a);
            b.addAll(a);
            newLines.set(0, b);
            // normalize last line
            b = Line.leftAt(newLines.get(newLines.size() - 1), posLast);
            a = Line.rightAt(newLines.get(newLines.size() - 1), posLast);
            if (!a.isEmpty()) {
                b.add(a.remove(0));
            }
            Line.normalize(b);
            b.addAll(a);
            newLines.set(newLines.size() - 1, b);
            // normalize middle lines
            for (int i = 1; i < newLines.size() - 1; i++) {
                Line.normalize(newLines.get(i));
            }
        } else {
            // only one line
            TextLine b = Line.leftAt(newLines.get(0), offset);
            TextLine a = Line.rightAt(newLines.get(0), offset);
            TextLine m = Line.leftAt(a, length);
            a = Line.rightAt(a, length);
            if (!b.isEmpty()) {
                m.add(0, b.remove(b.size() - 1));
            }
            if (!a.isEmpty()) {
                m.add(a.remove(0));
            }
            Line.normalize(m);
            b.addAll(m);
            b.addAll(a);
            newLines.set(0, b);
        }
    }

    MyLineElement[] recreateParagraphs(List<TextLine> newLines, int pOffset, int pIndex) {
        int startOffset = 0;

        MyLineElement[] newParagraphs = new MyLineElement[newLines.size()];
        for (int i = 0; i < newParagraphs.length; i++) {
            TextLine newLine = newLines.get(i);

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
        public List<T> children = new ArrayList<>();

        public MyGroupElement(Element parent) {
            super(parent, null);
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

    public class MyRootElement extends MyGroupElement<MyLineElement> {
        public MyRootElement() {
            super(null);
        }

        @Override
        public String getName() {
            return "MyRootElement";
        }
    }

    public class MyLineElement extends MyGroupElement<MyWordElement> {
        public MyLineElement(MyRootElement parent) {
            super(parent);
        }

        public TextLine extractItems() {
            TextLine result = new TextLine();
            for (int i = 0; i < children.size(); i++) {
                result.add(children.get(i).item);
            }
            return result;
        }

        @Override
        public String getName() {
            return "MyLineElement";
        }
    }

    public class MyWordElement extends AbstractElement {
        public final ITextLineElement item;
        private transient Position p0;
        private transient Position p1;
        int p0v, p1v;

        public MyWordElement(MyLineElement parent, ITextLineElement item) {
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

        /*public MyInlineElement(Element parent, int startOffset) {
            super(parent, null);
            this.other =item instanceof O;
            p0v = startOffset;
            p1v = startOffset + item.getText().length();
        }*/

        public void createPositions() {
            try {
                p0 = createPosition(p0v);
                p1 = createPosition(p1v);
            } catch (BadLocationException e) {
                throw new RuntimeException("Can't create Position references");
            }
        }

        public String getElementText() throws BadLocationException {
            return getText(p0.getOffset(), p1.getOffset() - p0.getOffset());
        }

        public boolean isMarked() {
            boolean marked = false;
            if (item instanceof WordItem) {
                WordItem wi = (WordItem) item;
                switch (markType) {
                case UNK_LEMMA:
                    marked = wi.lemmas == null;
                    break;
                case AMAN_LEMMA:
                    marked = wi.lemmas != null && wi.lemmas.indexOf(';') >= 0;
                    break;
                case AMAN_GRAM:
                    marked = wi.tags != null && wi.tags.indexOf(';') >= 0;
                    break;
                }
            }
            return marked;
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
            return "MyWordElement";
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
