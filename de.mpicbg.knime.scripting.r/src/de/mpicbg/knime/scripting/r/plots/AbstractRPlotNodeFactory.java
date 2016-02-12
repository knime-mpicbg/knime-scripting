package de.mpicbg.knime.scripting.r.plots;

import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeFactory;
import org.knime.core.node.NodeView;

import de.mpicbg.knime.scripting.r.node.plot.RPlotNodeDialog;
import de.mpicbg.knime.scripting.r.node.plot.RPlotNodeView;


/**
 * @author Holger Brandl
 */
public abstract class AbstractRPlotNodeFactory extends NodeFactory<AbstractRPlotNodeModel> {

    @Override
    public abstract AbstractRPlotNodeModel createNodeModel();


    @Override
    public int getNrNodeViews() {
        return 1;
    }


    @Override
    public NodeView<AbstractRPlotNodeModel> createNodeView(final int viewIndex, final AbstractRPlotNodeModel nodeModel) {
        return new RPlotNodeView<AbstractRPlotNodeModel>(nodeModel);
    }


    @Override
    public boolean hasDialog() {
        return true;
    }


    @Override
    public NodeDialogPane createNodeDialogPane() {
        return new RPlotNodeDialog(AbstractRPlotNodeFactory.this.createNodeModel().getDefaultScript(""), true);
    }

}
