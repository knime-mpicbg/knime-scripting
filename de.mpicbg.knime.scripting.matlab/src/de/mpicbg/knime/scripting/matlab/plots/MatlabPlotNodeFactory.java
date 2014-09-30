package de.mpicbg.knime.scripting.matlab.plots;

import de.mpicbg.knime.scripting.matlab.MatlabScriptingBundleActivator;
import de.mpicbg.knime.scripting.matlab.prefs.MatlabPreferenceInitializer;

import org.knime.core.node.*;
import org.knime.core.node.defaultnodesettings.SettingsModelBoolean;
import org.knime.core.node.defaultnodesettings.SettingsModelInteger;
import org.knime.core.node.defaultnodesettings.SettingsModelIntegerBounded;
import org.knime.core.node.defaultnodesettings.SettingsModelString;


/**
 * @author Holger Brandl
 */
public class MatlabPlotNodeFactory extends NodeFactory<MatlabPlotNodeModel> {

    @Override
    public MatlabPlotNodeModel createNodeModel() {
        return new MatlabPlotNodeModel();
    }


    @Override
    public int getNrNodeViews() {
        return 1;
    }


    @Override
    public NodeView<MatlabPlotNodeModel> createNodeView(final int viewIndex, final MatlabPlotNodeModel nodeModel) {
        return new MatlabPlotNodeView(nodeModel);
    }


    @Override
    public boolean hasDialog() {
        return true;
    }


    @Override
    public NodeDialogPane createNodeDialogPane() {
        String templateResources = MatlabScriptingBundleActivator.getDefault().getPreferenceStore().getString(MatlabPreferenceInitializer.MATLAB_PLOT_TEMPLATE_RESOURCES);
        return new MatlabPlotNodeDialog(templateResources, MatlabPlotNodeFactory.this.createNodeModel().getDefaultScript(), enableTemplateRepository());
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
