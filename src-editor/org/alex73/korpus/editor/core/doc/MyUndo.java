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

import javax.swing.event.DocumentEvent;
import javax.swing.text.Element;
import javax.swing.undo.AbstractUndoableEdit;
import javax.swing.undo.CannotRedoException;
import javax.swing.undo.CannotUndoException;

public class MyUndo extends AbstractUndoableEdit implements DocumentEvent.ElementChange {

    public MyUndo(Element e, int index, Element[] removed, Element[] added) {
        super();
        this.e = e;
        this.index = index;
        this.removed = removed;
        this.added = added;
    }

    public Element getElement() {
        return e;
    }

    public int getIndex() {
        return index;
    }

    public Element[] getChildrenRemoved() {
        return removed;
    }

    public Element[] getChildrenAdded() {
        return added;
    }

    public void redo() throws CannotRedoException {
        super.redo();

        // Since this event will be reused, switch around added/removed.
        Element[] tmp = removed;
        removed = added;
        added = tmp;

        // PENDING(prinz) need MutableElement interface, canRedo() should check
        ((KorpusDocument3.ElementChanger) e).replace(index, removed.length, added);
    }


    public void undo() throws CannotUndoException {
        super.undo();
        // PENDING(prinz) need MutableElement interface, canUndo() should check
        ((KorpusDocument3.ElementChanger) e).replace(index, added.length, removed);

        // Since this event will be reused, switch around added/removed.
        Element[] tmp = removed;
        removed = added;
        added = tmp;
    }

    private Element e;
    private int index;
    private Element[] removed;
    private Element[] added;
}