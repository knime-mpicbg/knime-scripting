package de.mpicbg.knime.scripting.matlab.plots;

import org.knime.core.node.NodeView;


/**
 * <code>NodeView</code> for the "RSnippet" Node. Improved R Integration for KNIME
 *
 * @author Holger Brandl (MPI-CBG)
 */
public class MatlabPlotNodeView extends NodeView<MatlabPlotNodeModel> {

    /**
     * Creates a new view.
     */
    public MatlabPlotNodeView(final MatlabPlotNodeModel nodeModel) {
        super(nodeModel);
        
        updateView(nodeModel);
    }

    /**
     * Create or update the Node view
     * 
     * @param nodeModel
     */
    private void updateView(MatlabPlotNodeModel nodeModel) {
        if (nodeModel.getImage() == null) return;
        MatlabPlotCanvas canvas = new MatlabPlotCanvas(nodeModel);
        setComponent(canvas);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void modelChanged() {

        //  retrieve the new model from your node model and
        // update the view.
        MatlabPlotNodeModel nodeModel = getNodeModel();
        assert nodeModel != null;

        // be aware of a possibly not executed nodeModel! The data you retrieve
        // from your node model could be null, empty, or invalid in any kind.

        updateView(getNodeModel());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void onClose() {
        //  things to do when closing the view
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void onOpen() {
        //  things to do when opening the view
    }
}
