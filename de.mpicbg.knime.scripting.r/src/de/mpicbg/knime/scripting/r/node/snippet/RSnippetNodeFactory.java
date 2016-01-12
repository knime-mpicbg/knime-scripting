package de.mpicbg.knime.scripting.r.node.snippet;

import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeFactory;
import org.knime.core.node.NodeView;

import de.mpicbg.knime.scripting.r.AbstractRScriptingNodeModel;

/**
 * <code>NodeFactory</code> for the "RSnippet" Node.
 *
 * @author Holger Brandl, Antje Janosch (MPI-CBG)
 */
public class RSnippetNodeFactory extends NodeFactory<RSnippetNodeModel> {

	/**
	 * {@inheritDoc}
	 */
    @Override
    public RSnippetNodeModel createNodeModel() {
        return new RSnippetNodeModel(1, 1);
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
    public NodeView<RSnippetNodeModel> createNodeView(final int viewIndex,
                                                      final RSnippetNodeModel nodeModel) {
        return null;
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
        return new RSnippetNodeDialog(AbstractRScriptingNodeModel.CFG_SCRIPT_DFT, true);
    }
}


