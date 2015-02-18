package de.mpicbg.knime.scripting.r;

import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeFactory;
import org.knime.core.node.NodeView;


/**
 * <code>NodeFactory</code> for the "RSnippet" Node. Improved R Integration for Knime
 *
 * @author Holger Brandl (MPI-CBG)
 */
public class OpenInRNodeFactory
        extends NodeFactory<OpenInRNodeModel> {

    /**
     * {@inheritDoc}
     */
    @Override
    public OpenInRNodeModel createNodeModel() {
        return new OpenInRNodeModel();
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public int getNrNodeViews() {
        return 0;
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public NodeView<OpenInRNodeModel> createNodeView(final int viewIndex,
                                                     final OpenInRNodeModel nodeModel) {
        throw null;
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public boolean hasDialog() {
        return true;
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public NodeDialogPane createNodeDialogPane() {
        //String templateResources = R4KnimeBundleActivator.getDefault().getPreferenceStore().getString(RPreferenceInitializer.R_OPENINR_TEMPLATES);
        return new RSnippetNodeDialog("", true);
    }

}