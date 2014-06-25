package de.mpicbg.knime.knutils.ui;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.Arrays;


/**
 * Document me!
 *
 * @author Holger Brandl
 */
public class TwoPanelSelectTester {


    public static void main(String[] args) {
        JDialog jDialog = new JDialog((Frame) null, true);

        jDialog.setBounds(100, 100, 500, 500);

        TwoPaneSelectionPanel<String> selectionPanel = new TwoPaneSelectionPanel<String>(false, new DefaultListCellRenderer());
        selectionPanel.update(Arrays.asList("test", "baum", "haus", "lbladf"), new ArrayList<String>());
        selectionPanel.invalidate();
        jDialog.add(selectionPanel);

        jDialog.setVisible(true);


        System.err.println("includes" + selectionPanel.getIncludedColumnSet());
        System.err.println("excludes" + selectionPanel.getExcludedColumnSet());
    }

}
