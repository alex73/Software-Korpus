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

package org.alex73.korpus.client.controls;

/**
 * Grammar checkboxes handler.
 */
public class WordType {
    public String name;
    public char code;
    public Group[] groups;

    public static class Group {
        public String name;
        public Value[] values;

        public Group(String name, Object... v) {
            this.name = name;
            values = new Value[v.length / 2];
            for (int i = 0; i < values.length; i++) {
                values[i] = new Value();
                values[i].name = (String) v[i * 2];
                values[i].code = (Character) v[i * 2 + 1];
            }
        }
    }

    public static class Value {
        public String name;
        public char code;
    }

    public WordType(String name, char code, Group... groups) {
        this.name = name;
        this.code = code;
        this.groups = groups;
    }
}
