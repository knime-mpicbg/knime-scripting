package de.mpicbg.knime.scripting.core;

import de.mpicbg.knime.scripting.core.rgg.RGGDialogPanel;
import de.mpicbg.knime.scripting.core.rgg.TemplateUtils;
import de.mpicbg.knime.scripting.core.rgg.wizard.ScriptTemplate;

import org.knime.core.data.DataColumnSpec;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


/**
 * A container for the the rgg-panel
 *
 * @author Holger Brandl
 */
public class TemplateConfigurator extends JPanel {

    private RGGDialogPanel rggDialog;
    private ScriptTemplate template;
    private ScriptProvider scriptProvider;

    private Map<Integer, List<DataColumnSpec>> nodeInputModel;


    public TemplateConfigurator(ScriptProvider scriptProvider, boolean isReconfigurable) {
        this.scriptProvider = scriptProvider;

        initComponents();

        if (!isReconfigurable) {
            remove(reconfigureTemplatePanel);
        }
    }


    public void reconfigureUI(final ScriptTemplate newTemplate, boolean dolazyUIRebuild) {
        // do not rebuild the ui if the template has not changed
        if (dolazyUIRebuild && this.template != null && this.template.equals(newTemplate)) {
            return;
        }

        // create a dialog if there's a template with ui-config which is no detached
        this.template = newTemplate;

        String templateText = TemplateUtils.prepareScript(newTemplate.getTemplate(), nodeInputModel);
        InputStream xmlStream = new BufferedInputStream(new ByteArrayInputStream(templateText.getBytes(Charset.forName("UTF-8"))));

        rggDialog = new RGGDialogPanel(xmlStream);

        if (newTemplate.getPersistedConfig() != null) {
            rggDialog.rgg.getRggModel().restoreState(newTemplate.getPersistedConfig());
        }

        // create the ui of the template and make it visible
        rggContainer.removeAll();

        rggContainer.add(rggDialog, BorderLayout.CENTER);

        // choose which ui to show
        if (newTemplate.isLinkedToScript()) {
            scriptProvider.showTab(ScriptProvider.TEMPLATE_DIALOG);
        }
    }


    private void unlinkTemplate() {
        template.setLinkedToScript(false);
        template.setPersistedConfig(persisteTemplateUIState());

        scriptProvider.setContent(rggDialog.generateRScriptFromTemplate(), template);
        scriptProvider.showTab(ScriptProvider.SCRIPT_EDITOR);

    }


    public static Window getOwnerDialog(Container awtOwner) {
        while (awtOwner != null && !(awtOwner instanceof JDialog || awtOwner instanceof JFrame)) {
            awtOwner = awtOwner.getParent();
        }

        return (Window) awtOwner;
    }


    public String generateScript() {
        return rggDialog.generateRScriptFromTemplate();
    }


    public HashMap<String, Object> persisteTemplateUIState() {
        HashMap<String, Object> persistedUiState = new HashMap<String, Object>();

        if (rggDialog.rgg != null) { // it will be null only if the current template is not a valid one and rgg-compilation failed before
            rggDialog.rgg.getRggModel().persistState(persistedUiState);
        }

        return persistedUiState;
    }


    public void setNodeInputModel(Map<Integer, List<DataColumnSpec>> newNodeInputModel) {
        boolean shouldRebuildUI = template != null && (this.nodeInputModel == null || !this.nodeInputModel.equals(newNodeInputModel));

        this.nodeInputModel = newNodeInputModel;

        // just update the ui if the input column model has changed
        if (shouldRebuildUI) {
            reconfigureUI(template, false);
        }
    }


    private void editTemplate() {
        Window window = getOwnerDialog(scriptProvider.scriptEditor);
        final JDialog editDialog = new JDialog(window);
        editDialog.setModal(true);
        editDialog.setLayout(new BorderLayout());


        // create a script-editor to allow the user to modify the template
        ScriptEditor scriptEditor = scriptProvider.scriptEditor;
        final ScriptEditor templateEditor = new ScriptEditor(scriptEditor.colNameReformater, null);

        // make the template-editor aware of all the available columns (what a hack)
        templateEditor.updateInputModel(nodeInputModel);

        // this don't edit the script but the 
        templateEditor.setScript(template.getTemplate(), null);

        JPanel buttonBar = new JPanel(new FlowLayout());

        // 1) add the cancel button
        JButton cancelButton = new JButton("Cancel");
        cancelButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent actionEvent) {
                editDialog.setVisible(false);
            }
        });

        buttonBar.add(cancelButton);


        // add the save button
        JButton saveButton = new JButton("Save");
        saveButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent actionEvent) {
                editDialog.setVisible(false);


                ScriptTemplate changedTemplate = (ScriptTemplate) template.clone();
                // what to do after the user is done
                changedTemplate.setTemplate(templateEditor.getScript());
                changedTemplate.setPersistedConfig(persisteTemplateUIState());

                // rebuild the ui
                scriptProvider.setContent(null, changedTemplate);

            }
        });
        buttonBar.add(saveButton);


        // add the button bar to the dialog-pane
        editDialog.add(buttonBar, BorderLayout.SOUTH);


        editDialog.add(templateEditor);

        editDialog.setBounds(100, 100, 600, 700);

        editDialog.setLocationRelativeTo(null);

        // show the dialog
        editDialog.setVisible(true);
    }


    private void initComponents() {
        // JFormDesigner - Component initialization - DO NOT MODIFY  //GEN-BEGIN:initComponents
        // Generated using JFormDesigner Open Source Project license - Sphinx-4 (cmusphinx.sourceforge.net/sphinx4/)
        reconfigureTemplatePanel = new JPanel();
        detachButton = new JButton();
        editTemplateButton = new JButton();
        rggContainer = new JPanel();

        //======== this ========
        setLayout(new BorderLayout());

        //======== reconfigureTemplatePanel ========
        {
            reconfigureTemplatePanel.setLayout(new FlowLayout());

            //---- detachButton ----
            detachButton.setText("Unlink from Template");
            detachButton.setToolTipText("Uses the current configuration to create a script which \nyou can further customize beyond the options the template-dialog offers");
            detachButton.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    unlinkTemplate();
                }
            });
            reconfigureTemplatePanel.add(detachButton);

            //---- editTemplateButton ----
            editTemplateButton.setText("Edit Template");
            editTemplateButton.setToolTipText("Allows you to modify the template text itself.  If you don't hold a degree in computer science or engineering you probably don't want to do this.");
            editTemplateButton.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    editTemplate();
                }
            });
            reconfigureTemplatePanel.add(editTemplateButton);
        }
        add(reconfigureTemplatePanel, BorderLayout.SOUTH);

        //======== rggContainer ========
        {
            rggContainer.setLayout(new BorderLayout());
        }
        add(rggContainer, BorderLayout.CENTER);
        // JFormDesigner - End of component initialization  //GEN-END:initComponents
    }

    // JFormDesigner - Variables declaration - DO NOT MODIFY  //GEN-BEGIN:variables
    // Generated using JFormDesigner Open Source Project license - Sphinx-4 (cmusphinx.sourceforge.net/sphinx4/)
    private JPanel reconfigureTemplatePanel;
    private JButton detachButton;
    private JButton editTemplateButton;
    private JPanel rggContainer;
    // JFormDesigner - End of variables declaration  //GEN-END:variables


    public void enableConfigureTemplateButton(boolean isEnabled) {
        editTemplateButton.setEnabled(isEnabled);

    }


    public static String generateScript(ScriptTemplate template) {
        RGGDialogPanel rggPanel = createRGGModel(template, template.getPersistedConfig());

        return rggPanel.generateRScriptFromTemplate();
    }


    public static RGGDialogPanel createRGGModel(ScriptTemplate scriptTemplate, Map<String, Object> dialogConfig) {
        return createRGGModel(scriptTemplate.getTemplate(), dialogConfig);
    }


    public static RGGDialogPanel createRGGModel(String scriptTemplateText, Map<String, Object> dialogConfig) {
        InputStream xmlStream = new BufferedInputStream(new ByteArrayInputStream(scriptTemplateText.getBytes(Charset.forName("UTF-8"))));

        RGGDialogPanel rggPanel = new RGGDialogPanel(xmlStream);

        if (dialogConfig != null) {
            rggPanel.rgg.getRggModel().restoreState(dialogConfig);
        }
        return rggPanel;
    }
}
