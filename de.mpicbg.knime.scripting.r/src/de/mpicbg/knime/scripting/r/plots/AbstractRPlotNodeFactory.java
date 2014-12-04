package de.mpicbg.knime.scripting.r.plots;

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
        return new RPlotNodeDialog(AbstractRPlotNodeFactory.this.createNodeModel().getDefaultScript(), true);
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
        return new SettingsModelString("figure.output.file", "");
    }

    public static SettingsModelString createPropOutputType() {
        return new SettingsModelString("figure.ouput.type", "png");
    }


	public static SettingsModelBoolean createEnableFile() {
		return new SettingsModelBoolean("write.output.file", true);
	}
}
