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
import javax.swing.text.StyleContext;

import org.alex73.korpus.editor.UI;
import org.alex73.korpus.editor.core.structure.BaseItem;
import org.alex73.korpus.editor.core.structure.KorpusDocument;
import org.alex73.korpus.editor.core.structure.Line;
import org.alex73.korpus.editor.core.structure.SpaceItem;
import org.alex73.korpus.editor.core.structure.TagLongItem;
import org.alex73.korpus.editor.core.structure.TagShortItem;
import org.alex73.korpus.editor.core.structure.WordItem;
import org.alex73.korpus.editor.core.structure.ZnakItem;
import org.alex73.korpus.editor.parser.Splitter;

import alex73.corpus.paradigm.W;

/**
 * Рэдактар дакумэнту корпуса.
 */
public class KorpusDocument3 extends AbstractDocument {
    public enum MARK_WORDS {
        UNK_LEMMA, AMAN_LEMMA, AMAN_GRAM
    };

    MyRootElement rootElem;
    public MARK_WORDS markType = MARK_WORDS.UNK_LEMMA;

    public KorpusDocument3(KorpusDocument fs) throws Exception {
        super(new GapContent(65536), new StyleContext());

        rootElem = new MyRootElement();

        StringBuilder str = new StringBuilder(100000);

        for (Line line : fs) {
            MyLineElement pElem = new MyLineElement(rootElem);
            rootElem.children.add(pElem);
            pElem.addWords(str.length(), 0, line);
            for (BaseItem item : line) {
                str.append(item.getText());
            }
            pElem.addWord(str.length(), pElem.getChildCount(), new SpaceItem("\n"));
            str.append('\n');
        }
        {
            // Ctrl+End hack
            MyLineElement pElem = new MyLineElement(rootElem);
            rootElem.children.add(pElem);
            pElem.addWord(str.length(), 0, new SpaceItem(" "));
        }
        Content c = getContent();
        c.insertString(0, str.toString());
        for (int i = 0; i < rootElem.getChildCount(); i++) {
            MyLineElement p = rootElem.getElement(i);
            for (int j = 0; j < p.getChildCount(); j++) {
                MyWordElement e = p.getElement(j);
                e.createPositions();
            }
        }

        addUndoableEditListener(UI.editorUndoManager);
    }

    public KorpusDocument extractText() {
        KorpusDocument doc = new KorpusDocument();
        for (MyLineElement pd : rootElem.children) {
            Line line = new Line();
            for (MyWordElement el : pd.children) {
                line.add(el.item);
            }
            doc.add(line);
        }
        return doc;
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
        return new MyLineElement(parent);
    }

    @Override
    protected Element createLeafElement(Element parent, AttributeSet a, int p0, int p1) {
        throw new RuntimeException("Not implemented: createLeafElement");
    }

    @Override
    public void insertString(int offs, String str, AttributeSet a) throws BadLocationException {
        writeLock();
        try {
            DefaultDocumentEvent e = new DefaultDocumentEvent(offs, str.length(), DocumentEvent.EventType.INSERT);

            String[] newStrs = splitInserted(str);
            List<Line> newLines = new ArrayList<>();
            for (int i = 0; i < newStrs.length; i++) {
                newLines.add(new Splitter(newStrs[i]).splitParagraph());
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

    BaseItem[] extractItems(MyWordElement[] els) {
        BaseItem[] result = new BaseItem[els.length];
        for (int i = 0; i < els.length; i++) {
            result[i] = els[i].item;
        }
        return result;
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
        MyLineElement[] newParagraphs = new MyLineElement[newLines.size()];
        for (int i = 0; i < newParagraphs.length; i++) {
            MyLineElement p = newParagraphs[i] = new MyLineElement(rootElem);
            newLines.get(i).normalize();
            p.addWords(pOffset, 0, newLines.get(i));
            for (int j = 0; j < p.getChildCount(); j++) {
                p.getElement(j).createPositions();
            }
            rootElem.children.add(pIndex + i, p);
            pOffset = p.getEndOffset();
        }
        return newParagraphs;
    }

    public interface ElementChanger {
        public void replace(int offset, int length, Element[] elems);
    }

    public abstract class MyGroupElement<T extends AbstractElement> extends AbstractElement implements ElementChanger {
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
            return getClass().getSimpleName() + "(" + getName() + ") " + getStartOffset() + "-" + getEndOffset() + "\n";
        }
    }

    protected class MyRootElement extends MyGroupElement<MyLineElement> {
        public MyRootElement() {
            super(null);
        }
    }

    public class MyLineElement extends MyGroupElement<MyWordElement> {

        public MyLineElement(Element parent) {
            super(parent);
        }

        public MyWordElement[] addWords(int startOffset, int atIndex, Line items) {
            MyWordElement[] result = new MyWordElement[items.size()];
            for (int i = 0; i < result.length; i++) {
                result[i] = new MyWordElement(this, startOffset, items.get(i));
                children.add(atIndex, result[i]);
                startOffset += items.get(i).getText().length();
                atIndex++;
            }
            return result;
        }

        public MyWordElement addWord(int startOffset, int atIndex, BaseItem item) {
            MyWordElement result = new MyWordElement(this, startOffset, item);
            children.add(atIndex, result);
            return result;
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
        BaseItem item;

        private transient Position p0;
        private transient Position p1;
        int p0v, p1v;

        public MyWordElement(Element parent, int offs0, BaseItem item) {
            super(parent, null);
            this.item = item;
            p0v = offs0;
            p1v = offs0 + item.getText().length();
        }

        public boolean isTag() {
            return (item instanceof TagShortItem) || (item instanceof TagLongItem);
        }

        public W getWordInfo() {
            if (item instanceof WordItem) {
                return ((WordItem) item).w;
            } else if (item instanceof WordItem) {
                return ((ZnakItem) item).w;
            } else {
                return null;
            }
        }

        public void setWordInfo(W w) {
            if (item instanceof WordItem) {
                ((WordItem) item).w = w;
            } else if (item instanceof WordItem) {
                ((WordItem) item).w = w;
            } else {
                throw new ClassCastException("Trying to set word info into " + item.getClass().getSimpleName());
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
