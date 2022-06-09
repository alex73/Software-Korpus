package org.alex73.korpus.editor;

import java.awt.Component;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Rectangle;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CancellationException;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.regex.Pattern;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.SwingWorker;
import javax.swing.Timer;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.AbstractTableModel;
import javax.xml.bind.Marshaller;

import org.alex73.corpus.paradigm.Form;
import org.alex73.corpus.paradigm.FormType;
import org.alex73.corpus.paradigm.Paradigm;
import org.alex73.corpus.paradigm.Variant;
import org.alex73.korpus.base.GrammarDB2;
import org.alex73.korpus.base.StaticGrammarFiller2;
import org.alex73.korpus.belarusian.BelarusianTags;
import org.alex73.korpus.belarusian.BelarusianWordNormalizer;
import org.alex73.korpus.belarusian.TagLetter;
import org.alex73.korpus.editor.grammar.GrammarConstructor;
import org.alex73.korpus.editor.grammar.GrammarConstructor.PVW;
import org.alex73.korpus.editor.ui.GrammarPane2;
import org.alex73.korpus.editor.ui.ShowXMLDialog;
import org.alex73.korpus.utils.SetUtils;

/**
 * Кіруе разгортваннем слоў.
 * 
 * Сістэмныя налады:
 * DEFAULT_GRAMMAR - стандартная граматыка
 * DEFAULT_END_LENGTH - памер канчатку каб паказаць такія самыя словы
 */
public class GrammarPaneController2 {
    enum MODE {
        PARADIGM, VARIANT, FORMS
    };

    static final int MAX_TO = 10000;

    static GrammarPane2 ui;

    static String currentWord;
    static volatile boolean notRealUpdate = false;
    static String constructedVariantTag;

    static Context contextParadigm, contextVariant, contextForm;

    public static synchronized void show(String word) {
        String grammar = System.getProperty("DEFAULT_GRAMMAR", "");
        boolean preserveCase = Boolean.parseBoolean(System.getProperty("DEFAULT_PRESERVE_CASE", "false"));
        String endLengthStr = System.getProperty("DEFAULT_END_LENGTH");
        String end;
        try {
            int endLength = Integer.parseInt(endLengthStr);
            end = '*' + word.substring(word.length() - endLength);
        } catch (Exception ex) {
            end = "";
        }

        notRealUpdate = true;
        try {
            ui.txtWhatParadigm.setText(word);
            ui.txtWhatVariant.setText(word);
            ui.txtGrammarParadigm.setText(grammar);
            ui.txtGrammarVariant.setText(grammar);
            ui.cbPreserveCaseParadigm.setSelected(preserveCase);
            ui.cbPreserveCaseVariant.setSelected(preserveCase);
            ui.txtLikeParadigm.setText(end);
            ui.txtLikeVariant.setText(end);
            ui.txtToVariant.setText(word);
            ui.txtToForm.setText(word);
        } finally {
            notRealUpdate = false;
        }

        contextParadigm.resetForms();
        contextVariant.resetForms();
        contextForm.resetForms();

        contextVariant.updateTo();
        contextForm.updateTo();

        contextParadigm.updateList();
        contextVariant.updateList();

        contextParadigm.updateGrammar(() -> "", contextParadigm.txtGrammar);
        contextVariant.updateGrammar(() -> "", contextVariant.txtGrammar);
    }

    public static void init() {
        contextParadigm = new Context(MODE.PARADIGM);
        contextParadigm.txtWhat = ui.txtWhatParadigm;
        contextParadigm.txtGrammar = ui.txtGrammarParadigm;
        contextParadigm.txtLike = ui.txtLikeParadigm;
        contextParadigm.txtOutTag = ui.txtOutTagParadigm;
        contextParadigm.txtOutTheme = ui.txtOutThemeParadigm;
        contextParadigm.tableFound = ui.tableFoundParadigm;
        contextParadigm.listScroll = ui.listScrollParadigm;
        contextParadigm.outScroll = ui.outScrollParadigm;
        contextParadigm.cbPreserveCase = ui.cbPreserveCaseParadigm;
        contextParadigm.outPanel = ui.outPanelParadigm;
        contextParadigm.btnSave = ui.btnSaveParadigm;
        contextParadigm.init();

        contextVariant = new Context(MODE.VARIANT);
        contextVariant.txtTo = ui.txtToVariant;
        contextVariant.txtWhat = ui.txtWhatVariant;
        contextVariant.txtGrammar = ui.txtGrammarVariant;
        contextVariant.txtLike = ui.txtLikeVariant;
        contextVariant.tableTo = ui.tableToVariant;
        contextVariant.tableFound = ui.tableFoundVariant;
        contextVariant.listScroll = ui.listScrollVariant;
        contextVariant.outScroll = ui.outScrollVariant;
        contextVariant.cbPreserveCase = ui.cbPreserveCaseVariant;
        contextVariant.btnSave = ui.btnSaveVariant;
        contextVariant.outPanel = ui.outPanelVariant;
        contextVariant.init();

        contextForm = new Context(MODE.FORMS);
        contextForm.txtTo = ui.txtToForm;
        contextForm.tableTo = ui.tableToForm;
        contextForm.outScroll = ui.outScrollForm;
        contextForm.btnSave = ui.btnSaveForm;
        contextForm.outPanel = ui.outPanelForm;
        contextForm.btnAddForm = ui.btnAddFormForm;
        contextForm.init();
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

    static void applyFont() {
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

    static class Context {
        MODE mode;
        JTextField txtTo;
        JTextField txtWhat;
        JTextField txtGrammar;
        JTextField txtLike;
        JTextField txtOutTag, txtOutTheme;
        JTable tableTo;
        JTable tableFound;
        JScrollPane listScroll;
        JScrollPane outScroll;
        JCheckBox cbPreserveCase;
        JPanel outPanel;
        UpdaterTo updaterTo;
        UpdaterList updaterList;
        JButton btnAddForm;
        JButton btnSave;

        public Context(MODE mode) {
            this.mode = mode;
        }

        void init() {
            outScroll.getVerticalScrollBar().setUnitIncrement(8);

            if (tableTo != null) {
                tableTo.setModel(new Model(Collections.emptyList()));
                tableTo.addMouseListener(new MouseAdapter() {
                    @Override
                    public void mousePressed(MouseEvent e) {
                        if (e.getClickCount() == 2 && tableTo.getSelectedRow() != -1) {
                            showXML(tableTo);
                        }
                    }
                });
            }
            if (tableFound != null) {
                tableFound.setModel(new Model(Collections.emptyList()));
                tableFound.getSelectionModel().addListSelectionListener(e -> constructForms());
                tableFound.addMouseListener(new MouseAdapter() {
                    @Override
                    public void mousePressed(MouseEvent e) {
                        if (e.getClickCount() == 2 && tableFound.getSelectedRow() != -1) {
                            showXML(tableFound);
                        }
                    }
                });
            }

            if (txtWhat != null || txtLike != null || txtGrammar != null) {
                txtWhat.getDocument().addDocumentListener(new DocumentChanger(e -> updateList()));
                txtLike.getDocument().addDocumentListener(new DocumentChanger(e -> updateList()));
                txtGrammar.getDocument().addDocumentListener(new DocumentChanger(e -> updateList()));
                txtGrammar.getDocument().addDocumentListener(new DocumentChanger(e -> updateGrammar(() -> "", txtGrammar)));
            }

            if (txtOutTag != null) {
                txtOutTag.getDocument().addDocumentListener(new DocumentChanger(e -> updateGrammar(() -> "", txtOutTag)));
            }


            if (cbPreserveCase != null) {
                cbPreserveCase.addActionListener(e -> updateList());
            }

            if (txtTo != null) {
                txtTo.getDocument().addDocumentListener(new DocumentChanger(e -> updateTo()));
            }

            if (btnAddForm != null) {
                btnAddForm.addActionListener(e -> addForm());
            }

            btnSave.addActionListener(e -> save());
        }

        synchronized void updateGrammar(Supplier<String> getGrammerPrefix, JTextField field) {
            try {
                String tag = getGrammerPrefix.get() + field.getText().trim();
                String tooltip = "<html>";

                for (String s : BelarusianTags.getInstance().describe(tag, Set.of())) {
                    tooltip += s + "<br>\n";
                }

                TagLetter ti = BelarusianTags.getInstance().getNextAfter(tag);
                for (TagLetter.OneLetterInfo o : ti.letters) {
                    if (!tooltip.isEmpty()) {
                        tooltip += "<br>\n";
                    }
                    tooltip += o.letter + ": " + o.description;
                }
                tooltip += "</html>";
                field.setToolTipText(tooltip);
            } catch (Exception ex) {
                field.setToolTipText("Памылка вызначэння тэга: " + ex.getMessage());
            }
        }

        synchronized void updateTo() {
            if (notRealUpdate) {
                return;
            }
            if (updaterTo != null) {
                updaterTo.cancel(true);
            }
            updaterTo = new UpdaterTo();
            updaterTo.execute();
        }

        synchronized void updateList() {
            if (notRealUpdate) {
                return;
            }
            if (updaterList != null) {
                updaterList.cancel(true);
            }
            updaterList = new UpdaterList();
            updaterList.execute();
        }

        class UpdaterList extends SwingWorker<List<PVW>, Void> {
            String word, grammar, looksLike;
            GrammarConstructor grConstr;

            public UpdaterList() {
                grammar = txtGrammar.getText().trim().toUpperCase();
                looksLike = txtLike.getText().trim();
                word = txtWhat.getText().trim();
                if (grammar.isEmpty() && tableTo != null && tableTo.getSelectedRow() >= 0) {
                    // табліца паказваецца - абіраць толькі гэтую часціну мовы
                    PVW row = ((Model) tableTo.getModel()).rows.get(tableTo.getSelectedRow());
                    grammar = row.p.getTag() != null ? row.p.getTag().substring(0, 1) : row.v.getTag().substring(0, 1);
                }
            }

            @Override
            protected List<PVW> doInBackground() throws Exception {
                grConstr = new GrammarConstructor(MainController.gr);
                return grConstr.getLooksLike(word, looksLike, grammar, null);
            }

            @Override
            protected void done() {
                List<PVW> found;
                try {
                    found = get();
                    tableFound.setModel(new Model(found));
                    if (!found.isEmpty()) {
                        tableFound.setRowSelectionInterval(0, 0);
                    }
                    constructForms();
                } catch (CancellationException ex) {
                } catch (Throwable ex) {
                    ex.printStackTrace();
                    JOptionPane.showMessageDialog(UI.mainWindow, "Памылка: " + ex.getMessage(), "Памылка", JOptionPane.ERROR_MESSAGE);
                    found = List.of();
                }
            }
        }

        class UpdaterTo extends SwingWorker<List<PVW>, Void> {

            String find;
            String selected;

            public UpdaterTo() {
                find = txtTo.getText();
                int sel = tableTo.getSelectedRow();
                if (sel >= 0) {
                    PVW r = ((Model) tableTo.getModel()).rows.get(sel);
                    selected = r.p.getPdgId() + r.v.getId();
                }
            }

            @Override
            protected List<PVW> doInBackground() throws Exception {
                find = BelarusianWordNormalizer.superNormalized(find);
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
                        String lemma = BelarusianWordNormalizer.superNormalized(v.getLemma());
                        if (reFilter != null) {
                            if (!reFilter.matcher(lemma).matches()) {
                                continue;
                            }
                        } else {
                            if (!find.equals(lemma)) {
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
                    tableTo.setModel(new Model(found));
                    if (!found.isEmpty()) {
                        if (selected != null) {
                            for (int i = 0; i < found.size(); i++) {
                                PVW r = found.get(i);
                                if (selected.equals(r.p.getPdgId() + r.v.getId())) {
                                    tableTo.setRowSelectionInterval(i, i);
                                    tableTo.scrollRectToVisible(new Rectangle(tableTo.getCellRect(i, 0, true)));
                                    break;
                                }
                            }
                        } else {
                            tableTo.setRowSelectionInterval(0, 0);
                        }
                    }
                    ui.validate();
                } catch (CancellationException ex) {
                } catch (Throwable ex) {
                    ex.printStackTrace();
                    JOptionPane.showMessageDialog(UI.mainWindow, "Памылка: " + ex.getMessage(), "Памылка", JOptionPane.ERROR_MESSAGE);
                }
            }
        }

        void showXML(JTable table) {
            PVW row = ((Model) table.getModel()).rows.get(table.getSelectedRow());

            StringWriter s = new StringWriter();
            try {
                Marshaller m = GrammarDB2.getContext().createMarshaller();
                m.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
                m.marshal(row.p, s);
            } catch (Exception ex) {
                ex.printStackTrace(new PrintWriter(s));
            }

            ShowXMLDialog dialog = new ShowXMLDialog(UI.mainWindow, true);
            dialog.text.setText(s.toString());
            dialog.text.setCaretPosition(0);
            dialog.setSize(1200, 500);
            UI.setOnCenter(UI.mainWindow, dialog);
            dialog.setVisible(true);
        }

        synchronized void resetForms() {
            outPanel.removeAll();
            outPanel.revalidate();
            outPanel.repaint();
        }

        synchronized void constructForms() {
            resetForms();
            int r = tableFound.getSelectedRow();
            if (r < 0) {
                return;
            }
            PVW basedOn = ((Model) tableFound.getModel()).rows.get(r);
            String word = txtWhat.getText().trim();
            if (word.startsWith("ў")) {
                word = "у" + word.substring(1);
            }
            if (txtOutTag != null) {
                txtOutTag.setText(SetUtils.tag(basedOn.p, basedOn.v));
            }
            if (txtOutTheme != null) {
                if (StaticGrammarFiller2.fillTheme != null) {
                    txtOutTheme.setText(StaticGrammarFiller2.fillTheme);
                }
            }
            Paradigm p = new GrammarConstructor(MainController.gr).constructParadigm(word, basedOn.p, basedOn.v, basedOn.w, cbPreserveCase.isSelected());
            constructedVariantTag = basedOn.v.getTag();
            for (Form f : p.getVariant().get(0).getForm()) {
                addRow(outPanel.getComponentCount(), true, f.getTag(), f.getValue());
            }
            outPanel.revalidate();
            outPanel.repaint();
        }

        private void addRow(int toIndex, boolean readOnlyTag, String tag, String form) {
            GridBagConstraints gbc = new GridBagConstraints();
            gbc.fill = GridBagConstraints.HORIZONTAL;
            gbc.gridx = 0;
            gbc.gridy = GridBagConstraints.RELATIVE;
            gbc.anchor = GridBagConstraints.CENTER;
            gbc.insets = new Insets(5, 5, 5, 5);

            JButton bAdd = new JButton("+");
            bAdd.setName("Add");
            outPanel.add(bAdd, gbc, toIndex++);

            gbc.gridx++;
            JButton bDel = new JButton("-");
            bDel.setName("Remove");
            outPanel.add(bDel, gbc, toIndex++);

            gbc.gridx++;
            JTextField t = new JTextField(tag);
            if (readOnlyTag) {
                t.setEnabled(false);
            } else {
                t.setColumns(4);
                Supplier<String> beginTag = () -> {
                    if (tableTo.getSelectedRow() >= 0) {
                        PVW row = ((Model) tableTo.getModel()).rows.get(tableTo.getSelectedRow());
                        return SetUtils.tag(row.p, row.v);
                    } else {
                        return "";
                    }
                };
                updateGrammar(beginTag, t);
                t.getDocument().addDocumentListener(new DocumentChanger(e -> updateGrammar(beginTag, t)));
            }
            t.setName("Tag");
            outPanel.add(t, gbc, toIndex++);

            gbc.gridx++;
            JTextField w = new JTextField(form);
            w.setColumns(10);
            w.setName("Value");
            outPanel.add(w, gbc, toIndex++);

            String[] ft = new String[FormType.values().length + 1];
            ft[0] = "";
            for (int i = 0; i < FormType.values().length; i++) {
                ft[i + 1] = FormType.values()[i].value();
            }
            gbc.gridx++;
            JComboBox<String> tp = new JComboBox<>(ft);
            tp.setName("Type");
            outPanel.add(tp, gbc, toIndex++);

            bDel.addActionListener(e -> {
                outPanel.remove(bAdd);
                outPanel.remove(bDel);
                outPanel.remove(t);
                outPanel.remove(w);
                outPanel.remove(tp);
                outPanel.revalidate();
                outPanel.repaint();
            });
            bAdd.addActionListener(e -> {
                for (int i = 0; i < outPanel.getComponentCount(); i++) {
                    if (outPanel.getComponent(i) == tp) {
                        addRow(i+1, false, t.getText(), w.getText());
                        break;
                    }
                }
            });

            outPanel.revalidate();
            outPanel.repaint();
        }

        synchronized void addForm() {
            addRow(outPanel.getComponentCount(), false, "", "");
        }

        synchronized void save() {
            Variant newVariant = new Variant();
            Form f = new Form();
            for (Component c : outPanel.getComponents()) {
                switch (c.getName()) {
                case "Add":
                case "Remove":
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
                    JOptionPane.showMessageDialog(UI.mainWindow, "Невядомы тып поля: " + c.getName(), "Памылка", JOptionPane.ERROR_MESSAGE);
                    return;
                }
            }
            if (newVariant.getForm().isEmpty()) {
                JOptionPane.showMessageDialog(UI.mainWindow, "Няма што захоўваць", "Памылка", JOptionPane.ERROR_MESSAGE);
                return;
            }
            try {
                switch (mode) {
                case FORMS: {
                    if (tableTo.getSelectedRow() < 0) {
                        JOptionPane.showMessageDialog(UI.mainWindow, "Неабраны варыянт у які дадаваць формы", "Памылка", JOptionPane.ERROR_MESSAGE);
                        return;
                    }
                    if (newVariant.getForm().isEmpty()) {
                        JOptionPane.showMessageDialog(UI.mainWindow, "Неабрана аніводнай формы каб дадаць", "Памылка", JOptionPane.ERROR_MESSAGE);
                        return;
                    }
                    PVW row = ((Model) tableTo.getModel()).rows.get(tableTo.getSelectedRow());
                    if (row.p.getPdgId() == 0) {
                        JOptionPane.showMessageDialog(UI.mainWindow, "Нельга будаваць на падставе парадыгмы не з базы", "Памылка", JOptionPane.ERROR_MESSAGE);
                        return;
                    }
                    MainController.gr.addForms(newVariant.getForm(), row.p.getPdgId(), row.v.getId());
                }
                    break;
                case VARIANT: {
                    if (tableTo.getSelectedRow() < 0) {
                        JOptionPane.showMessageDialog(UI.mainWindow, "Неабраная парадыгма ў якую дадаваць варыянт", "Памылка", JOptionPane.ERROR_MESSAGE);
                        return;
                    }
                    PVW row = ((Model) tableTo.getModel()).rows.get(tableTo.getSelectedRow());
                    if (row.p.getPdgId() == 0) {
                        JOptionPane.showMessageDialog(UI.mainWindow, "Нельга будаваць на падставе парадыгмы не з базы", "Памылка", JOptionPane.ERROR_MESSAGE);
                        return;
                    }
                    newVariant.setLemma(newVariant.getForm().get(0).getValue());
                    newVariant.setTag(constructedVariantTag);
                    MainController.gr.addVariant(newVariant, row.p.getPdgId());
                }
                    break;
                case PARADIGM: {
                    Paradigm p = new Paradigm();
                    p.getVariant().add(newVariant);
                    newVariant.setId("a");
                    newVariant.setLemma(newVariant.getForm().get(0).getValue());
                    newVariant.setTag(constructedVariantTag);
                    p.setLemma(newVariant.getLemma());
                    p.setTag(txtOutTag.getText().trim());
                    p.setTheme(txtOutTheme.getText().trim());
                    MainController.gr.addParadigm(p);
                }
                    break;
                }
                MainController.updateFullGrammar();

                JOptionPane messagePane = new JOptionPane("Змены захаваныя", JOptionPane.INFORMATION_MESSAGE);
                final JDialog dialog = messagePane.createDialog(UI.mainWindow, "Паведамленне");
                new SwingWorker<Void, Void>() {
                    @Override
                    protected Void doInBackground() throws Exception {
                        Thread.sleep(500);
                        return null;
                    }

                    protected void done() {
                        dialog.dispose();
                    };
                }.execute();
                dialog.setVisible(true);
            } catch (Exception ex) {
                ex.printStackTrace();
                JOptionPane.showMessageDialog(UI.mainWindow, "Памылка запісу файла: " + ex.getMessage(), "Памылка", JOptionPane.ERROR_MESSAGE);
            }
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
