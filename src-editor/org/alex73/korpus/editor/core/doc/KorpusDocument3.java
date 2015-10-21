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
import org.alex73.korpus.editor.core.structure.ItemHelper;
import org.alex73.korpus.editor.core.structure.KorpusDocument;
import org.alex73.korpus.editor.core.structure.Line;
import org.alex73.korpus.editor.core.structure.LongTagItem;
import org.alex73.korpus.editor.core.structure.SentenceSeparatorItem;
import org.alex73.korpus.parser.Splitter;
import org.alex73.korpus.parser.TextParser;

import alex73.corpus.text.InlineTag;
import alex73.corpus.text.P;
import alex73.corpus.text.S;
import alex73.corpus.text.Se;
import alex73.corpus.text.Tag;
import alex73.corpus.text.W;
import alex73.corpus.text.XMLText;
import alex73.corpus.text.Z;

/**
 * Рэдактар дакумэнту корпуса.
 */
public class KorpusDocument3 extends AbstractDocument {
    public enum MARK_WORDS {
        UNK_LEMMA, AMAN_LEMMA, AMAN_GRAM
    };

    MyRootElement rootElem;
    public MARK_WORDS markType = MARK_WORDS.UNK_LEMMA;

private     StringBuilder text = new StringBuilder(100000);

    
    public KorpusDocument3(XMLText fs) throws Exception {
        super(new GapContent(65536), new StyleContext());

        rootElem = new MyRootElement();


        for (Object line : fs.getContent().getPOrTag()) {
            MyLineElement pLine = new MyLineElement(rootElem);
            rootElem.children.add(pLine);
            if (line instanceof P) {
                P p=(P)line;
                for(Se sentence:p.getSe()) {
                    for(Object inc: sentence.getWOrSOrZ()) {
                        MyWordElement we;
                        if (inc instanceof W) {
                            we = new MyWordElement(pLine,  inc);
                        }else if (inc instanceof S) {
                            we = new MyWordElement(pLine,  inc);
                        }else if (inc instanceof Z) {
                            we = new MyWordElement(pLine,  inc);
                        }else  {
                            throw new RuntimeException("Wrong tag");
                        }
                        pLine.add(we);
                    }
                    pLine.add(new MyWordElement(pLine, new SentenceSeparatorItem()));
                }
            } else if (line instanceof Tag) {
                Tag tag=(Tag)line;
                MyLineElement pElem = new MyLineElement(rootElem);
                rootElem.children.add(pElem);
                pElem.add(new MyWordElement(pElem, new LongTagItem("##"+tag.getName()+": "+tag.getValue())));
            }else {
                throw new RuntimeException("Wrong tag");
            }
            text.append('\n');
        }
        {
            // Ctrl+End hack
            MyLineElement pElem = new MyLineElement(rootElem);
            rootElem.children.add(pElem);
            pElem.add(new MyWordElement(pElem, ItemHelper.createS(" ")));
        }
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
        KorpusDocument doc = new KorpusDocument();
        for (MyLineElement pd : rootElem.children) {
            Line line = new Line();
            for (MyWordElement el : pd.children) {
                line.add(el.item);
            }
            doc.add(line);
        }
        return TextParser.constructXML(doc);
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
            DefaultDocumentEvent e = new DefaultDocumentEvent(offs, str.length(),
                    DocumentEvent.EventType.INSERT);

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
            Line newLine = 
            newLines.get(i);
            newLine.normalize();
            
            int startOffset=0;
            for(Object item:newLine) {
               p.add(new MyWordElement(p,startOffset, item));
               startOffset+=ItemHelper.getText(item).length();
            }
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

    @SuppressWarnings("serial")
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

    @SuppressWarnings("serial")
    protected class MyRootElement extends MyGroupElement<MyLineElement> {
        public MyRootElement() {
            super(null);
        }
    }

    @SuppressWarnings("serial")
    public class MyLineElement extends MyGroupElement<MyWordElement> {

        public MyLineElement(Element parent) {
            super(parent);
        }


        public void add(MyWordElement elem) {
            children.add(elem);
        }

        public Line extractItems() {
            Line result = new Line();
            for (int i = 0; i < children.size(); i++) {
                result.add(children.get(i).item);
            }
            return result;
        }
    }

    @SuppressWarnings("serial")
    public class MyWordElement extends AbstractElement {
        public final Object item;

        private transient Position p0;
        private transient Position p1;
        int p0v, p1v;

        public MyWordElement(Element parent,  Object item) {
            super(parent, null);
            this.item = item;
            p0v = text.length();
            text.append(ItemHelper.getText(item));
            p1v = text.length() ;
        }

        public MyWordElement(Element parent, int startOffset, Object item) {
            super(parent, null);
            this.item = item;
            p0v = startOffset;
            p1v = startOffset+ItemHelper.getText(item) .length();
        }
        

        public boolean isTag() {
            return (item instanceof InlineTag) || (item instanceof LongTagItem);
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
                W w=(W)item;
                w.setCat(otherW.getCat());
                w.setLemma(otherW.getLemma());
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
