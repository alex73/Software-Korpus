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

package org.alex73.korpus.editor.core.doc.structure;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.alex73.korpus.text.structure.corpus.Word.OtherType;
import org.alex73.korpus.text.structure.files.ITextLineElement;
import org.alex73.korpus.text.structure.files.InlineTag;
import org.alex73.korpus.text.structure.files.LongTagItem;
import org.alex73.korpus.text.structure.files.SentenceSeparatorItem;
import org.alex73.korpus.text.structure.files.TailItem;
import org.alex73.korpus.text.structure.files.TextLine;
import org.alex73.korpus.text.structure.files.WordItem;

/**
 * Сховішча для радку дакумэнту корпуса.
 */
@SuppressWarnings("serial")
public class Line {
    public static void fillWordsInfo(TextLine line) {
        for (ITextLineElement item : line) {
            if (item instanceof WordItem) {
                WordItem wi = (WordItem) item;
                if (wi.lemmas == null) {
                    //TODO MainController.gr.filler.fill(wi);
                }
            }
        }
    }

    private static void splitAt(TextLine line, int offset) {
        int pos = 0;
        for (int i = 0; i < line.size(); i++) {
            ITextLineElement item = line.get(i);
            int len = item.getText().length();
            if (pos < offset && offset < pos + len) {
                // inside item
                ITextLineElement itLeft = ItemHelper.splitLeft(item, offset - pos);
                ITextLineElement itRight = ItemHelper.splitRight(item, offset - pos);
                line.remove(i);
                line.add(i, itLeft);
                line.add(i + 1, itRight);
                break;
            }
            pos += len;
        }
    }

    public static int length(TextLine line) {
        int len = 0;
        for (int i = 0; i < line.size(); i++) {
            len += line.get(i).getText().length();
        }
        return len;
    }

    /*public void insertItemAt(int offset, ITextLineElement newItem) {
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
    }*/

    public static TextLine leftAt(TextLine line, int offset) {
        splitAt(line, offset);
        TextLine result = new TextLine();
        int pos = 0;
        for (int i = 0; i < line.size(); i++) {
            if (pos == offset) {
                return result;
            }
            result.add(line.get(i));
            pos += line.get(i).getText().length();
        }
        throw new RuntimeException("Invalid leftAt");
    }

    public static TextLine rightAt(TextLine line, int offset) {
        splitAt(line, offset);
        TextLine result = null;
        int pos = 0;
        for (int i = 0; i < line.size(); i++) {
            if (pos == offset) {
                result = new TextLine();
            }
            if (result != null) {
                result.add(line.get(i));
            }
            pos += line.get(i).getText().length();
        }
        if (result == null) {
            throw new RuntimeException("Invalid rightAt");
        }
        return result;
    }

    public static void normalize(TextLine line) {
        while (mergeAndSplitItems(line))
            ;
        fillWordsInfo(line);
    }

    static final Pattern RE_TAG = Pattern.compile("<.+?>");
    static final Pattern RE_DIGITS = Pattern.compile("[0-9]+");
    
    public static boolean mergeAndSplitItems(TextLine line) {
        boolean modified = false;
        for (int i = 0; i < line.size(); i++) {
            // convert non-tags to words
            ITextLineElement currentItem = line.get(i);
            if (currentItem instanceof InlineTag) {
                String text = ((InlineTag) currentItem).text;
                if (!RE_TAG.matcher(text).matches()) {
                      currentItem =new WordItem(text);
                    line.set(i, currentItem);
                    modified = true;
                }
            } else if (currentItem instanceof LongTagItem) {
                String text = currentItem.getText();
                if (!text.startsWith("##")) {
                    currentItem = new WordItem(text);
                    line.set(i, currentItem);
                    modified = true;
                }
            }
        }
        if (line.size() > 0) {
            // convert words to tags
            ITextLineElement currentItem = line.get(0);
            if (currentItem instanceof WordItem) {
                String text = currentItem.getText();
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
            if (currentItem instanceof WordItem && nextItem instanceof WordItem) {
                WordItem wi=  new WordItem(currentItem.getText() + nextItem.getText());
                newItem =wi;
            }
            if (currentItem instanceof TailItem && nextItem instanceof TailItem) {
                TailItem wi=  new TailItem(currentItem.getText() + nextItem.getText());
                newItem =wi;
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
            if (currentItem instanceof WordItem) {
                WordItem w = (WordItem) currentItem;
                if (w.lightNormalized.startsWith("'")) {
                    TailItem newItem = new TailItem();
                    newItem.text = "'";
                    w = (WordItem) ItemHelper.splitRight(w, 1);
                    line.set(i, w);
                    line.add(i, newItem);
                    modified = true;
                }
                if (w.lightNormalized.endsWith("'")) {
                    TailItem newItem = new TailItem();
                    newItem.text = "'";
                    w = (WordItem) ItemHelper.splitLeft(w, w.lightNormalized.length() - 1);
                    line.set(i, w);
                    line.add(i + 1, newItem);
                    i--;
                    modified = true;
                }
            }
        }
        for (int i = 0; i < line.size(); i++) {
            // split words by tags
            ITextLineElement currentItem = line.get(i);
            if (currentItem instanceof WordItem) {
                String text = currentItem.getText();
                Matcher m = RE_TAG.matcher(text);
                if (m.find()) {
                    String textBefore = text.substring(0, m.start());
                    String textIn = text.substring(m.start(), m.end());
                    String textAfter = text.substring(m.end());
                    line.set(i, new WordItem(textBefore));
                    line.add(i + 1, new InlineTag(textIn));
                    line.add(i + 2, new WordItem(textAfter));
                    modified = true;
                }
            }
        }
        for (int i = 0; i < line.size() - 1; i++) {
            // merge numbers
            ITextLineElement currentItem = line.get(i);
            ITextLineElement nextItem = line.get(i + 1);
            if (currentItem instanceof WordItem && nextItem instanceof WordItem) {
                if (RE_DIGITS.matcher( currentItem.getText()).matches()) {
                    if (RE_DIGITS.matcher(nextItem.getText()).matches()) {
                        WordItem w = new WordItem(currentItem.getText() + nextItem.getText());
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


    public static TextLine splitOther(String line, OtherType type) {
        TextLine result = new TextLine();
        int partStart=0;
        int currentPos;
        for (currentPos = 0; currentPos < line.length(); currentPos++) {
            char ch = line.charAt(currentPos);
            if (ch == SentenceSeparatorItem.SENTENCE_SEPARATOR_CHAR) {
                String part = line.substring(partStart, currentPos);
                WordItem wi=new WordItem();
                wi.lightNormalized = part;
                wi.type = type;
                result.add(wi);
                result.add(new SentenceSeparatorItem());
                partStart = currentPos + 1;
            }
        }
        String part = line.substring(partStart, currentPos);
        WordItem wi=new WordItem();
        wi.lightNormalized = part;
        wi.type = type;
        result.add(wi);

        normalize(result);
        return result;
    }
}
