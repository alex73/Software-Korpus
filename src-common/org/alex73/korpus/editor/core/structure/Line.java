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

package org.alex73.korpus.editor.core.structure;

import java.util.ArrayList;
import java.util.Arrays;

import org.alex73.korpus.parser.Splitter;

/**
 * Сховішча для радку дакумэнту корпуса.
 */
public class Line extends ArrayList<BaseItem> {
    public Line() {
    }

    public Line(BaseItem[] items) {
        super(Arrays.asList(items));
    }

    void splitAt(int offset) {
        int pos = 0;
        for (int i = 0; i < size(); i++) {
            BaseItem item = get(i);
            int len = item.getText().length();
            if (pos < offset && offset < pos + len) {
                // inside item
                BaseItem itLeft = item.splitLeft(offset - pos);
                BaseItem itRight = item.splitRight(offset - pos);
                remove(i);
                add(i, itLeft);
                add(i + 1, itRight);
                break;
            }
            pos += len;
        }
    }

    public int length() {
        int len = 0;
        for (int i = 0; i < size(); i++) {
            len += get(i).getText().length();
        }
        return len;
    }

    public void insertItemAt(int offset, BaseItem newItem) {
        int pos = 0;
        for (int i = 0; i <= size(); i++) {
            if (pos == offset) {
                add(i, newItem);
                return;
            }
            pos += get(i).getText().length();
        }
        throw new RuntimeException("Invalid insertItemAt");
    }

    public void removeItemsAt(int offset, int length) {
        int pos = 0;
        for (int i = 0; i <= size(); i++) {
            if (pos == offset) {
                while (length > 0) {
                    int itlen = get(i).getText().length();
                    if (length < itlen) {
                        throw new RuntimeException("Invalid removeItems");
                    }
                    remove(i);
                    length -= itlen;
                }
                return;
            }
            pos += get(i).getText().length();
        }
        throw new RuntimeException("Invalid removeItemsAt");
    }

    public Line leftAt(int offset) {
        splitAt(offset);
        Line result = new Line();
        int pos = 0;
        for (int i = 0; i < size(); i++) {
            if (pos == offset) {
                return result;
            }
            result.add(get(i));
            pos += get(i).getText().length();
        }
        throw new RuntimeException("Invalid leftAt");
    }

    public Line rightAt(int offset) {
        splitAt(offset);
        Line result = null;
        int pos = 0;
        for (int i = 0; i < size(); i++) {
            if (pos == offset) {
                result = new Line();
            }
            if (result != null) {
                result.add(get(i));
            }
            pos += get(i).getText().length();
        }
        if (result == null) {
            throw new RuntimeException("Invalid rightAt");
        }
        return result;
    }

    public void normalize() {
        while (Splitter.mergeAndSplitItems(this))
            ;
        Splitter.fillWordsInfo(this);
    }

    @Override
    public String toString() {
        return super.toString() + " length=" + length();
    }
}