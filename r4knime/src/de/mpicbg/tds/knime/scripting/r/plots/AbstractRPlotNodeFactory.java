package de.mpicbg.tds.knime.scripting.r.plots;

import org.knime.core.node.*;
import org.knime.core.node.defaultnodesettings.SettingsModelBoolean;
import org.knime.core.node.defaultnodesettings.SettingsModelInteger;
import org.knime.core.node.defaultnodesettings.SettingsModelIntegerBounded;
import org.knime.core.node.defaultnodesettings.SettingsModelString;


/**
 * @author Holger Brandl
 */
public abstract class AbstractRPlotNodeFactory<RPlotModel extends AbstractRPlotNodeModel> extends NodeFactory<RPlotModel> {

    @Override
    public abstract RPlotModel createNodeModel();


    @Override
    public int getNrNodeViews() {
        return 1;
    }


    @Override
    public NodeView<RPlotModel> createNodeView(final int viewIndex, final RPlotModel nodeModel) {
        return new RPlotNodeView<RPlotModel>(nodeModel);
    }


    @Override
    public boolean hasDialog() {
        return true;
    }


    @Override
    public NodeDialogPane createNodeDialogPane() {
        //String templateResources = R4KnimeBundleActivator.getDefault().getPreferenceStore().getString(RPreferenceInitializer.R_PLOT_TEMPLATES);
        return new RPlotNodeDialog(AbstractRPlotNodeFactory.this.createNodeModel().getDefaultScript(), false, enableTemplateRepository());
        //return new RPlotNodeDialog(templateResources, AbstractRPlotNodeFactory.this.createNodeModel().getDefaultScript(), enableTemplateRepository());
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
