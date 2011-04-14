/*
 * Created by JFormDesigner on Tue May 04 10:16:24 CEST 2010
 */

package de.mpicbg.tds.knime.knutils.scripting;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.List;


/**
 * @author Holger Brandl
 */
public class DomainSelector extends JDialog {

    private List<String> curSelection = new ArrayList<String>();


    public DomainSelector(Frame owner) {
        super(owner);
        initComponents();
    }


    public DomainSelector(Dialog owner, ListModel domainModel) {
        super(owner);
        initComponents();


        domainList.setModel(domainModel);
        domainList.invalidate();

        setModal(true);
        setVisible(true);
    }


    private void updateSelection() {
        curSelection.clear();

        for (Object o : domainList.getSelectedValues()) {
            curSelection.add((String) o);
        }
    }


    private void selectionCanceled() {
        curSelection.clear(); // just to make sure
        closeDialog();
    }


    private void insertSelection() {
        for (Object o : domainList.getSelectedValues()) {
            curSelection.add((String) o);
        }
        closeDialog();
    }


    private void closeDialog() {
        setVisible(false);
        dispose();
    }


    private void initComponents() {
        // JFormDesigner - Component initialization - DO NOT MODIFY  //GEN-BEGIN:initComponents
        // Generated using JFormDesigner Open Source Project license - Sphinx-4 (cmusphinx.sourceforge.net/sphinx4/)
        scrollPane1 = new JScrollPane();
        domainList = new JList();
        panel1 = new JPanel();
        cancelButton = new JButton();
        insertButton = new JButton();

        //======== this ========
        setTitle("Select a subset");
        Container contentPane = getContentPane();
        contentPane.setLayout(new BorderLayout());

        //======== scrollPane1 ========
        {
            scrollPane1.setViewportView(domainList);
        }
        contentPane.add(scrollPane1, BorderLayout.CENTER);

        //======== panel1 ========
        {
            panel1.setLayout(new BorderLayout());

            //---- cancelButton ----
            cancelButton.setText("Cancel");
            cancelButton.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    selectionCanceled();
                }
            });
            panel1.add(cancelButton, BorderLayout.WEST);

            //---- insertButton ----
            insertButton.setText("Insert Selection");
            insertButton.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    insertSelection();
                }
            });
            panel1.add(insertButton, BorderLayout.CENTER);
        }
        contentPane.add(panel1, BorderLayout.SOUTH);
        pack();
        setLocationRelativeTo(getOwner());
        // JFormDesigner - End of component initialization  //GEN-END:initComponents
    }

    // JFormDesigner - Variables declaration - DO NOT MODIFY  //GEN-BEGIN:variables
    // Generated using JFormDesigner Open Source Project license - Sphinx-4 (cmusphinx.sourceforge.net/sphinx4/)
    private JScrollPane scrollPane1;
    private JList domainList;
    private JPanel panel1;
    private JButton cancelButton;
    private JButton insertButton;
    // JFormDesigner - End of variables declaration  //GEN-END:variables


    public List<String> getSelection() {
        return curSelection;
    }
}
