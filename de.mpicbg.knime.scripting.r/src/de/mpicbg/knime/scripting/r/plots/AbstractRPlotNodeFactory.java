package de.mpicbg.knime.scripting.r.plots;

import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeFactory;
import org.knime.core.node.NodeView;


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

}
