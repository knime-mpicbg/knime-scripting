package de.mpicbg.tds.knime.knutils.scripting;

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.io.xml.DomDriver;
import de.mpicbg.tds.knime.knutils.scripting.templatewizard.ScriptTemplate;
import de.mpicbg.tds.knime.knutils.scripting.templatewizard.ScriptTemplateWizard;
import de.mpicbg.tds.knime.knutils.scripting.templatewizard.UseTemplateListenerImpl;
import org.apache.commons.lang.StringUtils;
import org.knime.core.node.*;
import org.knime.core.node.defaultnodesettings.DefaultNodeSettingsPane;
import org.knime.core.node.defaultnodesettings.SettingsModelString;
import org.knime.core.node.port.PortObjectSpec;

import javax.swing.*;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableColumn;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import static de.mpicbg.tds.knime.knutils.scripting.AbstractTableScriptingNodeModel.*;


/**
 * Document me!
 *
 * @author Holger Brandl
 */
public abstract class ScriptingNodeDialog extends DefaultNodeSettingsPane {

    public static final String SCRIPT_TAB_NAME = "Script Editor";

    private static final NodeLogger logger = NodeLogger.getLogger(ScriptingNodeDialog.class);

    private JTable table;
    private int outTableDefCounter = 1;
    private JCheckBox m_appendColsCB;


    private String defaultScript;
    private boolean hasOutput;
    private List<String> urlList;

    // the two main user interface elements: the template editor/configurator and the template repository browser
    private ScriptProvider scriptProvider;
    public ScriptTemplateWizard templateWizard;


    public static final String SCRIPT_PROPERTY = "node.script";
    public static String SCRIPT_TEMPLATE = "node.template";

    public static final String SCRIPT_TEMPLATE_DEFAULT = "";

    /**
     * Will be set by tempaltes that are deployed as acutal knime nodes.
     */
    private ScriptTemplate hardwiredTemplate;


    /**
     * New pane for configuring ScriptedNode node dialog
     */
    public ScriptingNodeDialog(String defaultScript, ColNameReformater colNameReformater, boolean hasOutput, boolean enableTemplateRepository) {
        this.defaultScript = defaultScript;
        this.hasOutput = hasOutput;


        // construct the panel for script loading/authoring
        scriptProvider = new ScriptProvider(colNameReformater, isReconfigurable());
        JPanel scriptDialogContainer = new JPanel(new BorderLayout());
        scriptDialogContainer.add(scriptProvider, BorderLayout.CENTER);
        this.addTabAt(0, SCRIPT_TAB_NAME, scriptDialogContainer);


        // create the template repository browser tab (if enabled)
        if (enableTemplateRepository) {

            // parse URLs and update the cache
            /*TemplateCache templateCache = TemplateCache.getInstance();
            List<URL> urlList = templateCache.parseConcatendatedURLs(templateResources);
            List<ScriptTemplate> templates = new ArrayList<ScriptTemplate>();
            List<String> warnings = new ArrayList<String>();
            for(URL filePath : urlList) {
                try {
                    templates.addAll(templateCache.getTemplateCache(filePath));
                } catch (IOException e) { warnings.add(e.getMessage()); }
            }
            // show warning if files are empty or could not be read
            if(!warnings.isEmpty()) {
                logger.warn(warnings);
            }     */
            updateUrlList(getTemplatesFromPreferences());
            List<ScriptTemplate> templates = updateTemplates();

            //templateWizard = new ScriptTemplateWizard(templateResources);
            templateWizard = new ScriptTemplateWizard(this, templates);
            templateWizard.addUseTemplateListener(new UseTemplateListenerImpl(this));
            this.addTabAt(1, "Templates", templateWizard);
        }


        if (hasOutput) {
            addTab("Script Output", createOutputModelPanel());
        }

        removeTab("Options");
        selectTab(SCRIPT_TAB_NAME);
    }

    /**
     * parses a preference string and fills list of URLs
     *
     * @param templatesFromPreferences
     */
    public void updateUrlList(String templatesFromPreferences) {
        TemplateCache templateCache = TemplateCache.getInstance();
        urlList = templateCache.parseConcatendatedURLs(templatesFromPreferences);
    }


    public void useTemplate(ScriptTemplate template) {
        selectScriptTab();
        scriptProvider.setContent(template.getTemplate(), template);
    }


    /**
     * @return <code>true</code> if the node-template should be acessible to the user. False if not.
     */
    protected boolean isReconfigurable() {
        return true;
    }


    private JPanel createOutputModelPanel() {
        // construct the output column selection panel
        JPanel outputPanel = new JPanel();
        outputPanel.setLayout(new BoxLayout(outputPanel, BoxLayout.Y_AXIS));
        JPanel outputButtonPanel = new JPanel();
        JPanel outputMainPanel = new JPanel(new BorderLayout());
        JPanel newtableCBPanel = new JPanel();
        m_appendColsCB = new JCheckBox("Append columns to input table");
        newtableCBPanel.add(m_appendColsCB, BorderLayout.WEST);
        JButton addButton = new JButton(new AbstractAction() {

            public void actionPerformed(ActionEvent e) {
                ((ScriptNodeOutputColumnsTableModel) table.getModel()).addRow("script output " + outTableDefCounter,
                        "String");
                outTableDefCounter++;
            }
        });
        addButton.setText("Add Output Column");

        JButton removeButton = new JButton(new AbstractAction() {

            public void actionPerformed(ActionEvent e) {
                int[] selectedRows = table.getSelectedRows();

                if (selectedRows.length == 0) {
                    return;
                }

                for (int i = selectedRows.length - 1; i >= 0; i--) {
                    ((ScriptNodeOutputColumnsTableModel) table.getModel()).removeRow(selectedRows[i]);
                }
            }
        });
        removeButton.setText("Remove Output Column");

        outputButtonPanel.add(addButton);
        outputButtonPanel.add(removeButton);

        table = new JTable();
        table.putClientProperty("terminateEditOnFocusLost", Boolean.TRUE);

        table.setAutoscrolls(true);
        ScriptNodeOutputColumnsTableModel model = new ScriptNodeOutputColumnsTableModel();
        model.addColumn("Column name");
        model.addColumn("Column type");
        model.addRow("script output " + outTableDefCounter, "String");
        outTableDefCounter++;
        table.setModel(model);

        outputMainPanel.add(table.getTableHeader(), BorderLayout.PAGE_START);
        outputMainPanel.add(table, BorderLayout.CENTER);
        outputMainPanel.add(m_appendColsCB, BorderLayout.SOUTH);
//        outputPanel.add(newtableCBPanel);
        outputPanel.add(outputButtonPanel);
        outputPanel.add(outputMainPanel);

        TableColumn typeColumn = table.getColumnModel().getColumn(1);
        JComboBox typeSelector = new JComboBox();
        typeSelector.addItem("String");
        typeSelector.addItem("Integer");
        typeSelector.addItem("Double");
        typeColumn.setCellEditor(new DefaultCellEditor(typeSelector));
        return outputPanel;
    }


    @Override
    public void loadAdditionalSettingsFrom(final NodeSettingsRO settings,
                                           final PortObjectSpec[] specs) throws NotConfigurableException {
        super.loadAdditionalSettingsFrom(settings, specs);

        String serializedTemplate = settings.getString(SCRIPT_TEMPLATE, SCRIPT_TEMPLATE_DEFAULT);


        ScriptTemplate template = null;
        if (getHardwiredTemplate() != null && serializedTemplate.equals(SCRIPT_TEMPLATE_DEFAULT)) {
            template = getHardwiredTemplate();

        } else {
            if (StringUtils.isNotBlank(serializedTemplate)) {
                template = deserializeTemplate(serializedTemplate);
            }
        }

        if (getHardwiredTemplate() != null) {
            ScriptTemplate predefTemplate = getHardwiredTemplate();
            if (predefTemplate == null) {
                throw new RuntimeException("Hardwired nodes require a predefined template");
            }

            if (template != null) {
                predefTemplate.setPersistedConfig(template.getPersistedConfig());
                predefTemplate.setLinkedToScript(template.isLinkedToScript());
            }

            // use the hardwired template instead of the persisted one
            template = predefTemplate;
        }


        final ScriptTemplate finalTemplate = template;
        final String script = settings.getString(SCRIPT_PROPERTY, defaultScript);

        // update the ui but not from within the application thread
//        SwingUtilities.invokeLater(new Runnable() {
//            public void run() {

        try {
            scriptProvider.updateInputModel(specs);
        } catch (NotConfigurableException e) {
            e.printStackTrace();
        }

        scriptProvider.setContent(script, finalTemplate);
        // scriptEditor.resetEditorHistory();
//            }
//        });


        if (hasOutput) {
            loadOutputModel(settings);
        }
    }


    public static ScriptTemplate deserializeTemplate(String serializedTemplate) {
        if (serializedTemplate == null || StringUtils.isBlank(serializedTemplate)) {
            return null;
        }

        return (ScriptTemplate) new XStream(new DomDriver()).fromXML(serializedTemplate);
    }


    private void loadOutputModel(NodeSettingsRO settings) {
        boolean appendCols = settings.getBoolean(APPEND_COLS, true);
        m_appendColsCB.setSelected(appendCols);

        String[] dataTableColumnNames =
                settings.getStringArray(COLUMN_NAMES, new String[0]);
        String[] dataTableColumnTypes =
                settings.getStringArray(COLUMN_TYPES, new String[0]);

        ((ScriptNodeOutputColumnsTableModel) table.getModel()).clearRows();

        if (dataTableColumnNames == null) {
            return;
        }

        for (int i = 0; i < dataTableColumnNames.length; i++) {
            ((ScriptNodeOutputColumnsTableModel) table.getModel()).addRow(dataTableColumnNames[i],
                    dataTableColumnTypes[i]);
        }
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public void saveAdditionalSettingsTo(final NodeSettingsWO settings)
            throws InvalidSettingsException {
        super.saveAdditionalSettingsTo(settings);

        SettingsModelString scriptProperty = createSnippetProperty(defaultScript);
        scriptProperty.setStringValue(scriptProvider.getScript());
        scriptProperty.saveSettingsTo(settings);

//        settings.addString(SCRIPT_PROPERTY, scriptProvider.getScript());

        ScriptTemplate template = scriptProvider.getTemplate();
        SettingsModelString templateProperty = createTemplateProperty();

        if (template != null) {

            // save the state of the configuration dialog if there's a template and it is linked to the script
            if (template.isLinkedToScript() && template.isRGG()) {
                HashMap<String, Object> persistedUIConfig = scriptProvider.getTemplateConfigurator().persisteTemplateUIState();
                template.setPersistedConfig(persistedUIConfig);
            }

            // save the template as xml (with is kinda hacky) but makes live much easier here
            templateProperty.setStringValue(new XStream().toXML(scriptProvider.getTemplate()));

        } else {
            templateProperty.setStringValue("");
        }

        templateProperty.saveSettingsTo(settings);

        if (hasOutput) {
            saveOutputModel(settings);
        }
    }


    private void saveOutputModel(NodeSettingsWO settings) {
        // work around a jtable cell value persistence problem
        // by explicitly stopping editing if a cell is currently in edit mode
        int editingRow = table.getEditingRow();
        int editingColumn = table.getEditingColumn();

        if (editingRow != -1 && editingColumn != -1) {
            TableCellEditor editor = table.getCellEditor(editingRow, editingColumn);
            editor.stopCellEditing();
        }

        settings.addBoolean(APPEND_COLS, m_appendColsCB.isSelected());
        String[] columnNames =
                ((ScriptNodeOutputColumnsTableModel) table.getModel()).getDataTableColumnNames();
        settings.addStringArray(COLUMN_NAMES, columnNames);

        String[] columnTypes =
                ((ScriptNodeOutputColumnsTableModel) table.getModel()).getDataTableColumnTypes();
        settings.addStringArray(COLUMN_TYPES, columnTypes);
    }


    public void selectScriptTab() {
        selectTab(SCRIPT_TAB_NAME);
    }


    public ScriptTemplate getHardwiredTemplate() {
        return hardwiredTemplate;
    }


    public void setHardwiredTemplate(ScriptTemplate predefinedTemplate) {
        scriptProvider.getTemplateConfigurator().enableConfigureTemplateButton(false);
        this.hardwiredTemplate = predefinedTemplate;
    }

    public abstract String getTemplatesFromPreferences();

    public List<ScriptTemplate> updateTemplates() {
        TemplateCache templateCache = TemplateCache.getInstance();

        List<ScriptTemplate> templates = new ArrayList<ScriptTemplate>();
        List<String> warnings = new ArrayList<String>();

        for (String filePath : urlList) {
            try {
                templates.addAll(templateCache.updateTemplateCache(filePath));
            } catch (IOException e) {
                warnings.add(e.getMessage());
            }
        }

        // show warning if files are empty or could not be read
        if (!warnings.isEmpty()) {
            logger.warn(warnings);
        }

        return templates;
    }
}
