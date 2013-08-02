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

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Rectangle;
import java.awt.Shape;

import javax.swing.text.BoxView;
import javax.swing.text.Element;
import javax.swing.text.GlyphView;
import javax.swing.text.ParagraphView;
import javax.swing.text.View;
import javax.swing.text.ViewFactory;

public class KorpusDocumentViewFactory implements ViewFactory {
    static final Color TAG_BACKGROUND_COLOR = new Color(224, 224, 224);

    public View create(Element elem) {
        if (elem instanceof KorpusDocument3.MyWordElement) {
            return new MyGlyphView(elem);
        } else if (elem instanceof KorpusDocument3.MyLineElement) {
            return new MyParagraphView(elem);
        } else if (elem instanceof KorpusDocument3.MyRootElement) {
            return new BoxView(elem, View.Y_AXIS);
        } else {
            throw new RuntimeException("Unknown element: " + elem.getClass().getSimpleName());
        }
        // String kind = elem.getName();
        // if (kind != null) {
        // if (kind.equals(AbstractDocument.ContentElementName)) {
        // return new MyGlyphView(elem);
        // } else if (kind.equals(AbstractDocument.ParagraphElementName)) {
        // return new MyParagraphView(elem);
        // } else if (kind.equals(AbstractDocument.SectionElementName)) {
        // return new BoxView(elem, View.Y_AXIS);
        // } else if (kind.equals(StyleConstants.ComponentElementName)) {
        // return new ComponentView(elem);
        // } else if (kind.equals(StyleConstants.IconElementName)) {
        // return new IconView(elem);
        // }
        // }
        //
        // // default to text display
        // return new MyGlyphView(elem);
    }

    public static class MyGlyphView extends GlyphView {
        public MyGlyphView(Element elem) {
            super(elem);
        }

        @Override
        public Color getBackground() {
            KorpusDocument3.MyWordElement wordElement = (KorpusDocument3.MyWordElement) getElement();
            return wordElement.isTag() ? TAG_BACKGROUND_COLOR : super.getBackground();
        }

        @Override
        public void paint(Graphics g, Shape a) {
            KorpusDocument3.MyWordElement wordElement = (KorpusDocument3.MyWordElement) getElement();
            Rectangle r = a.getBounds();

            g.setColor(new Color(192, 192, 255));
            g.drawLine(r.x, r.y, r.x, r.y + r.height - 1);

            boolean mark;
            if (wordElement.getWordInfo() == null) {
                mark = false;
            } else if (wordElement.getWordInfo().isManual()) {
                g.setColor(new Color(224, 255, 224));
                g.fillRect(r.x, r.y, r.width, r.height);
                mark = false;
            } else {
                mark = wordElement.isMarked();
            }
            if (mark) {
                g.setColor(Color.RED);
                int y = r.y + r.height - 1;
                g.drawLine(r.x, y, r.x + r.width, y);
            }

            super.paint(g, a);
        }

        @Override
        public int getBreakWeight(int axis, float pos, float len) {
            int r;
            if (axis == View.X_AXIS) {
                checkPainter();
                int p0 = getStartOffset();
                int p1 = getGlyphPainter().getBoundedPosition(this, p0, pos, len);
                return p1 == getEndOffset() ? View.ExcellentBreakWeight : View.BadBreakWeight;
            } else {
                r = super.getBreakWeight(axis, pos, len);
            }

            return r;
        }
    }

    public static class MyParagraphView extends ParagraphView {
        public MyParagraphView(Element elem) {
            super(elem);
            setFirstLineIndent(30);
            setInsets((short) 0, (short) 0, (short) 3, (short) 0);
        }

        @Override
        public void paint(Graphics g, Shape a) {
            super.paint(g, a);
            g.setColor(Color.BLUE);
            Rectangle r = a.getBounds();
            g.drawLine(r.x, r.y + r.height, r.x + r.width, r.y + r.height);
        }
    }
}
