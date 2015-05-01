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

package org.alex73.korpus.client;

import java.util.List;

import org.alex73.korpus.base.DBTagsGroups;

import com.google.gwt.core.client.EntryPoint;
import com.google.gwt.user.client.Window;
/**
 * Show initial UI.
 */
public class Main implements EntryPoint {

    public void onModuleLoad() {
        patchData();

        String url = Window.Location.getHref();
        if (url.contains("grammarDB.html")) {
            new GrammarDB().onModuleLoad();
        } else if (url.contains("otherSearch.html")) {
            new Korpus(false).onModuleLoad();
        } else {
            new Korpus(true).onModuleLoad();
        }
    }

    void patchData() {
        List<DBTagsGroups.Group> n = DBTagsGroups.getTagGroupsByWordType().get('N').groups;
        for (int i = 0; i < n.size(); i++) {
            if (n.get(i).name.equals("Скарот")) {
                n.get(i).hidden = true;
                break;
            }
        }
        for (DBTagsGroups group : DBTagsGroups.getTagGroupsByWordType().values()) {
            for (DBTagsGroups.Group g : group.groups) {
                for (int i = 0; i < g.items.size(); i++) {
                    DBTagsGroups.Item item = g.items.get(i);
                    if (item.description.contains("???") || item.code == '_') {
                        g.items.remove(i);
                        i--;
                    }
                }
            }
        }
    }
}
