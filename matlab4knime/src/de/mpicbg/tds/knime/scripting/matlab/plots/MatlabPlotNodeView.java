package de.mpicbg.tds.knime.scripting.matlab.plots;

import org.knime.core.node.NodeView;


/**
 * <code>NodeView</code> for the "RSnippet" Node. Improved R Integration for Knime
 *
 * @author Holger Brandl (MPI-CBG)
 */
public class MatlabPlotNodeView extends NodeView<MatlabPlotNodeModel> {

    /**
     * Creates a new view.
     */
    public MatlabPlotNodeView(final MatlabPlotNodeModel nodeModel) {
        super(nodeModel);

        //  instantiate the components of the view here.

//        JPanel jPanel = new JPanel(new BorderLayout());
//        jPanel.add(new JLabel("blabla"));


        updateView(nodeModel);

    }


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

        //  retrieve the new model from your nodemodel and
        // update the view.
        MatlabPlotNodeModel nodeModel = getNodeModel();
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