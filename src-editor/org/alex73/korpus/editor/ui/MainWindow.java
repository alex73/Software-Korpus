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
        jMenuBar1 = new javax.swing.JMenuBar();
        jMenu1 = new javax.swing.JMenu();
        mFileOpen = new javax.swing.JMenuItem();
        mFileSave = new javax.swing.JMenuItem();
        jMenu4 = new javax.swing.JMenu();
        mUndo = new javax.swing.JMenuItem();
        mRedo = new javax.swing.JMenuItem();
        jMenu2 = new javax.swing.JMenu();
        mUnk1 = new javax.swing.JRadioButtonMenuItem();
        mUnk2 = new javax.swing.JRadioButtonMenuItem();
        mUnk3 = new javax.swing.JRadioButtonMenuItem();
        jSeparator2 = new javax.swing.JPopupMenu.Separator();
        mFontDec = new javax.swing.JMenuItem();
        mFontInc = new javax.swing.JMenuItem();
        jMenu3 = new javax.swing.JMenu();
        mGoNextMark = new javax.swing.JMenuItem();
        jSeparator1 = new javax.swing.JPopupMenu.Separator();
        mGoEditor = new javax.swing.JMenuItem();
        mGoWordInfo = new javax.swing.JMenuItem();
        mGoGrammar = new javax.swing.JMenuItem();

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);
        setTitle("Рэдагаваньне файлаў корпусу, v.1.1");

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
        jMenu1.add(mFileSave);

        jMenuBar1.add(jMenu1);

        jMenu4.setText("Рэдагаваньне");

        mUndo.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_Z, java.awt.event.InputEvent.CTRL_MASK));
        mUndo.setText("Адрабіць");
        jMenu4.add(mUndo);

        mRedo.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_Y, java.awt.event.InputEvent.CTRL_MASK));
        mRedo.setText("Узнавіць");
        jMenu4.add(mRedo);

        jMenuBar1.add(jMenu4);

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

        mFontDec.setText("Паменьшыць шрыфт");
        jMenu2.add(mFontDec);

        mFontInc.setText("Павялічыць шрыфт");
        jMenu2.add(mFontInc);

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
    public javax.swing.JMenu jMenu1;
    public javax.swing.JMenu jMenu2;
    public javax.swing.JMenu jMenu3;
    public javax.swing.JMenu jMenu4;
    public javax.swing.JMenuBar jMenuBar1;
    public javax.swing.JPopupMenu.Separator jSeparator1;
    public javax.swing.JPopupMenu.Separator jSeparator2;
    public javax.swing.JMenuItem mFileOpen;
    public javax.swing.JMenuItem mFileSave;
    public javax.swing.JMenuItem mFontDec;
    public javax.swing.JMenuItem mFontInc;
    public javax.swing.JMenuItem mGoEditor;
    public javax.swing.JMenuItem mGoGrammar;
    public javax.swing.JMenuItem mGoNextMark;
    public javax.swing.JMenuItem mGoWordInfo;
    public javax.swing.JMenuItem mRedo;
    public javax.swing.JMenuItem mUndo;
    public javax.swing.JRadioButtonMenuItem mUnk1;
    public javax.swing.JRadioButtonMenuItem mUnk2;
    public javax.swing.JRadioButtonMenuItem mUnk3;
    // End of variables declaration//GEN-END:variables
}
