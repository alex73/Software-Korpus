package org.alex73.korpus.editor;

import java.awt.Component;
import java.awt.GridBagConstraints;
import java.awt.Insets;
import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CancellationException;
import java.util.function.Consumer;
import java.util.regex.Pattern;

import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JOptionPane;
import javax.swing.JTextField;
import javax.swing.SwingWorker;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.AbstractTableModel;

import org.alex73.corpus.paradigm.Form;
import org.alex73.corpus.paradigm.FormType;
import org.alex73.corpus.paradigm.Paradigm;
import org.alex73.corpus.paradigm.Variant;
import org.alex73.korpus.belarusian.BelarusianTags;
import org.alex73.korpus.belarusian.BelarusianWordNormalizer;
import org.alex73.korpus.belarusian.TagLetter;
import org.alex73.korpus.editor.grammar.GrammarConstructor;
import org.alex73.korpus.editor.grammar.GrammarConstructor.PVW;
import org.alex73.korpus.editor.ui.GrammarPane2;
import org.alex73.korpus.utils.SetUtils;

public class GrammarPaneController2 {
    static final int MAX_TO = 10000;

    static GrammarPane2 ui;

    static String currentWord;
    static boolean notRealUpdate = false;

    public static synchronized void show(String word) {
        currentWord = word;

        notRealUpdate = true;
        try {
            ui.txtWhat.setText(word);
            ui.txtGrammar.setText("");
            ui.txtLike.setText("");
            ui.txtTo.setText("");
            ui.rbAddVariant.setSelected(true);
        } finally {
            notRealUpdate = false;
        }
        updateList();
        updateTo();
        updateGrammar();
    }

    public static void init() {
        ui.outScroll.getVerticalScrollBar().setUnitIncrement(8);
        ui.tableFound.setModel(new Model(Collections.emptyList()));
        ui.tableFound.getSelectionModel().addListSelectionListener(e -> constructForms());
        ui.txtLike.getDocument().addDocumentListener(new DocumentChanger(e -> updateList()));
        ui.txtWhat.getDocument().addDocumentListener(new DocumentChanger(e -> updateList()));
        ui.txtGrammar.getDocument().addDocumentListener(new DocumentChanger(e -> updateList()));
        ui.txtGrammar.getDocument().addDocumentListener(new DocumentChanger(e -> updateGrammar()));
        ui.cbPreserveCase.addActionListener(e -> updateList());
        ui.rbAddForm.addActionListener(e -> updateTo());
        ui.rbAddParadigm.addActionListener(e -> updateTo());
        ui.rbAddVariant.addActionListener(e -> updateTo());
        ui.txtTo.getDocument().addDocumentListener(new DocumentChanger(e -> updateTo()));
        ui.btnSave.addActionListener(e -> save());
    }

    static class DocumentChanger implements DocumentListener {
        private final Consumer<DocumentEvent> action;

        public DocumentChanger(Consumer<DocumentEvent> action) {
            this.action = action;
        }

        public void changedUpdate(DocumentEvent e) {
            action.accept(e);
        }

        public void insertUpdate(DocumentEvent e) {
            action.accept(e);
        }

        public void removeUpdate(DocumentEvent e) {
            action.accept(e);
        }
    }

    static synchronized void updateGrammar() {
        TagLetter ti = BelarusianTags.getInstance().getNextAfter(ui.txtGrammar.getText().trim());
        String tooltip = "<html>";
        for (TagLetter.OneLetterInfo o : ti.letters) {
            if (!tooltip.isEmpty()) {
                tooltip += "<br>\n";
            }
            tooltip += o.letter + ": " + o.description;
        }
        tooltip += "</html>";
        ui.txtGrammar.setToolTipText(tooltip);
    }

    static synchronized void updateTo() {
        if (notRealUpdate) {
            return;
        }
        if (ui.rbAddParadigm.isSelected()) {
            ui.txtTo.setVisible(false);
            ui.scrollTo.setVisible(false);
        } else {
            ui.txtTo.setVisible(true);
            ui.scrollTo.setVisible(true);
            if (updaterTo != null) {
                updaterTo.cancel(true);
            }
            updaterTo = new UpdaterTo();
            updaterTo.execute();
        }
    }

    static UpdaterTo updaterTo;

    static UpdaterList updaterList;

    static synchronized void updateList() {
        if (notRealUpdate) {
            return;
        }
        if (updaterList != null) {
            updaterList.cancel(true);
        }
        updaterList = new UpdaterList();
        updaterList.execute();
    }

    static void applyFont() {
    }

    static void constructForms() {
        ui.outPanel.removeAll();
        int r = ui.tableFound.getSelectedRow();
        if (r < 0) {
            return;
        }
        boolean someFormsOnly = ui.rbAddForm.isSelected();
        PVW basedOn = ((Model) ui.tableFound.getModel()).rows.get(r);
        Paradigm p = new GrammarConstructor(MainController.gr).constructParadigm(currentWord, basedOn.p, basedOn.v,
                basedOn.w);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.gridy = 0;
        gbc.anchor = GridBagConstraints.CENTER;
        gbc.insets = new Insets(5, 5, 5, 5);
        for (Form f : p.getVariant().get(0).getForm()) {
            gbc.gridx = 0;

            if (someFormsOnly) {
                JCheckBox cb = new JCheckBox();
                cb.setName("cb");
                ui.outPanel.add(cb, gbc);
                gbc.gridx++;
            }

            JTextField t = new JTextField(f.getTag());
            t.setEnabled(false);
            t.setName("Tag");
            ui.outPanel.add(t, gbc);

            gbc.gridx++;
            JTextField w = new JTextField(f.getValue());
            w.setName("Value");
            ui.outPanel.add(w, gbc);

            String[] ft = new String[FormType.values().length + 1];
            ft[0] = "";
            for (int i = 0; i < FormType.values().length; i++) {
                ft[i + 1] = FormType.values()[i].value();
            }
            gbc.gridx++;
            JComboBox<String> tp = new JComboBox<>(ft);
            tp.setName("Type");
            tp.setSelectedItem(f.getType() == null ? "" : f.getType().value());
            ui.outPanel.add(tp, gbc);

            gbc.gridy++;
        }
        ui.outPanel.revalidate();
        ui.outPanel.repaint();
    }

    static void save() {
        Variant newVariant = new Variant();
        Form f = new Form();
        for (Component c : ui.outPanel.getComponents()) {
            switch (c.getName()) {
            case "cb":
                if (!((JCheckBox) c).isSelected()) {
                    f = null;
                }
                break;
            case "Tag":
                if (f != null) {
                    f.setTag(((JTextField) c).getText().trim());
                }
                break;
            case "Value":
                if (f != null) {
                    f.setValue(((JTextField) c).getText().trim());
                }
                break;
            case "Type":
                if (f != null) {
                    String v = (String) ((JComboBox) c).getSelectedItem();
                    if (!v.isEmpty()) {
                        f.setType(FormType.fromValue(v));
                    }
                    newVariant.getForm().add(f);
                }
                f = new Form();
                break;
            default:
                JOptionPane.showMessageDialog(UI.mainWindow, "Невядомы тып поля: " + c.getName(), "Памылка",
                        JOptionPane.ERROR_MESSAGE);
                return;
            }
        }
        try {
            if (ui.rbAddForm.isSelected()) {
                if (ui.tableTo.getSelectedRow() < 0) {
                    JOptionPane.showMessageDialog(UI.mainWindow, "Неабраны варыянт у які дадаваць формы", "Памылка",
                            JOptionPane.ERROR_MESSAGE);
                    return;
                }
                if (newVariant.getForm().isEmpty()) {
                    JOptionPane.showMessageDialog(UI.mainWindow, "Неабрана аніводнай формы каб дадаць", "Памылка",
                            JOptionPane.ERROR_MESSAGE);
                    return;
                }
                PVW row = ((Model) ui.tableTo.getModel()).rows.get(ui.tableTo.getSelectedRow());
                MainController.gr.addForms(newVariant.getForm(), row.p.getPdgId(), row.v.getId());
            } else if (ui.rbAddVariant.isSelected()) {
                if (ui.tableTo.getSelectedRow() < 0) {
                    JOptionPane.showMessageDialog(UI.mainWindow, "Неабраная парадыгма ў якую дадаваць варыянт",
                            "Памылка", JOptionPane.ERROR_MESSAGE);
                    return;
                }
                PVW row = ((Model) ui.tableTo.getModel()).rows.get(ui.tableTo.getSelectedRow());
                newVariant.setLemma(newVariant.getForm().get(0).getValue());
                MainController.gr.addVariant(newVariant, row.p.getPdgId());
            } else {
                Paradigm p = new Paradigm();
                p.getVariant().add(newVariant);
                newVariant.setId("a");
                newVariant.setLemma(newVariant.getForm().get(0).getValue());
                p.setLemma(newVariant.getLemma());
                MainController.gr.addParadigm(p);
            }
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(UI.mainWindow, "Памылка запісу файла: " + ex.getMessage(), "Памылка",
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    static class UpdaterList extends SwingWorker<List<PVW>, Void> {
        String word, grammar, theme, looksLike;
        boolean preserveCase;
        GrammarConstructor grConstr;

        public UpdaterList() {
            grammar = ui.txtGrammar.getText().toUpperCase();
            looksLike = ui.txtLike.getText();
            word = currentWord;
            preserveCase = ui.cbPreserveCase.isSelected();
        }

        @Override
        protected List<PVW> doInBackground() throws Exception {
            grConstr = new GrammarConstructor(MainController.gr);
            return grConstr.getLooksLike(word, looksLike, preserveCase, grammar, null);
        }

        @Override
        protected void done() {
            try {
                List<PVW> found = get();
                ui.tableFound.setModel(new Model(found));
                if (!found.isEmpty()) {
                    ui.tableFound.setRowSelectionInterval(0, 0);
                }
            } catch (CancellationException ex) {
            } catch (Throwable ex) {
                JOptionPane.showMessageDialog(UI.mainWindow, "Памылка: " + ex.getMessage(), "Памылка",
                        JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    static class UpdaterTo extends SwingWorker<List<PVW>, Void> {
        String find;
        String selected;

        public UpdaterTo() {
            find = ui.txtTo.getText();
            int sel = ui.tableTo.getSelectedRow();
            if (sel >= 0) {
                PVW r = ((Model) ui.tableTo.getModel()).rows.get(sel);
                selected = r.p.getPdgId() + r.v.getId();
            }
        }

        @Override
        protected List<PVW> doInBackground() throws Exception {
            find = BelarusianWordNormalizer.lightNormalizedWithStars(find);
            if (find.isEmpty()) {
                return Collections.emptyList();
            }

            Pattern reFilter;
            if (!find.isEmpty() && find.contains("*")) {
                reFilter = Pattern.compile(find.replace("+", "\\+").replace("*", ".*"));
            } else {
                reFilter = null;
            }
            List<PVW> result = Collections.synchronizedList(new ArrayList<>());
            MainController.gr.getAllParadigms().parallelStream().forEach(p -> {
                if (result.size() > MAX_TO) {
                    return;
                }
                for (Variant v : p.getVariant()) {
                    if (reFilter != null) {
                        if (!reFilter.matcher(v.getLemma()).matches()) {
                            continue;
                        }
                    } else {
                        if (!find.equals(v.getLemma())) {
                            continue;
                        }
                    }
                    PVW d = new PVW();
                    d.p = p;
                    d.v = v;
                    result.add(d);
                }
            });
            Collections.sort(result, (a, b) -> Integer.compare(a.p.getPdgId(), b.p.getPdgId()));
            return result;
        }

        @Override
        protected void done() {
            try {
                List<PVW> found = get();
                ui.tableTo.setModel(new Model(found));
                if (!found.isEmpty()) {
                    if (selected != null) {
                        for (int i = 0; i < found.size(); i++) {
                            PVW r = found.get(i);
                            if (selected.equals(r.p.getPdgId() + r.v.getId())) {
                                ui.tableTo.setRowSelectionInterval(i, i);
                                ui.tableTo.scrollRectToVisible(new Rectangle(ui.tableTo.getCellRect(i, 0, true)));
                                break;
                            }
                        }
                    } else {
                        ui.tableTo.setRowSelectionInterval(0, 0);
                    }
                }
                constructForms();
            } catch (CancellationException ex) {
            } catch (Throwable ex) {
                JOptionPane.showMessageDialog(UI.mainWindow, "Памылка: " + ex.getMessage(), "Памылка",
                        JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    static class Model extends AbstractTableModel {
        final List<PVW> rows;

        public Model(List<PVW> rows) {
            this.rows = rows;
        }

        @Override
        public int getRowCount() {
            return rows.size();
        }

        @Override
        public int getColumnCount() {
            return 5;
        }

        @Override
        public Object getValueAt(int rowIndex, int columnIndex) {
            PVW r = rows.get(rowIndex);
            switch (columnIndex) {
            case 0:
                return r.p.getPdgId() + r.v.getId() + " / " + SetUtils.tag(r.p, r.v);
            case 1:
                return r.p.getLemma();
            case 2:
                return r.v.getLemma();
            case 3:
                return r.v.getPravapis();
            case 4:
                return r.p.getMeaning();
            }
            return null;
        }

        @Override
        public String getColumnName(int column) {
            switch (column) {
            case 0:
                return "ID / tag";
            case 1:
                return "Парадыгма";
            case 2:
                return "Варыянт";
            case 3:
                return "Правапіс";
            case 4:
                return "Значэнне";
            }
            return null;
        }
    }
}

/*
 * Шукаць як - трэба захаваць і перайсці на слова зноў Знікае іншая граматыка
 * калі здымаеш аманімію граматыкі калі дадаюцца прагалы - яны не збіраюцца
 * разам шнурочкам - не шукае
 * 
 * дадаванне формы - злева чэкбоксы, справа - combo nonstandard/potential
 */
