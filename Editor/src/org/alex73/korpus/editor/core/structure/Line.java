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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.alex73.korpus.editor.Splitter;
import org.alex73.korpus.text.xml.ITextLineElement;
import org.alex73.korpus.text.xml.InlineTag;
import org.alex73.korpus.text.xml.O;
import org.alex73.korpus.text.xml.OtherType;
import org.alex73.korpus.text.xml.W;
import org.alex73.korpus.utils.ItemHelper;

/**
 * Сховішча для радку дакумэнту корпуса.
 */
@SuppressWarnings("serial")
public class Line extends ArrayList<ITextLineElement> {

    void splitAt(int offset) {
        int pos = 0;
        for (int i = 0; i < size(); i++) {
            ITextLineElement item = get(i);
            int len = item.getText().length();
            if (pos < offset && offset < pos + len) {
                // inside item
                ITextLineElement itLeft = ItemHelper.splitLeft(item, offset - pos);
                ITextLineElement itRight = ItemHelper.splitRight(item, offset - pos);
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

    public void insertItemAt(int offset, ITextLineElement newItem) {
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
        while (mergeAndSplitItems(this))
            ;
        Splitter.fillWordsInfo(this);
    }

    @Override
    public String toString() {
        return super.toString() + " length=" + length();
    }
    
    static final Pattern RE_TAG = Pattern.compile("<.+?>");
    static final Pattern RE_DIGITS = Pattern.compile("[0-9]+");
    
    public static boolean mergeAndSplitItems(Line line) {
        boolean modified = false;
        for (int i = 0; i < line.size(); i++) {
            // convert non-tags to words
            ITextLineElement currentItem = line.get(i);
            if (currentItem instanceof InlineTag) {
                String text = ((InlineTag) currentItem).getValue();
                if (!RE_TAG.matcher(text).matches()) {
                    currentItem = new W(text);
                    line.set(i, currentItem);
                    modified = true;
                }
            } else if (currentItem instanceof LongTagItem) {
                String text = currentItem.getText();
                if (!text.startsWith("##")) {
                    currentItem = new W(text);
                    line.set(i, currentItem);
                    modified = true;
                }
            }
        }
        if (line.size() > 0) {
            // convert words to tags
            ITextLineElement currentItem = line.get(0);
            if (currentItem instanceof W) {
                String text = ((W) currentItem).getValue();
                if (text.startsWith("##")) {
                    currentItem = new LongTagItem(text);
                    line.set(0, currentItem);
                    modified = true;
                }
            }
        }
        while (line.size() > 2 && (line.get(0) instanceof LongTagItem)) {
            // merge big tags
            String newTagText = line.get(0).getText() + line.get(1).getText();
            line.set(0, new LongTagItem(newTagText));
            line.remove(1);
            modified = true;
        }
        for (int i = 0; i < line.size() - 1; i++) {
            // merge words and spaces
            ITextLineElement currentItem = line.get(i);
            ITextLineElement nextItem = line.get(i + 1);
            ITextLineElement newItem = null;
            if (currentItem instanceof W && nextItem instanceof W) {
                newItem =  new W(currentItem.getText() + nextItem.getText());
            }
            if (newItem != null) {
                line.remove(i);
                line.remove(i);
                line.add(i, newItem);
                i--;
                modified = true;
            }
        }
        for (int i = 0; i < line.size() - 1; i++) {
            // split words and apostrophes
            ITextLineElement currentItem = line.get(i);
            if (currentItem instanceof W) {
                ITextLineElement newItem = null;
                W w = (W) currentItem;
                if (w.getValue().startsWith("'")) {
                    newItem = Splitter.getZnakInfo("'");
                    w = (W) ItemHelper.splitRight(w, 1);
                    line.set(i, w);
                    line.add(i, newItem);
                    modified = true;
                }
                if (w.getValue().endsWith("'")) {
                    newItem = Splitter.getZnakInfo("'");
                    w = (W) ItemHelper.splitLeft(w, w.getValue().length() - 1);
                    line.set(i, w);
                    line.add(i + 1, newItem);
                    i--;
                    modified = true;
                }
            }
        }
        for (int i = 0; i < line.size(); i++) {
            // split words by tags
            Object currentItem = line.get(i);
            if (currentItem instanceof W) {
                String text = ((W) currentItem).getValue();
                Matcher m = RE_TAG.matcher(text);
                if (m.find()) {
                    String textBefore = text.substring(0, m.start());
                    String textIn = text.substring(m.start(), m.end());
                    String textAfter = text.substring(m.end());
                    line.set(i, new W(textBefore));
                    line.add(i + 1, new InlineTag(textIn));
                    line.add(i + 2, new W(textAfter));
                    modified = true;
                }
            }
        }
        for (int i = 0; i < line.size() - 1; i++) {
            // merge numbers
            Object currentItem = line.get(i);
            Object nextItem = line.get(i + 1);
            if (currentItem instanceof W && nextItem instanceof W) {
                if (RE_DIGITS.matcher(((W) currentItem).getValue()).matches()) {
                    if (RE_DIGITS.matcher(((W) nextItem).getValue()).matches()) {
                        W w = new W(((W) currentItem).getValue() + ((W) nextItem).getValue());
                        line.remove(i);
                        line.remove(i);
                        line.add(i, w);
                        i--;
                        modified = true;
                    }
                }
            }
        }
        return modified;
    }


    public static Line splitOther(String line, OtherType type) {
        Line result=new Line();
        int partStart=0;
        int currentPos;
        for (currentPos = 0; currentPos < line.length(); currentPos++) {
            char ch = line.charAt(currentPos);
            if (ch == Splitter.CH_SENT_SEPARATOR) {
                String part = line.substring(partStart, currentPos);
                result.add(new O(type, part));
                result.add(new SentenceSeparatorItem());
                partStart = currentPos + 1;
            }
        }
        String part = line.substring(partStart, currentPos);
        result.add(new O(type, part));

        result.normalize();
        return result;
    }

}