package de.mpicbg.tds.knime.scripting.r.plots;

import org.knime.core.node.NodeView;


/**
 * <code>NodeView</code> for the "RSnippet" Node. Improved R Integration for Knime
 *
 * @author Holger Brandl (MPI-CBG)
 */
public class RPlotNodeView<RPlotModel extends AbstractRPlotNodeModel> extends NodeView<RPlotModel> {

    /**
     * Creates a new view.
     *
     * @param nodeModel The model (class: {@link de.mpicbg.tds.knime.scripting.r.RSnippetNodeModel})
     */
    public RPlotNodeView(final RPlotModel nodeModel) {
        super(nodeModel);

        //  instantiate the components of the view here.

//        JPanel jPanel = new JPanel(new BorderLayout());
//        jPanel.add(new JLabel("blabla"));


        updateView(nodeModel);

    }


    private void updateView(RPlotModel nodeModel) {
        if (nodeModel.getWSFile() == null) {
            nodeModel.setPlotWarning();
            return;
        }

        RPlotCanvas rPlotCanvas = new RPlotCanvas(nodeModel);
        setComponent(rPlotCanvas);
    }


    /**
     * {@inheritDoc}
     */
    @Override
    protected void modelChanged() {

        //  retrieve the new model from your nodemodel and
        // update the view.
        RPlotModel nodeModel = getNodeModel();
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