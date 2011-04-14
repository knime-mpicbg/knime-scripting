/*
 * Created by JFormDesigner on Tue Jun 08 09:52:28 CEST 2010
 */

package de.mpicbg.tds.knime.knutils.scripting;

import de.mpicbg.tds.knime.knutils.scripting.templatewizard.ScriptTemplate;
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
public class ScriptProvider extends JPanel {

    public static final String SCRIPT_EDITOR = "script_editor";
    public static final String TEMPLATE_DIALOG = "template_ui";

    // the script (which may come along with user interface description or not) currently used by this script provider
    private ScriptTemplate template;

    // the 2 main UI containers: a smple editor and an rgg-based visual script configurator
    public ScriptEditor scriptEditor;
    public TemplateConfigurator templateConfigurator;


    public ScriptProvider(ColNameReformater colNameReformater, boolean isReconfigurable) {
        setLayout(new CardLayout());

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

        Map<Integer, List<DataColumnSpec>> inputModel = reshapeInputStructure(specs);

        scriptEditor.updateInputModel(inputModel);
        templateConfigurator.setNodeInputModel(inputModel);
    }


    public static Map<Integer, List<DataColumnSpec>> reshapeInputStructure(PortObject[] inData) {
        PortObjectSpec[] portSpecs = unwrapPortSpecs(inData);

        return reshapeInputStructure(portSpecs);
    }


    public static PortObjectSpec[] unwrapPortSpecs(PortObject[] inData) {
        PortObjectSpec[] portSpecs = new PortObjectSpec[inData.length];
        for (int i = 0; i < inData.length; i++) {
            PortObject portObject = inData[i];
            portSpecs[i] = portObject.getSpec();
        }
        return portSpecs;
    }


    public static Map<Integer, List<DataColumnSpec>> reshapeInputStructure(PortObjectSpec[] specs) {
        Map<Integer, List<DataColumnSpec>> inputModel = new HashMap<Integer, List<DataColumnSpec>>();

        for (int inputIndex = 0; inputIndex < specs.length; inputIndex++) {
            PortObjectSpec spec = specs[inputIndex];

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

                if (type.isCompatible(IntValue.class)) {
                    compatibleColSpecs.add(cspec);
                } else if (type.isCompatible(DoubleValue.class)) {
                    compatibleColSpecs.add(cspec);
                } else if (type.isCompatible(StringValue.class)) {
                    compatibleColSpecs.add(cspec);
                }
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
