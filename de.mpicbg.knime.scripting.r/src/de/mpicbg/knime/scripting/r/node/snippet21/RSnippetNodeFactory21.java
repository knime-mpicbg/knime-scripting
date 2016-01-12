package de.mpicbg.knime.scripting.r.node.snippet21;

import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeFactory;
import org.knime.core.node.NodeView;

import de.mpicbg.knime.scripting.r.AbstractRScriptingNodeModel;
import de.mpicbg.knime.scripting.r.RUtils;
import de.mpicbg.knime.scripting.r.node.snippet.RSnippetNodeDialog;
import de.mpicbg.knime.scripting.r.node.snippet.RSnippetNodeModel;

/**
 * <code>NodeFactory</code> for the "RSnippet (2:1)" Node.
 *
 * @author Holger Brandl, Antje Janosch (MPI-CBG)
 */
public class RSnippetNodeFactory21 extends NodeFactory<RSnippetNodeModel> {

	/**
	 * {@inheritDoc}
	 */
    @Override
    public RSnippetNodeModel createNodeModel() {
        return new RSnippetNodeModel(2, 1);
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
        return new RSnippetNodeDialog(AbstractRScriptingNodeModel.CFG_SCRIPT2_DFT, true);
    }
}
