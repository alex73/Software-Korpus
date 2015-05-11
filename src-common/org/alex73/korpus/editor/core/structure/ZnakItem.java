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

import alex73.corpus.text.Z;

public class ZnakItem extends BaseItem {
    public Z w;

    public ZnakItem(Z w) {
        this.w = w;
    }

    @Override
    public String getText() {
        return w.getValue();
    }

    @Override
    public BaseItem splitLeft(int pos) {
        return null;
    }

    @Override
    public BaseItem splitRight(int pos) {
        return null;
    }
}
