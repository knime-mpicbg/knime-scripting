package de.mpicbg.knime.scripting.r;

import org.knime.core.node.NodeView;


/**
 * <code>NodeView</code> for the "RSnippet" Node. Improved R Integration for Knime
 *
 * @author Holger Brandl (MPI-CBG)
 */
public class RSnippetNodeView extends NodeView<RSnippetNodeModel> {

    /**
     * Creates a new view.
     *
     * @param nodeModel The model (class: {@link RSnippetNodeModel})
     */
    protected RSnippetNodeView(final RSnippetNodeModel nodeModel) {
        super(nodeModel);

        // TODO instantiate the components of the view here.

    }


    /**
     * {@inheritDoc}
     */
    @Override
    protected void modelChanged() {

        // TODO retrieve the new model from your nodemodel and 
        // update the view.
        RSnippetNodeModel nodeModel =
                (RSnippetNodeModel) getNodeModel();
        assert nodeModel != null;

        // be aware of a possibly not executed nodeModel! The data you retrieve
        // from your nodemodel could be null, emtpy, or invalid in any kind.

    }


    /**
     * {@inheritDoc}
     */
    @Override
    protected void onClose() {

        // TODO things to do when closing the view
    }


    /**
     * {@inheritDoc}
     */
    @Override
    protected void onOpen() {

        // TODO things to do when opening the view
    }

}

