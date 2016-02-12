package de.mpicbg.knime.scripting.python.plots;

import de.mpicbg.knime.scripting.python.PythonScriptingBundleActivator;
import de.mpicbg.knime.scripting.python.prefs.PythonPreferenceInitializer;

import org.knime.core.node.*;
import org.knime.core.node.defaultnodesettings.SettingsModelBoolean;
import org.knime.core.node.defaultnodesettings.SettingsModelInteger;
import org.knime.core.node.defaultnodesettings.SettingsModelIntegerBounded;
import org.knime.core.node.defaultnodesettings.SettingsModelString;


/**
 * @author Holger Brandl
 */
public class PythonPlotNodeFactory extends NodeFactory<PythonPlotNodeModel> {

    @Override
    public PythonPlotNodeModel createNodeModel() {
        return new PythonPlotNodeModel();
    }


    @Override
    public int getNrNodeViews() {
        return 1;
    }


    @Override
    public NodeView<PythonPlotNodeModel> createNodeView(final int viewIndex, final PythonPlotNodeModel nodeModel) {
        return new PythonPlotNodeView(nodeModel);
    }


    @Override
    public boolean hasDialog() {
        return true;
    }


    @Override
    public NodeDialogPane createNodeDialogPane() {
        String templateResources = PythonScriptingBundleActivator.getDefault().getPreferenceStore().getString(PythonPreferenceInitializer.PYTHON_PLOT_TEMPLATE_RESOURCES);
        return new PythonPlotNodeDialog(templateResources, PythonPlotNodeFactory.this.createNodeModel().getDefaultScript(""), enableTemplateRepository());
    }


    protected boolean enableTemplateRepository() {
        return true;
    }


    public static SettingsModelInteger createPropFigureWidth() {
        return new SettingsModelIntegerBounded("figure.width", 1000, 100, 5000);
    }


    public static SettingsModelInteger createPropFigureHeight() {
        return new SettingsModelIntegerBounded("figure.height", 700, 100, 5000);
    }


    public static SettingsModelBoolean createOverwriteFile() {
        return new SettingsModelBoolean("overwrite.ok", false);
    }


    public static SettingsModelString createPropOutputFile() {
        return new SettingsModelString("figure.output.file", "") {
            @Override
            protected void validateSettingsForModel(NodeSettingsRO settings) throws InvalidSettingsException {
//                super.validateSettingsForModel(settings);
            }
        };
    }


    public static SettingsModelString createPropOutputType() {
        return new SettingsModelString("figure.ouput.type", "png");
    }
}
