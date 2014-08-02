package de.mpicbg.knime.scripting.core;

import de.mpicbg.knime.knutils.AbstractNodeModel;
import de.mpicbg.knime.scripting.core.rgg.RGGDialogPanel;
import de.mpicbg.knime.scripting.core.rgg.TemplateUtils;
import de.mpicbg.knime.scripting.core.rgg.wizard.ScriptTemplate;

import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataTableSpec;
import org.knime.core.node.*;
import org.knime.core.node.defaultnodesettings.SettingsModelString;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.core.node.port.PortType;

import java.util.HashMap;
import java.util.List;
import java.util.Map;


/**
 * Document me!
 *
 * @author Holger Brandl
 */
public abstract class AbstractScriptingNodeModel extends AbstractNodeModel {

    /**
     * The property for the string.
     */
    protected final SettingsModelString script;
    protected final SettingsModelString template;
    protected int numOutputs;
    protected int numInputs;


    private ScriptTemplate hardwiredTemplate = null;
    private String contextAwareHWTemplateText;


    public AbstractScriptingNodeModel(PortType[] inPorts, PortType[] outPorts) {
    	this(inPorts, outPorts, false);
    }
    
    public AbstractScriptingNodeModel(PortType[] inPorts, PortType[] outPorts, boolean useNewSettingsHashmap) {
        super(inPorts, outPorts, useNewSettingsHashmap);

        numInputs = inPorts.length;
        numOutputs = outPorts.length;

        script = createSnippetProperty(getDefaultScript());
        template = createTemplateProperty();
    }


    public void setHardwiredTemplate(ScriptTemplate hardwiredTemplate) {
        // note we clone it here as it might be (and will be in most cases) an instance variable in the node factory.
        this.hardwiredTemplate = (ScriptTemplate) hardwiredTemplate.clone();
    }


    public ScriptTemplate getHardwiredTemplate() {
        return hardwiredTemplate;
    }


    public static SettingsModelString createSnippetProperty(String defaultScript) {
        return new SettingsModelString(ScriptingNodeDialog.SCRIPT_PROPERTY, defaultScript);
    }


    public static SettingsModelString createTemplateProperty() {
        return new SettingsModelString(ScriptingNodeDialog.SCRIPT_TEMPLATE, ScriptingNodeDialog.SCRIPT_TEMPLATE_DEFAULT);
    }


    @Override
    protected BufferedDataTable[] execute(BufferedDataTable[] inData, ExecutionContext exec) throws Exception {
        throw new RuntimeException("Could not execute node: Node implementation needs to override execute behavior");
    }


    @Override
    protected DataTableSpec[] configure(DataTableSpec[] inSpecs) throws InvalidSettingsException {
        // adapt hardwired templates to the input specs. Important: this just applies to nodes with outputs.
        // Plot-nodes need to be handled separately
        adaptHardwiredTemplateToContext(inSpecs);

        return super.configure(inSpecs);
    }


    public String getDefaultScript() {
        return "";
    }


    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) {
        super.saveSettingsTo(settings);

        script.saveSettingsTo(settings);
        template.saveSettingsTo(settings);
    }


    @Override
    protected void loadValidatedSettingsFrom(final NodeSettingsRO settings)
            throws InvalidSettingsException {
        super.loadValidatedSettingsFrom(settings);

        // It can be safely assumed that the settings are valided by the
        // method below.

        try {
            script.loadSettingsFrom(settings);
        } catch (Throwable t) {
        }

        try {
            template.loadSettingsFrom(settings);
        } catch (Throwable t) {
            throw new RuntimeException("Could not unpersist template");
        }

    }


    @Override
    protected void validateSettings(final NodeSettingsRO settings)
            throws InvalidSettingsException {
        super.validateSettings(settings);

        // e.g. if the count is in a certain range (which is ensured by the
        // SettingsModel).
        // Do not actually set any values of any member variables.

//        script.validateSettings(settings);
    }


    protected boolean hasOutput() {
        return numOutputs > 0;
    }


    protected void adaptHardwiredTemplateToContext(PortObjectSpec[] inData) {
        if (hardwiredTemplate != null && hardwiredTemplate.isLinkedToScript()) {
            Map<Integer, List<DataColumnSpec>> nodeInputModel = ScriptProvider.reshapeInputStructure(inData);
            contextAwareHWTemplateText = TemplateUtils.prepareScript(hardwiredTemplate.getTemplate(), nodeInputModel);
        }
    }


    protected void adaptHardwiredTemplateToContext(DataTableSpec[] inData) {
        if (hardwiredTemplate != null && hardwiredTemplate.isLinkedToScript()) {
            Map<Integer, List<DataColumnSpec>> nodeInputModel = ScriptProvider.reshapeInputStructure(inData);
            contextAwareHWTemplateText = TemplateUtils.prepareScript(hardwiredTemplate.getTemplate(), nodeInputModel);
        }
    }


    public Map<String, Object> getTemplateConfig(PortObjectSpec[] inSpecs) {
        adaptHardwiredTemplateToContext(inSpecs);

        return getTemplateConfigInternal();
    }


    public Map<String, Object> getTemplateConfig(DataTableSpec[] inSpecs) {
        adaptHardwiredTemplateToContext(inSpecs);

        return getTemplateConfigInternal();
    }


    private Map<String, Object> getTemplateConfigInternal() {
        ScriptTemplate deserializedTemplate = ScriptingNodeDialog.deserializeTemplate(template.getStringValue());

        if (deserializedTemplate == null) {

            if (hardwiredTemplate.getTemplate() != null) {

                RGGDialogPanel panel = TemplateConfigurator.createRGGModel(contextAwareHWTemplateText, null);

                HashMap<String, Object> persistedUiState = new HashMap<String, Object>();
                panel.rgg.getRggModel().persistState(persistedUiState);
                return persistedUiState;
            }

            throw new RuntimeException("no template confugration found for current template.");
        }

        return deserializedTemplate.getPersistedConfig();
    }


    /**
     * This method is usually just called from within the different execute implementations. Occassionally it is also
     * called in the view implemntations.
     */
    public String prepareScript() {


        String script;

        String serializedTemplate = template.getStringValue();
        ScriptTemplate restoredTemplate = ScriptingNodeDialog.deserializeTemplate(serializedTemplate);


        // if the node is a hard-wired one, use the default template, otherwise use the script as saved in the template definition
        if (contextAwareHWTemplateText == null || hardwiredTemplate == null || (restoredTemplate != null && !restoredTemplate.isLinkedToScript())) {
            script = this.script.getStringValue();

        } else {
            ScriptTemplate contextAwareHWTemplate = new ScriptTemplate();
            contextAwareHWTemplate.setName("context-aware hardwired tempalte");
            contextAwareHWTemplate.setTemplate(contextAwareHWTemplateText);

            if (restoredTemplate != null) {
                Map<String, Object> uiConfig = restoredTemplate.getPersistedConfig();

                if (uiConfig != null) {
                    contextAwareHWTemplate.setPersistedConfig(uiConfig);
                }
            }

            script = TemplateConfigurator.generateScript(contextAwareHWTemplate);
        }

        // replace flow-variables
        return FlowVarUtils.replaceFlowVars(script, this);
    }
}
