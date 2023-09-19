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

package org.alex73.korpus.editor;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import javax.swing.JCheckBox;
import javax.swing.JLabel;

import org.alex73.grammardb.themes.Theme;
import org.alex73.korpus.editor.ui.ThemesDialog;

/**
 * Вакно выбару тэмы для слова ў граматычнай базе.
 */
public class ThemesDialogController {
    ThemesDialog dialog;
    int column, row;
    Font f;
    String result;
    Map<String, JCheckBox> checkboxes = new HashMap<>();

    private static ThemesDialogController instance = new ThemesDialogController();

    public static ThemesDialogController getInstance() {
        return instance;
    }

    public ThemesDialogController() {
        dialog = new ThemesDialog(UI.mainWindow, true);
        f = dialog.getFont();
        f = new Font(f.getName(), f.getStyle(), f.getSize() - 1);
        UI.setOnCenter(UI.mainWindow, dialog);
    }

    public String show(String grammar, String themes) {
        if (grammar.length() < 1) {
            return null;
        }
        init(grammar.substring(0, 1));
        for (String t : themes.split(";")) {
            JCheckBox cb = checkboxes.get(t);
            if (cb != null) {
                cb.setSelected(true);
            }
        }

        dialog.setVisible(true);
        return result;
    }

    void init(String grammar) {
        dialog.panel.removeAll();
        row = 0;
        column = 0;
        checkboxes.clear();
        Theme thRoot = MainController.gr.getThemes(grammar);
        switch (grammar) {
        case "N":
            themeExact(thRoot, "Прадметныя");
            nextColumn();
            themeExact(thRoot, "Непрадметныя імёны");
            nextColumn();
            themeNot(thRoot, "Прадметныя", "Непрадметныя імёны");
            break;
        case "A":
            themeExact(thRoot, "Таксанамія");
            nextColumn();
            themeNot(thRoot, "Таксанамія");
            break;
        case "V":
            themeNot(thRoot);
            break;
        case "R":
            themeNot(thRoot);
            break;
        }
        result = null;
        dialog.btnSave.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                result = "";
                for (String t : dialog.txtResult.getText().split("\n")) {
                    t = t.trim();
                    if (!t.isEmpty()) {
                        if (!result.isEmpty()) {
                            result += ';';
                        }
                        result += t;
                    }
                }
                dialog.dispose();
            }
        });
    }

    void nextColumn() {
        column++;
        row = 0;
    }

    void themeExact(Theme thRoot, String name) {
        for (Theme th : thRoot.children) {
            if (name.equals(th.name)) {
                fill(th, 0, th.name);
            }
        }
    }

    void themeNot(Theme thRoot, String... name) {
        Set<String> not = new TreeSet<>(Arrays.asList(name));
        for (Theme th : thRoot.children) {
            if (!not.contains(th.name)) {
                fill(th, 0, th.name);
            }
        }
    }

    void fill(Theme th, int level, String path) {
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = column;
        gbc.gridy = row++;
        gbc.insets = new Insets(0, level * 20, 0, 0);
        gbc.anchor = GridBagConstraints.WEST;
        if (th.children.isEmpty()) {
            JCheckBox c = new JCheckBox(th.name);
            c.setFont(f);
            Dimension preferredSize = c.getPreferredSize();
            preferredSize.height -= 3;
            c.setPreferredSize(preferredSize);
            c.setToolTipText(path);
            checkboxes.put(path, c);
            c.addActionListener(checkBoxListener);
            dialog.panel.add(c, gbc);
        } else {
            JLabel c = new JLabel(th.name);
            c.setFont(f);
            dialog.panel.add(c, gbc);
        }
        for (Theme child : th.children) {
            fill(child, level + 1, path + "/" + child.name);
        }
    }

    ActionListener checkBoxListener = new ActionListener() {
        public void actionPerformed(ActionEvent e) {
            String list = "";
            int rows = 0;
            for (int i = 0; i < dialog.panel.getComponentCount(); i++) {
                Component c = dialog.panel.getComponent(i);
                if (c instanceof JCheckBox) {
                    JCheckBox cb = (JCheckBox) c;
                    if (cb.isSelected()) {
                        if (!list.isEmpty()) {
                            list += '\n';
                        }
                        rows++;
                        list += cb.getToolTipText();
                    }
                }
            }
            dialog.txtResult.setText(list);
            dialog.txtResult.setCaretPosition(0);
            dialog.txtResult.setRows(rows);
        }
    };
}
