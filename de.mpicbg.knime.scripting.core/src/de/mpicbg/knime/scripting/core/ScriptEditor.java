/*
 * Created by JFormDesigner on Sat Apr 17 14:46:14 CEST 2010
 */

package de.mpicbg.knime.scripting.core;

import de.mpicbg.knime.scripting.core.rgg.wizard.ScriptTemplate;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.IntValue;
import org.knime.core.data.StringValue;
import org.knime.core.node.util.DataColumnSpecListCellRenderer;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.awt.event.*;
import java.util.List;
import java.util.Map;
import java.util.Set;


/**
 * @author Holger Brandl
 */
public class ScriptEditor extends JPanel {

    private DefaultListModel attributeListModel = new DefaultListModel();

    ColNameReformater colNameReformater;
    private ScriptProvider scriptProvider;


    public ScriptEditor(ColNameReformater colNameReformater, final ScriptProvider scriptProvider) {
        this.colNameReformater = colNameReformater;
        this.scriptProvider = scriptProvider;

        initComponents();

        scriptEditorPanel.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent keyEvent) {
                super.keyPressed(keyEvent);

                boolean isRGGTemplate = scriptProvider == null || scriptProvider.getTemplate() == null || !scriptProvider.getTemplate().isRGG();
                boolean isConvert2RggShortcut = keyEvent.isMetaDown() && keyEvent.isShiftDown() && keyEvent.getKeyChar() == 'r';

                if (isRGGTemplate && isConvert2RggShortcut) {
                    int status = JOptionPane.showConfirmDialog(null, "Do you really want to convert this script into a template?", "Are you sure?", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);
                    if (status == JOptionPane.YES_OPTION) {
                        scriptProvider.getTemplate();
                        ScriptTemplate st = new ScriptTemplate();
                        st.setName("Give me a name!");
                        st.setDescription("Document me!");

                        st.setTemplate("<rgg>\n" +
                                "\n" +
                                "\n" +
                                "    <!--1. Title and short description -->\n" +
                                "\n" +
                                "    <h3 text=\"Give me a name!\" aligment=\"center\" span=\"full\"/>\n" +
                                "    <separator label=\"Description\" span=\"full\"/>\n" +
                                "    <labelarea span=\"full\">Document me!</labelarea>\n" +
                                "    <gaprow height=\"1\"/>\n" +
                                "\n" +
                                "    <!-- 2. Configuration-->\n" +
                                "\n" +
                                "    <separator label=\"Options\" span=\"full\"/>\n" +
                                "    <gaprow height=\"2\"/>\n" +
                                "\n" +
                                "    # 1. Parameter selection\n" +
                                "\n" +
                                "    <group>\n" +
                                "\n" +
                                "        <!--catVar = R$<combobox items=\"$$$FACTORS$$$\" selected-item=\"treatment\" label=\"Categorical variable of interest\"-->\n" +
                                "        <!--selected=\"T\"/>;-->\n" +
                                "\n" +
                                "        <!--params = which(names(R) %in% c(<panellistbox keepMissingOptions=\"true\" label=\"Assay parameters of interest\"-->\n" +
                                "                                                     <!--items=\"baum,haus,maus\" span=\"full\"/>));-->\n" +
                                "\n" +
                                "    </group>\n" +
                                "\n" +
                                "\n" +
                                "    # 2. R-code\n" +
                                "\n" +
                                "    <![CDATA[\n" + scriptProvider.getScript() + "\n\n]]>\n" +
                                "\n" +
                                "</rgg>");

                        scriptProvider.setContent(null, st);
                    }
                }
            }


        });
        attributeList.setModel(attributeListModel);

        scriptingPanel.add(scriptEditorPanel.createToolBar(), BorderLayout.NORTH);
//        attributeList.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        attributeList.setCellRenderer(new DataColumnSpecListCellRenderer());

        attributeList.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent mouseEvent) {
                if (mouseEvent.isControlDown()) {
                    showDomainSelector();

                } else {
                    // .. do attirbute selection
                    if (mouseEvent.isMetaDown()) {
                        return;
                    }

                    processAttributeSelection(mouseEvent.isAltDown());
                }
            }
        });

        attributeList.addKeyListener(new KeyAdapter() {
            @Override
            public void keyReleased(KeyEvent keyEvent) {
                if (keyEvent.getKeyCode() == KeyEvent.VK_META) {
                    processAttributeSelection(keyEvent.isAltDown());
                }
            }
        });
    }


    public void setScript(String script, ScriptTemplate template) {

        if (script == null) {
            script = "";
        }

        // add a reconfigure button if the script has been unlinked from a valid rgg-template
        if (template != null && template.isRGG()) {
//            assert !template.isLinkedToScript();

            if (!template.isLinkedToScript()) {
                SwingUtilities.invokeLater(new Runnable() {
                    public void run() {

                        // this try catch is another means to get rid of the nasty crashes on slow machines
                        if (scriptingPanel.getComponentCount() == 2) {
                            scriptingPanel.add(configureButton, BorderLayout.SOUTH);
                        }
                    }
                });
            }
        }

        // DEBUGGING OF CRASH ON SLOW MACHINES
        String currentText = scriptEditorPanel.getText();
        if (currentText == null) {
            scriptEditorPanel.setText(script);
            return;
        }
        // DEBUGGING OF CRASH ON SLOW MACHINES


        if (currentText.equals(script)) {
            return;
        }

        // the script has changed. let's replace it
        scriptEditorPanel.selectAll();
        scriptEditorPanel.replaceSelection(script);
//        scriptEditorPanel.setText(script);


    }


    private void showDomainSelector() {
        Object[] selValues = attributeList.getSelectedValues();

        if (selValues.length != 1) {
            return;
        }


        DataColumnSpec cspec = (DataColumnSpec) selValues[0];


        // don't do anything if its not a nominal attribute
        if (!cspec.getType().isCompatible(StringValue.class) && !cspec.getType().isCompatible(IntValue.class)) {
            attributeList.clearSelection();
            return;
        }


        Set<DataCell> domainCells = cspec.getDomain().getValues();

        if (domainCells == null) {
            JOptionPane.showMessageDialog(this, "No domain information available. Use the domain-calculator to recalculate it.");
            attributeList.clearSelection();
            return;
        }

        DefaultListModel domainModel = new DefaultListModel();
        for (DataCell dataCell : cspec.getDomain().getValues()) {
            domainModel.addElement(dataCell.toString());
        }


        DomainSelector selector = new DomainSelector(null, domainModel);
        java.util.List<String> subDomain = selector.getSelection();

        if (subDomain == null || subDomain.isEmpty()) {
            attributeList.clearSelection();
            return;
        }

        // prepare the string for the insertion
        String replacement;
        int selectionSize = subDomain.size();

        if (selectionSize == 1) {

            replacement = "\"" + subDomain.get(0) + "\"";

        } else {
            StringBuffer attrInsertList = new StringBuffer();

            // multiselection
            for (int i = 0; i < selectionSize; i++) {
                String domElement = subDomain.get(i);
                attrInsertList.append("\"" + domElement + "\"");


                if (i < selectionSize - 1) {
                    attrInsertList.append(", ");
                }
            }

            replacement = attrInsertList.toString();
        }


        // do the actual replacement in the buffer
        insertTextIntoEditor(replacement);
    }


    private void processAttributeSelection(boolean altDown) {
        Object[] selValues = attributeList.getSelectedValues();

        String replacement;
        if (selValues.length == 1) {
            DataColumnSpec cspec = (DataColumnSpec) selValues[0];
            replacement = this.colNameReformater.reformat(cspec.getName(), cspec.getType(), altDown);

        } else {
            StringBuffer attrInsertList = new StringBuffer();

            // multiselection
            for (int i = 0, selValuesLength = selValues.length; i < selValuesLength; i++) {
                Object selValue = selValues[i];
                DataColumnSpec cspec = (DataColumnSpec) selValues[i];
                String reformatAttrName = this.colNameReformater.reformat(cspec.getName(), cspec.getType(), altDown);

                attrInsertList.append(reformatAttrName);

                if (i < selValuesLength - 1) {
                    attrInsertList.append(", ");
                }
            }

            replacement = attrInsertList.toString();
        }

        // do the actual replacement in the buffer
        insertTextIntoEditor(replacement);
    }


    private void insertTextIntoEditor(String replacement) {
        scriptEditorPanel.replaceSelection(replacement);

        attributeList.clearSelection();
        scriptEditorPanel.requestFocus();
    }


    /**
     * Formats the given string by attaching the String "R$".
     *
     * @param name The name of the column to format.
     * @return The formatted column name.
     */
    private static String formatColumnName(final String name) {
        return "R$\"" + name + "\"";
    }


    private void showHelp() {
        JOptionPane.showMessageDialog(this, "Basically, there are 2 modes to copy attribute names to the script\n" +
                "1) Single selection: Simply click on an attribute and it will be pasted double-quoted into your script\n" +
                "2) Multi-selection: Keep Apple/Windows-key pressed while selecting attributes. As soon as you release the " +
                "\nkey all of them are inserted as comma-separed double-quoted list.\n\n" +
                "If you keep the ALT-key pressed while selecting attribtues, the inserted values will be inserted using a\n" +
                "scripting language specific pattern. Examples: R: R$myattribute, Groovy: new Attribute(\"myattribute\", input) \n\n" +
                "If you would like to insert a subset of the domain of a categorical attribute press the CTRL-key while \n" +
                "selecting it. A dialog will show which allows to select elements from the domain.");
    }


    private void configureButtonActionPerformed() {
        String msg = "This will reset all custom-changes you've applied to the script\n since it has been detached from its template";
        String title = "Do you really want to switch back to the dialog-mode?";

        int answer = JOptionPane.showConfirmDialog(null, msg, title, JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);
        if (answer == JOptionPane.NO_OPTION) {
            // we do nothing here because the user cancled the operation
            return;
        }

        ScriptTemplate template = scriptProvider.getTemplate();

        // reattach the node to the originally chosen template
        template.setLinkedToScript(true);
        scriptProvider.setContent(null, (ScriptTemplate) template.clone());
        scriptProvider.showTab(ScriptProvider.TEMPLATE_DIALOG);
    }


    public String getScript() {
        return scriptEditorPanel.getText();
    }


    public void resetEditorHistory() {
        scriptEditorPanel.resetHistory();
    }


    public void updateInputModel(Map<Integer, List<DataColumnSpec>> inputModel) {
        attributeListModel.removeAllElements();

        for (Integer input : inputModel.keySet()) {
            List<DataColumnSpec> columnSpecList = inputModel.get(input);
            if (columnSpecList == null) { // it will be null if we have a non-table input port
                continue;
            }

            for (DataColumnSpec columnSpec : columnSpecList) {
                attributeListModel.addElement(columnSpec);
            }
        }


        makeAttrSelectorVisible(!attributeListModel.isEmpty());

        // this is disabled because it well disable the opening of the node
//        if (attibuteListModel.size() <= 0) {
//            throw new NotConfigurableException("No valid columns (Integer, Double, String) are available!");
//        }

        repaint();
    }


    private void makeAttrSelectorVisible(boolean makeVisible) {
        if (makeVisible) {
            if (scriptingPanelContainer.getLeftComponent() == null) {
                scriptingPanelContainer.setLeftComponent(attributePanel);
                invalidate();
            }
        } else {
            if (scriptingPanelContainer.getLeftComponent() != null) {
                scriptingPanelContainer.setLeftComponent(null);
                invalidate();
            }
        }
    }


    private void initComponents() {
        // JFormDesigner - Component initialization - DO NOT MODIFY  //GEN-BEGIN:initComponents
        // Generated using JFormDesigner Open Source Project license - Sphinx-4 (cmusphinx.sourceforge.net/sphinx4/)
        scriptingPanelContainer = new JSplitPane();
        attributePanel = new JPanel();
        scrollPane2 = new JScrollPane();
        panel1 = new JPanel();
        attributeList = new JList();
        helpButton = new JButton();
        scriptingPanel = new JPanel();
        scrollPane1 = new JScrollPane();
        scriptEditorPanel = new UndoableEditPane();
        configureButton = new JButton();

        //======== this ========
        setLayout(new BorderLayout());

        //======== scriptingPanelContainer ========
        {
            scriptingPanelContainer.setDividerSize(2);
            scriptingPanelContainer.setDividerLocation(160);

            //======== attributePanel ========
            {
                attributePanel.setLayout(new BorderLayout());

                //======== scrollPane2 ========
                {
                    scrollPane2.setPreferredSize(new Dimension(160, 98));

                    //======== panel1 ========
                    {
                        panel1.setBorder(new TitledBorder("Input attributes"));
                        panel1.setLayout(new BorderLayout());
                        panel1.add(attributeList, BorderLayout.CENTER);
                    }
                    scrollPane2.setViewportView(panel1);
                }
                attributePanel.add(scrollPane2, BorderLayout.CENTER);

                //---- helpButton ----
                helpButton.setText("help");
                helpButton.addActionListener(new ActionListener() {
                    public void actionPerformed(ActionEvent e) {
                        showHelp();
                    }
                });
                attributePanel.add(helpButton, BorderLayout.SOUTH);
            }
            scriptingPanelContainer.setLeftComponent(attributePanel);

            //======== scriptingPanel ========
            {
                scriptingPanel.setLayout(new BorderLayout());

                //======== scrollPane1 ========
                {
                    scrollPane1.setViewportView(scriptEditorPanel);
                }
                scriptingPanel.add(scrollPane1, BorderLayout.CENTER);
            }
            scriptingPanelContainer.setRightComponent(scriptingPanel);
        }
        add(scriptingPanelContainer, BorderLayout.CENTER);

        //---- configureButton ----
        configureButton.setText("Reconfigure using wizard");
        configureButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                configureButtonActionPerformed();
            }
        });
        // JFormDesigner - End of component initialization  //GEN-END:initComponents
    }

    // JFormDesigner - Variables declaration - DO NOT MODIFY  //GEN-BEGIN:variables
    // Generated using JFormDesigner Open Source Project license - Sphinx-4 (cmusphinx.sourceforge.net/sphinx4/)
    private JSplitPane scriptingPanelContainer;
    private JPanel attributePanel;
    private JScrollPane scrollPane2;
    private JPanel panel1;
    private JList attributeList;
    protected JButton helpButton;
    private JPanel scriptingPanel;
    private JScrollPane scrollPane1;
    private UndoableEditPane scriptEditorPanel;
    JButton configureButton;
    // JFormDesigner - End of variables declaration  //GEN-END:variables


}
