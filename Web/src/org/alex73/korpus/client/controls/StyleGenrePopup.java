/**************************************************************************
 Korpus - Corpus Linguistics Software.

 Copyright (C) 2014 Aleś Bułojčyk (alex73mail@gmail.com)
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

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.alex73.korpus.shared.StyleGenres;

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.ui.Anchor;
import com.google.gwt.user.client.ui.CheckBox;
import com.google.gwt.user.client.ui.DecoratedPopupPanel;
import com.google.gwt.user.client.ui.DecoratorPanel;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.HTMLPanel;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.InlineLabel;
import com.google.gwt.user.client.ui.VerticalPanel;

public class StyleGenrePopup extends DecoratedPopupPanel {
    private List<CheckBox> allCheckboxes = new ArrayList<CheckBox>();

    public StyleGenrePopup(List<String> selected) {
        super(true);

        Set<String> selectedSet = new HashSet<>(selected);
        if (selectedSet.isEmpty()) {
            selectedSet.addAll(StyleGenres.KNOWN_SET);
        }

        setTitle("Стылі і жанры");

        HorizontalPanel dialogContents = new HorizontalPanel();
        dialogContents.setSpacing(4);
        setWidget(dialogContents);

        for (String g : StyleGenres.KNOWN_GROUPS) {
            VerticalPanel panel = new VerticalPanel();
            final String prefix = g + '/';

            HTMLPanel gr = new HTMLPanel("");
            gr.add(new InlineLabel(g));
            final CheckBox cbg = new CheckBox();
            gr.add(cbg);
            panel.add(gr);
            cbg.addClickHandler(new ClickHandler() {
                @Override
                public void onClick(ClickEvent event) {
                    for (CheckBox cb : allCheckboxes) {
                        if (cb.getFormValue().startsWith(prefix)) {
                            cb.setValue(cbg.getValue());
                        }
                    }
                }
            });

            boolean allChecked = true;
            for (String sg : StyleGenres.KNOWN) {
                if (sg.startsWith(prefix)) {
                    CheckBox cb = new CheckBox(sg.substring(prefix.length()));
                    cb.setFormValue(sg);
                    cb.setWordWrap(false);
                    if (!selectedSet.contains(sg)) {
                        allChecked = false;
                    }
                    cb.setValue(selectedSet.contains(sg));
                    panel.add(cb);
                    allCheckboxes.add(cb);
                }
            }
            if (allChecked) {
                cbg.setValue(true);
            }
            DecoratorPanel dp = new DecoratorPanel();
            dp.setWidget(panel);
            dialogContents.add(dp);
        }

        VerticalPanel panel = new VerticalPanel();

        CheckBox cb = new CheckBox("Іншыя");
        cb.setFormValue(StyleGenres.KNOWN_OTHER);
        cb.setWordWrap(false);
        cb.setValue(selectedSet.contains(StyleGenres.KNOWN_OTHER));
        panel.add(cb);
        allCheckboxes.add(cb);

        panel.add(new HTML("<br/>"));

        Anchor selectAll = new Anchor("Пазначыць усе");
        selectAll.setWordWrap(false);
        panel.add(selectAll);

        Anchor deselectAll = new Anchor("Зняць усе");
        deselectAll.setWordWrap(false);
        panel.add(deselectAll);

        dialogContents.add(panel);

        selectAll.addClickHandler(new ClickHandler() {
            @Override
            public void onClick(ClickEvent event) {
                for (CheckBox cb : allCheckboxes) {
                    cb.setValue(true);
                }
            }
        });
        deselectAll.addClickHandler(new ClickHandler() {
            @Override
            public void onClick(ClickEvent event) {
                for (CheckBox cb : allCheckboxes) {
                    cb.setValue(false);
                }
            }
        });
    }

    public List<String> getSelected() {
        List<String> result = new ArrayList<>();
        for (CheckBox cb : allCheckboxes) {
            if (cb.getValue()) {
                result.add(cb.getFormValue());
            }
        }
        return result;
    }
}
