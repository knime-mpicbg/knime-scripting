package de.mpicbg.knime.scripting.matlab.plots;

import de.mpicbg.knime.scripting.matlab.MatlabScriptingBundleActivator;
import de.mpicbg.knime.scripting.matlab.prefs.MatlabPreferenceInitializer;

import org.knime.core.node.*;


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
        String templateResources = MatlabScriptingBundleActivator
        		.getDefault()
        		.getPreferenceStore()
        		.getString(MatlabPreferenceInitializer.MATLAB_PLOT_TEMPLATE_RESOURCES);
        String defaultScript = MatlabPlotNodeFactory
        		.this
        		.createNodeModel()
        		.getDefaultScript();
        
        return new MatlabPlotNodeDialog(templateResources, defaultScript, true);
    }

}
