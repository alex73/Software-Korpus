package org.alex73.korpus.editor;

import java.awt.Component;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CancellationException;
import java.util.function.Supplier;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.SwingWorker;
import javax.xml.bind.Marshaller;

import org.alex73.grammardb.GrammarDB2;
import org.alex73.grammardb.SetUtils;
import org.alex73.grammardb.structures.Form;
import org.alex73.grammardb.structures.FormType;
import org.alex73.grammardb.structures.Paradigm;
import org.alex73.grammardb.structures.Variant;
import org.alex73.grammardb.tags.BelarusianTags;
import org.alex73.grammardb.tags.TagLetter;
import org.alex73.korpus.base.StaticGrammarFiller2;
import org.alex73.korpus.editor.GrammarPaneController2.DocumentChanger;
import org.alex73.korpus.editor.GrammarPaneController2.Model;
import org.alex73.korpus.editor.grammar.GrammarConstructor;
import org.alex73.korpus.editor.grammar.GrammarConstructor.PVW;
import org.alex73.korpus.editor.ui.GrammarPane3;
import org.alex73.korpus.editor.ui.ShowXMLDialog;

public class GrammarPaneController3Main {

    GrammarPane3 ui = new GrammarPane3();
    List<OneWord> controllers = new ArrayList<>();
    volatile boolean notRealUpdate = false;
    String constructedVariantTag;
    UpdaterList updaterList;

    public synchronized void setWords(List<String> selectedWords) {
        List<String> words = new ArrayList<>(selectedWords);
        for (OneWord c : new ArrayList<>(controllers)) {
            if (!words.contains(c.word)) {
                controllers.remove(c);
                ui.outputPanel.remove(c.outPanel);
            }
            words.remove(c.word);
        }
        if (controllers.isEmpty() && !words.isEmpty()) {
            show(words.get(0));
        }
        for (String w : words) {
            OneWord c = new OneWord(w);
            controllers.add(c);
            ui.outputPanel.add(c.outPanel);
            updateList();
            updateGrammar(() -> "", ui.txtGrammarParadigm);
        }
        ui.outputPanel.revalidate();
        ui.outputPanel.repaint();
    }

    synchronized void show(String word) {
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
            ui.txtGrammarParadigm.setText(grammar);
            ui.cbPreserveCaseParadigm.setSelected(preserveCase);
            ui.txtLikeParadigm.setText(end);
        } finally {
            notRealUpdate = false;
        }
    }

    public void init() {
//        outScroll.getVerticalScrollBar().setUnitIncrement(8);

        ui.tableFoundParadigm.setModel(new Model(Collections.emptyList()));
        ui.tableFoundParadigm.getSelectionModel().addListSelectionListener(e -> controllers.forEach(c -> c.constructForms()));
        ui.tableFoundParadigm.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                if (e.getClickCount() == 2 && ui.tableFoundParadigm.getSelectedRow() != -1) {
                    // showXML(tableFound);
                }
            }
        });

        ui.txtLikeParadigm.getDocument().addDocumentListener(new DocumentChanger(e -> updateList()));
        ui.txtGrammarParadigm.getDocument().addDocumentListener(new DocumentChanger(e -> updateList()));
        ui.txtGrammarParadigm.getDocument().addDocumentListener(new DocumentChanger(e -> updateGrammar(() -> "", ui.txtGrammarParadigm)));

        ui.txtOutTagParadigm.getDocument().addDocumentListener(new DocumentChanger(e -> updateGrammar(() -> "", ui.txtOutTagParadigm)));

        ui.cbPreserveCaseParadigm.addActionListener(e -> updateList());

        ui.btnSaveParadigm.addActionListener(e -> {
            controllers.forEach(c -> c.save());

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

            controllers.clear();
            ui.outputPanel.removeAll();
            ui.outputPanel.revalidate();
            ui.outputPanel.repaint();
            UI2.updateList();
        });
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

    class OneWord {
        String word;
        JPanel outPanel;

        public OneWord(String word) {
            this.word = word;
            outPanel = new JPanel(new GridBagLayout());
            outPanel.setBorder(BorderFactory.createTitledBorder(word));
        }

//        synchronized void updateTo() {
//            if (notRealUpdate) {
//                return;
//            }
//            if (updaterTo != null) {
//                updaterTo.cancel(true);
//            }
//            updaterTo = new UpdaterTo();
//            updaterTo.execute();
//        }

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
            int r = ui.tableFoundParadigm.getSelectedRow();
            if (r < 0) {
                return;
            }
            PVW basedOn = ((Model) ui.tableFoundParadigm.getModel()).rows.get(r);
            String word = this.word;
            if (word.startsWith("ў")) {
                word = "у" + word.substring(1);
            }
            if (ui.txtOutTagParadigm != null) {
                ui.txtOutTagParadigm.setText(SetUtils.tag(basedOn.p, basedOn.v));
            }
            if (ui.txtOutThemeParadigm != null) {
                if (StaticGrammarFiller2.fillTheme != null) {
                    ui.txtOutThemeParadigm.setText(StaticGrammarFiller2.fillTheme);
                }
            }
            Paradigm p = new GrammarConstructor(UI2.gr).constructParadigm(word, basedOn.p, basedOn.v, basedOn.w, ui.cbPreserveCaseParadigm.isSelected());
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
//                Supplier<String> beginTag = () -> {
//                    if (tableTo.getSelectedRow() >= 0) {
//                        PVW row = ((Model) tableTo.getModel()).rows.get(tableTo.getSelectedRow());
//                        return SetUtils.tag(row.p, row.v);
//                    } else {
//                        return "";
//                    }
//                };
//                updateGrammar(beginTag, t);
//                t.getDocument().addDocumentListener(new DocumentChanger(e -> updateGrammar(beginTag, t)));
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
                        addRow(i + 1, false, t.getText(), w.getText());
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
                Paradigm p = new Paradigm();
                p.getVariant().add(newVariant);
                newVariant.setId("a");
                newVariant.setLemma(newVariant.getForm().get(0).getValue());
                newVariant.setTag(constructedVariantTag);
                p.setLemma(newVariant.getLemma());
                p.setTag(ui.txtOutTagParadigm.getText().trim());
                p.setTheme(ui.txtOutThemeParadigm.getText().trim());
                UI2.gr.addParadigm(p);
            } catch (Exception ex) {
                ex.printStackTrace();
                JOptionPane.showMessageDialog(UI.mainWindow, "Памылка запісу файла: " + ex.getMessage(), "Памылка", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    class UpdaterList extends SwingWorker<List<PVW>, Void> {
        String word, grammar, looksLike;
        GrammarConstructor grConstr;

        public UpdaterList() {
            grammar = ui.txtGrammarParadigm.getText().trim().toUpperCase();
            looksLike = ui.txtLikeParadigm.getText().trim();
            word = controllers.get(0).word;
//            if (grammar.isEmpty() && tableTo != null && tableTo.getSelectedRow() >= 0) {
//                // табліца паказваецца - абіраць толькі гэтую часціну мовы
//                PVW row = ((Model) tableTo.getModel()).rows.get(tableTo.getSelectedRow());
//                grammar = row.p.getTag() != null ? row.p.getTag().substring(0, 1) : row.v.getTag().substring(0, 1);
//            }
        }

        @Override
        protected List<PVW> doInBackground() throws Exception {
            grConstr = new GrammarConstructor(UI2.gr);
            return grConstr.getLooksLike(word, looksLike, grammar, null);
        }

        @Override
        protected void done() {
            List<PVW> found;
            try {
                found = get();
                ui.tableFoundParadigm.setModel(new Model(found));
                if (!found.isEmpty()) {
                    ui.tableFoundParadigm.setRowSelectionInterval(0, 0);
                }
                controllers.forEach(c -> c.constructForms());
            } catch (CancellationException ex) {
            } catch (Throwable ex) {
                ex.printStackTrace();
                JOptionPane.showMessageDialog(UI.mainWindow, "Памылка: " + ex.getMessage(), "Памылка", JOptionPane.ERROR_MESSAGE);
                found = List.of();
            }
        }
    }
}
