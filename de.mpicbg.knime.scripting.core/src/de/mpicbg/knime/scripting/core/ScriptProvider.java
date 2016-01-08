/*
 * Created by JFormDesigner on Tue Jun 08 09:52:28 CEST 2010
 */

package de.mpicbg.knime.scripting.core;

import de.mpicbg.knime.scripting.core.rgg.wizard.ScriptTemplate;

import org.knime.core.data.*;
import org.knime.core.data.StringValue;
import org.knime.core.node.NotConfigurableException;
import org.knime.core.node.port.PortObject;
import org.knime.core.node.port.PortObjectSpec;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


/**
 * @author Holger Brandl
 */
public class ScriptProvider extends JPanel{

    public static final String SCRIPT_EDITOR = "script_editor";
    public static final String TEMPLATE_DIALOG = "template_ui";

    // the script (which may come along with user interface description or not) currently used by this script provider
    private ScriptTemplate template;

    // the 2 main UI containers: a simple editor and an rgg-based visual script configurator
    public ScriptEditor scriptEditor;
    public TemplateConfigurator templateConfigurator;
    
    ColumnSupport m_colSupport;

    public ScriptProvider(ColumnSupport colNameReformater, boolean isReconfigurable) {
    	
    	this.m_colSupport = colNameReformater;
    	
        setLayout(new ResizableCardLayout());

        scriptEditor = new ScriptEditor(colNameReformater, this);
        add(scriptEditor, SCRIPT_EDITOR);

        templateConfigurator = new TemplateConfigurator(this, isReconfigurable);
        add(templateConfigurator, TEMPLATE_DIALOG);
    }


    public void setContent(String script, final ScriptTemplate newTemplate) {
        this.template = newTemplate;

        scriptEditor.setScript(script, newTemplate);

        if (template != null && template.isRGG() && template.isLinkedToScript()) {
            // don't do anything if the parameters have not changed
            templateConfigurator.reconfigureUI(newTemplate, false);

        } else {
            // switch to the scripting panel if necessary (no template or unlinked)
            showTab(ScriptProvider.SCRIPT_EDITOR);
        }
    }


    /**
     * Updates the list of columns based on the given table spec.
     */
    public void updateInputModel(PortObjectSpec[] specs) throws NotConfigurableException {

        Map<Integer, List<DataColumnSpec>> inputModel = getPushableInputSpec(specs, m_colSupport);

        scriptEditor.updateInputModel(inputModel);
        templateConfigurator.setNodeInputModel(inputModel);
    }


    public Map<Integer, List<DataColumnSpec>> reshapeInputStructure(PortObject[] inData) {
        PortObjectSpec[] portSpecs = unwrapPortSpecs(inData);

        return getPushableInputSpec(portSpecs, m_colSupport);
    }


    public static PortObjectSpec[] unwrapPortSpecs(PortObject[] inData) {
        PortObjectSpec[] portSpecs = new PortObjectSpec[inData.length];
        for (int i = 0; i < inData.length; i++) {
            PortObject portObject = inData[i];
            portSpecs[i] = portObject.getSpec();
        }
        return portSpecs;
    }

    /**
     * creates a map with entries per input port, data table inputs get a R compatible columns as values
     * (List of DataColumnSpecs)
     * @param specs - array of input port specs
     * @return map
     */
    public static Map<Integer, List<DataColumnSpec>> getPushableInputSpec(PortObjectSpec[] specs, ColumnSupport colSupport) {
        Map<Integer, List<DataColumnSpec>> inputModel = new HashMap<Integer, List<DataColumnSpec>>();

        for (int inputIndex = 0; inputIndex < specs.length; inputIndex++) {
            PortObjectSpec spec = specs[inputIndex];

            // no data table: add to map and give null as value
            if (!(spec instanceof DataTableSpec)) {
                inputModel.put(inputIndex, null);
                continue;
            }

            DataTableSpec tableSpec = (DataTableSpec) spec;
            List<DataColumnSpec> compatibleColSpecs = new ArrayList<DataColumnSpec>();

            // add compatible types only
            for (int colIndex = 0; colIndex < tableSpec.getNumColumns(); colIndex++) {
                DataColumnSpec cspec = tableSpec.getColumnSpec(colIndex);
                DataType type = cspec.getType();
                
                if(colSupport.isSupported(type))
                	compatibleColSpecs.add(cspec);
            }
            inputModel.put(inputIndex, compatibleColSpecs);
        }
        return inputModel;
    }


    public void showTab(final String tabName) {
        // choose which ui to show
        CardLayout cl = (CardLayout) (ScriptProvider.this.getLayout());
        cl.show(ScriptProvider.this, tabName);
    }


    public ScriptEditor getEditor() {
        return scriptEditor;
    }


    public TemplateConfigurator getTemplateConfigurator() {
        return templateConfigurator;
    }


    public ScriptTemplate getTemplate() {
        return template;
    }


    public String getScript() {
        if (template == null || !template.isLinkedToScript() || !template.isRGG()) {
            return scriptEditor.getScript();
        }

        return templateConfigurator.generateScript();
    }
}
