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

package org.alex73.korpus.shared;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

/**
 * DTO for grammar database search results.
 */
public class LemmaInfo {
    public long pdgId;
    public String output;
    public String meaning;
    public String grammar;

    public static class LemmaParadigm {
        public String lemma;
        public String tag;
        public String meaning;
        public List<LemmaVariant> variants = new ArrayList<>();
    }

    public static class LemmaVariant {
        public String id;
        public String tag;
        public List<LemmaForm> forms = new ArrayList<>();
        public Set<String> dictionaries = new TreeSet<>();
        public List<Author> authors = new ArrayList<>();
    }

    public static class LemmaForm {
        public String tag;
        public String value;
        public String options;
    }

    public static class Author {
        public String name;
        public String displayName;
    }
}
