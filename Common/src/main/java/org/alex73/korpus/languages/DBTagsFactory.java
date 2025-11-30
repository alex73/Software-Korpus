/**************************************************************************
 Korpus - Corpus Linguistics Software.

 Copyright (C) 2013 Aleś Bułojčyk (alex73mail@gmail.com)

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

package org.alex73.korpus.languages;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;

import org.alex73.grammardb.tags.BelarusianTags;
import org.alex73.grammardb.tags.IGrammarTags;
import org.alex73.grammardb.tags.TagLetter;
import org.alex73.korpus.languages.ILanguage.IDBTags;

/**
 * Зьбірае усе магчымыя назвы груп і ўсе магчымыя літары тэгаў ва ўсіх
 * варыянтах.
 */
public class DBTagsFactory implements IDBTags {
    private final IGrammarTags grammarTags;
    public final List<KeyValue> wordTypes = new ArrayList<KeyValue>();
    public final Map<Character, DBTagsGroup> tagGroupsByWordType = new TreeMap<Character, DBTagsGroup>();

    protected DBTagsFactory(IGrammarTags grammarTags) {
        this.grammarTags = grammarTags;
        for (TagLetter.OneLetterInfo li : grammarTags.getRoot().letters) {
            wordTypes.add(new KeyValue(Character.toString(li.letter), li.description));
            tagGroupsByWordType.put(li.letter, new DBTagsGroup(li.nextLetters));
        }
    }

    public List<KeyValue> getWordTypes() {
        return wordTypes;
    }

    public Map<Character, DBTagsGroup> getTagGroupsByWordType() {
        return tagGroupsByWordType;
    }

    private Map<String, String> cache = new ConcurrentHashMap<>();

    public String getDBTagString(String grammarTag) {
        String r = cache.get(grammarTag);
        if (r != null) {
            return r;
        }
        DBTagsGroup wt = tagGroupsByWordType.get(grammarTag.charAt(0));
        char[] result = new char[wt.groups.size() + 1];
        result[0] = grammarTag.charAt(0);
        for (int i = 1; i < result.length; i++) {
            result[i] = '_';
        }

        TagLetter tags = grammarTags.getNextAfter(grammarTag.substring(0, 1));
        for (int i = 1; i < grammarTag.length(); i++) {
            char ch = grammarTag.charAt(i);
            if (ch == 'x') {// TODO
                ch = tags.letters.get(0).letter;
            }
            TagLetter.OneLetterInfo li = tags.getLetterInfo(ch);
            assert (li != null);
            int pos = wt.getGroupIndex(li.groupName);
            result[pos + 1] = ch;
            tags = li.nextLetters;
        }
        r = new String(result);
        cache.put(grammarTag, r);
        return r;
    }

    public static class Group {
        public final String name;
        public final boolean formGroup;
        public List<Item> items = new ArrayList<Item>(20);

        public Group(String name, boolean formGroup) {
            this.name = name;
            this.formGroup = formGroup;
        }

        Item getItem(char code) {
            for (int i = 0; i < items.size(); i++) {
                if (code == items.get(i).code) {
                    return items.get(i);
                }
            }
            return null;
        }
    }

    public static class Item {
        public final char code;
        public String description = BelarusianTags.NO_GROUP_ITEM;

        public Item(char code) {
            this.code = code;
        }
    }

    public static class DBTagsGroup {
        public List<Group> groups = new ArrayList<Group>();

        DBTagsGroup(TagLetter tags) {
            collectGroups(tags, false);

            char[] codes = new char[groups.size()];
            for (int i = 0; i < codes.length; i++) {
                codes[i] = '_';
            }
            collectItems(tags, codes);

            collectDescriptions(tags);
        }

        private Group getGroup(String name) {
            for (int i = 0; i < groups.size(); i++) {
                if (name.equals(groups.get(i).name)) {
                    return groups.get(i);
                }
            }
            return null;
        }

        private int getGroupIndex(String name) {
            for (int i = 0; i < groups.size(); i++) {
                if (name.equals(groups.get(i).name)) {
                    return i;
                }
            }
            return -1;
        }

        private void collectGroups(TagLetter tags, boolean formGroup) {
            for (TagLetter.OneLetterInfo li : tags.letters) {
                Group gr = getGroup(li.groupName);
                if (gr == null) {
                    gr = new Group(li.groupName, formGroup || tags.isLatestInParadigm());
                    groups.add(gr);
                }
            }
            for (TagLetter.OneLetterInfo li : tags.letters) {
                collectGroups(li.nextLetters, formGroup || tags.isLatestInParadigm());
            }
        }

        private void collectItems(TagLetter tags, char[] codes) {
            if (tags.isFinish()) {
                for (int i = 0; i < codes.length; i++) {
                    Group gr = groups.get(i);
                    if (gr.getItem(codes[i]) == null) {
                        gr.items.add(new Item(codes[i]));
                    }
                }
            } else {
                char[] newCodes = new char[codes.length];
                for (int i = 0; i < newCodes.length; i++) {
                    newCodes[i] = codes[i];
                }
                for (TagLetter.OneLetterInfo li : tags.letters) {
                    int grIndex = getGroupIndex(li.groupName);
                    newCodes[grIndex] = li.letter;
                    collectItems(li.nextLetters, newCodes);
                }
            }
        }

        private void collectDescriptions(TagLetter tags) {
            for (TagLetter.OneLetterInfo li : tags.letters) {
                Group gr = getGroup(li.groupName);
                for (Item it : gr.items) {
                    if (li.letter == it.code) {
                        it.description = li.description;
                        break;
                    }
                }
            }
            for (TagLetter.OneLetterInfo li : tags.letters) {
                collectDescriptions(li.nextLetters);
            }
        }
    }

    public static class KeyValue {
        public final String key;
        public final String value;

        public KeyValue(String key, String value) {
            this.key = key;
            this.value = value;
        }
    }

    public static void main(String[] args) throws Exception {
        DBTagsFactory g = new DBTagsFactory(LanguageFactory.get("bel").getTags());
        for (Group a : g.tagGroupsByWordType.get('N').groups) {
            System.out.print(a.name + (a.formGroup ? "[form]" : "") + " : ");
            for (Item it : a.items) {
                System.out.print(it.code + "/" + it.description + " ");
            }
            System.out.println();
        }

        System.out.println(g.getDBTagString("NCIXNF2NS"));
    }
}
