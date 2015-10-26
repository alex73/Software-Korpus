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

package org.alex73.korpus.editor.ui;

public class MainWindow extends javax.swing.JFrame {

    /**
     * Creates new form MainWindow
     */
    public MainWindow() {
        initComponents();
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        buttonGroup1 = new javax.swing.ButtonGroup();
        buttonGroup2 = new javax.swing.ButtonGroup();
        buttonGroup3 = new javax.swing.ButtonGroup();
        jMenuBar1 = new javax.swing.JMenuBar();
        jMenu1 = new javax.swing.JMenu();
        mFileOpen = new javax.swing.JMenuItem();
        mFileSave = new javax.swing.JMenuItem();
        mFileClose = new javax.swing.JMenuItem();
        menuEdit = new javax.swing.JMenu();
        mUndo = new javax.swing.JMenuItem();
        mRedo = new javax.swing.JMenuItem();
        jSeparator3 = new javax.swing.JPopupMenu.Separator();
        mSetText = new javax.swing.JRadioButtonMenuItem();
        mSetOtherLanguage = new javax.swing.JRadioButtonMenuItem();
        mSetDigits = new javax.swing.JRadioButtonMenuItem();
        jMenu2 = new javax.swing.JMenu();
        mUnk1 = new javax.swing.JRadioButtonMenuItem();
        mUnk2 = new javax.swing.JRadioButtonMenuItem();
        mUnk3 = new javax.swing.JRadioButtonMenuItem();
        jSeparator2 = new javax.swing.JPopupMenu.Separator();
        jMenu5 = new javax.swing.JMenu();
        f10 = new javax.swing.JRadioButtonMenuItem();
        f12 = new javax.swing.JRadioButtonMenuItem();
        f16 = new javax.swing.JRadioButtonMenuItem();
        f20 = new javax.swing.JRadioButtonMenuItem();
        f24 = new javax.swing.JRadioButtonMenuItem();
        f30 = new javax.swing.JRadioButtonMenuItem();
        f36 = new javax.swing.JRadioButtonMenuItem();
        f44 = new javax.swing.JRadioButtonMenuItem();
        jMenu3 = new javax.swing.JMenu();
        mGoNextMark = new javax.swing.JMenuItem();
        jSeparator1 = new javax.swing.JPopupMenu.Separator();
        mGoEditor = new javax.swing.JMenuItem();
        mGoWordInfo = new javax.swing.JMenuItem();
        mGoGrammar = new javax.swing.JMenuItem();

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);

        jMenu1.setText("Тэксты");

        mFileOpen.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_O, java.awt.event.InputEvent.CTRL_MASK));
        mFileOpen.setText("Адчыніць...");
        mFileOpen.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                mFileOpenActionPerformed(evt);
            }
        });
        jMenu1.add(mFileOpen);

        mFileSave.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_S, java.awt.event.InputEvent.CTRL_MASK));
        mFileSave.setText("Захаваць");
        mFileSave.setEnabled(false);
        jMenu1.add(mFileSave);

        mFileClose.setText("Зачыніць");
        mFileClose.setEnabled(false);
        jMenu1.add(mFileClose);

        jMenuBar1.add(jMenu1);

        menuEdit.setText("Рэдагаваньне");

        mUndo.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_Z, java.awt.event.InputEvent.CTRL_MASK));
        mUndo.setText("Адрабіць");
        menuEdit.add(mUndo);

        mRedo.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_Y, java.awt.event.InputEvent.CTRL_MASK));
        mRedo.setText("Узнавіць");
        menuEdit.add(mRedo);
        menuEdit.add(jSeparator3);

        buttonGroup3.add(mSetText);
        mSetText.setSelected(true);
        mSetText.setText("Звычайны тэкст");
        menuEdit.add(mSetText);

        buttonGroup3.add(mSetOtherLanguage);
        mSetOtherLanguage.setText("Іншамоўнае");
        menuEdit.add(mSetOtherLanguage);

        buttonGroup3.add(mSetDigits);
        mSetDigits.setText("Лічбы");
        menuEdit.add(mSetDigits);

        jMenuBar1.add(menuEdit);

        jMenu2.setText("Паказваць");

        buttonGroup1.add(mUnk1);
        mUnk1.setSelected(true);
        mUnk1.setText("Невядомыя лемы");
        jMenu2.add(mUnk1);

        buttonGroup1.add(mUnk2);
        mUnk2.setText("Аманімія лемаў");
        jMenu2.add(mUnk2);

        buttonGroup1.add(mUnk3);
        mUnk3.setText("Аманімія граматыкі");
        jMenu2.add(mUnk3);
        jMenu2.add(jSeparator2);

        jMenu5.setText("Шрыфт");

        buttonGroup2.add(f10);
        f10.setSelected(true);
        f10.setText("10");
        jMenu5.add(f10);

        buttonGroup2.add(f12);
        f12.setText("12");
        jMenu5.add(f12);

        buttonGroup2.add(f16);
        f16.setText("16");
        jMenu5.add(f16);

        buttonGroup2.add(f20);
        f20.setText("20");
        jMenu5.add(f20);

        buttonGroup2.add(f24);
        f24.setText("24");
        jMenu5.add(f24);

        buttonGroup2.add(f30);
        f30.setText("30");
        jMenu5.add(f30);

        buttonGroup2.add(f36);
        f36.setText("36");
        jMenu5.add(f36);

        buttonGroup2.add(f44);
        f44.setText("44");
        jMenu5.add(f44);

        jMenu2.add(jMenu5);

        jMenuBar1.add(jMenu2);

        jMenu3.setText("Перайсьці");

        mGoNextMark.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_F3, 0));
        mGoNextMark.setText("Да наступнай пазнакі");
        jMenu3.add(mGoNextMark);
        jMenu3.add(jSeparator1);

        mGoEditor.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_F4, 0));
        mGoEditor.setText("Да вакна рэдактара");
        jMenu3.add(mGoEditor);

        mGoWordInfo.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_F5, 0));
        mGoWordInfo.setText("Да вакна \"Зьвесткі пра слова\"");
        jMenu3.add(mGoWordInfo);

        mGoGrammar.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_F6, 0));
        mGoGrammar.setText("Да вакна \"Граматычная база\"");
        mGoGrammar.setActionCommand("GoGrammar");
        jMenu3.add(mGoGrammar);

        jMenuBar1.add(jMenu3);

        setJMenuBar(jMenuBar1);

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void mFileOpenActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_mFileOpenActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_mFileOpenActionPerformed

    /**
     * @param args the command line arguments
     */
    public static void main(String args[]) {
        /*
         * Set the Nimbus look and feel
         */
        //<editor-fold defaultstate="collapsed" desc=" Look and feel setting code (optional) ">
        /*
         * If Nimbus (introduced in Java SE 6) is not available, stay with the
         * default look and feel. For details see
         * http://download.oracle.com/javase/tutorial/uiswing/lookandfeel/plaf.html
         */
        try {
            for (javax.swing.UIManager.LookAndFeelInfo info : javax.swing.UIManager.getInstalledLookAndFeels()) {
                if ("Nimbus".equals(info.getName())) {
                    javax.swing.UIManager.setLookAndFeel(info.getClassName());
                    break;
                }
            }
        } catch (ClassNotFoundException ex) {
            java.util.logging.Logger.getLogger(MainWindow.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (InstantiationException ex) {
            java.util.logging.Logger.getLogger(MainWindow.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (IllegalAccessException ex) {
            java.util.logging.Logger.getLogger(MainWindow.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (javax.swing.UnsupportedLookAndFeelException ex) {
            java.util.logging.Logger.getLogger(MainWindow.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        }
        //</editor-fold>

        /*
         * Create and display the form
         */
        java.awt.EventQueue.invokeLater(new Runnable() {

            public void run() {
                new MainWindow().setVisible(true);
            }
        });
    }
    // Variables declaration - do not modify//GEN-BEGIN:variables
    public javax.swing.ButtonGroup buttonGroup1;
    public javax.swing.ButtonGroup buttonGroup2;
    public javax.swing.ButtonGroup buttonGroup3;
    public javax.swing.JRadioButtonMenuItem f10;
    public javax.swing.JRadioButtonMenuItem f12;
    public javax.swing.JRadioButtonMenuItem f16;
    public javax.swing.JRadioButtonMenuItem f20;
    public javax.swing.JRadioButtonMenuItem f24;
    public javax.swing.JRadioButtonMenuItem f30;
    public javax.swing.JRadioButtonMenuItem f36;
    public javax.swing.JRadioButtonMenuItem f44;
    public javax.swing.JMenu jMenu1;
    public javax.swing.JMenu jMenu2;
    public javax.swing.JMenu jMenu3;
    public javax.swing.JMenu jMenu5;
    public javax.swing.JMenuBar jMenuBar1;
    public javax.swing.JPopupMenu.Separator jSeparator1;
    public javax.swing.JPopupMenu.Separator jSeparator2;
    public javax.swing.JPopupMenu.Separator jSeparator3;
    public javax.swing.JMenuItem mFileClose;
    public javax.swing.JMenuItem mFileOpen;
    public javax.swing.JMenuItem mFileSave;
    public javax.swing.JMenuItem mGoEditor;
    public javax.swing.JMenuItem mGoGrammar;
    public javax.swing.JMenuItem mGoNextMark;
    public javax.swing.JMenuItem mGoWordInfo;
    public javax.swing.JMenuItem mRedo;
    public javax.swing.JRadioButtonMenuItem mSetDigits;
    public javax.swing.JRadioButtonMenuItem mSetOtherLanguage;
    public javax.swing.JRadioButtonMenuItem mSetText;
    public javax.swing.JMenuItem mUndo;
    public javax.swing.JRadioButtonMenuItem mUnk1;
    public javax.swing.JRadioButtonMenuItem mUnk2;
    public javax.swing.JRadioButtonMenuItem mUnk3;
    public javax.swing.JMenu menuEdit;
    // End of variables declaration//GEN-END:variables
}
