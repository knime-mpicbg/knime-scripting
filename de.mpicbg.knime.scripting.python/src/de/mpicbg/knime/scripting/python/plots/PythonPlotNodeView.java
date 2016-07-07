package de.mpicbg.knime.scripting.python.plots;

import org.knime.core.node.NodeView;


/**
 * <code>NodeView</code> for the "RSnippet" Node. Improved R Integration for Knime
 *
 * @author Holger Brandl (MPI-CBG)
 */
public class PythonPlotNodeView extends NodeView<PythonPlotNodeModel> {

    /**
     * Creates a new view.
     */
    public PythonPlotNodeView(final PythonPlotNodeModel nodeModel) {
        super(nodeModel);
        updateView(nodeModel);
    }

    private void updateView(PythonPlotNodeModel nodeModel) {
        PythonPlotCanvas canvas = new PythonPlotCanvas(nodeModel);
        setComponent(canvas);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void modelChanged() {
        //  retrieve the new model from your nodemodel and
        // update the view.
        PythonPlotNodeModel nodeModel = getNodeModel();
        assert nodeModel != null;

        // be aware of a possibly not executed nodeModel! The data you retrieve
        // from your nodemodel could be null, emtpy, or invalid in any kind.
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
