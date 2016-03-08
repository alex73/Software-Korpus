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

import java.util.ArrayList;
import java.util.List;

import org.alex73.korpus.base.DBTagsGroups;

import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.Element;
import com.google.gwt.user.client.ui.CellPanel;
import com.google.gwt.user.client.ui.CheckBox;
import com.google.gwt.user.client.ui.DecoratedPopupPanel;
import com.google.gwt.user.client.ui.DecoratorPanel;
import com.google.gwt.user.client.ui.HasAlignment;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;

/**
 * Popup for select grammar checkboxes.
 */
public class WordGrammarPopup extends DecoratedPopupPanel {
    List<List<CheckBox>> groupsWidgets = new ArrayList<List<CheckBox>>();

    public WordGrammarPopup(DBTagsGroups wt) {
        super(true);
        setTitle("Граматыка");

        // calculate count without hiddens
        int count=0;
        for (DBTagsGroups.Group g : wt.groups) {
            if (!g.hidden) {
                count++;
            }
        }
        CellPanel dialogContents = count <= 6 ? new HorizontalPanel() : new TwoRowPanel(count);
        dialogContents.setSpacing(4);
        setWidget(dialogContents);

        for (DBTagsGroups.Group g : wt.groups) {
            VerticalPanel panel = new VerticalPanel();
            panel.add(new Label(g.name));

            List<CheckBox> groupCheckboxes = new ArrayList<CheckBox>();
            groupsWidgets.add(groupCheckboxes);
            for (DBTagsGroups.Item v : g.items) {
                CheckBox cb = new CheckBox(v.description + " (" + v.code + ")");
                cb.setFormValue("" + v.code);
                cb.setWordWrap(false);
                panel.add(cb);
                groupCheckboxes.add(cb);
            }
            DecoratorPanel dp = new DecoratorPanel();
            dp.setWidget(panel);
            if (!g.hidden) {
                dialogContents.add(dp);
            }
        }
        // Button btnClose = new Button("X");
        // btnClose.addClickHandler(new ClickHandler() {
        // @Override
        // public void onClick(ClickEvent event) {
        // hide();
        // }
        // });
        // dialogContents.add(btnClose);
    }

    public void setSelected(String sel) {
        if (sel == null || sel.length() == 0) {
            return;
        }
        int pos = 0;
        for (List<CheckBox> group : groupsWidgets) {
            switch (sel.charAt(pos)) {
            case '.':
                break;
            case '[':
                String currentCode = "";
                for (pos++; sel.charAt(pos) != ']'; pos++) {
                    currentCode += sel.charAt(pos);
                }
                for (CheckBox cb : group) {
                    if (currentCode.indexOf(cb.getFormValue().charAt(0)) >= 0) {
                        cb.setValue(true);
                    }
                }
                break;
            default:
                for (CheckBox cb : group) {
                    if (cb.getFormValue().equals("" + sel.charAt(pos))) {
                        cb.setValue(true);
                    }
                }
                break;
            }
            pos++;
        }
    }

    public String getSelected() {
        String result = "";
        for (List<CheckBox> group : groupsWidgets) {
            String currentCode = "";
            for (CheckBox cb : group) {
                if (Boolean.TRUE.equals(cb.getValue())) {
                    currentCode += cb.getFormValue();
                }
            }
            if (currentCode.length() == group.size() || currentCode.length() == 0) {
                // all checked or none checked
                currentCode = ".";
            }
            if (currentCode.length() == 1) {
                result += currentCode;
            } else {
                result += "[" + currentCode + "]";
            }
        }
        return result;
    }

    public static class TwoRowPanel extends CellPanel implements HasAlignment {
        private final int count;
        private Element tableRow1, tableRow2;
        private HorizontalAlignmentConstant horzAlign = ALIGN_DEFAULT;
        private VerticalAlignmentConstant vertAlign = ALIGN_TOP;

        public TwoRowPanel(int count) {
            this.count = count;
            tableRow1 = DOM.createTR();
            DOM.appendChild(getBody(), tableRow1);
            tableRow2 = DOM.createTR();
            DOM.appendChild(getBody(), tableRow2);

            DOM.setElementProperty(getTable(), "cellSpacing", "0");
            DOM.setElementProperty(getTable(), "cellPadding", "0");
        }

        @Override
        public void add(Widget w) {
            Element td = createAlignedTd();
            boolean firstRow = getChildren().size()+1 <= (count+1) / 2;
            DOM.appendChild(firstRow ? tableRow1 : tableRow2, td);
            add(w, td);
        }

        private Element createAlignedTd() {
            Element td = DOM.createTD();
            setCellHorizontalAlignment(td, ALIGN_DEFAULT);
            setCellVerticalAlignment(td, ALIGN_TOP);
            return td;
        }

        public HorizontalAlignmentConstant getHorizontalAlignment() {
            return horzAlign;
        }

        public VerticalAlignmentConstant getVerticalAlignment() {
            return vertAlign;
        }

        public void setHorizontalAlignment(HorizontalAlignmentConstant align) {
            horzAlign = align;
        }

        public void setVerticalAlignment(VerticalAlignmentConstant align) {
            vertAlign = align;
        }
    }
}
