package de.mpicbg.knime.scripting.r.node.generic.snippet;

import de.mpicbg.knime.scripting.r.RSnippetNodeDialog;
import de.mpicbg.knime.scripting.r.RUtils;

import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeFactory;
import org.knime.core.node.NodeView;


/**
 * Factory: A generic R node which modifies an R workspace
 *
 * @author Holger Brandl, Antje Janosch (MPI-CBG)
 */
public class GenericRSnippetNodeFactory2
        extends NodeFactory<GenericRSnippetNodeModel2> {

	/**
	 * {@inheritDoc}
	 */
    @Override
    public GenericRSnippetNodeModel2 createNodeModel() {
        return new GenericRSnippetNodeModel2();
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
    public NodeView<GenericRSnippetNodeModel2> createNodeView(final int viewIndex,
                                                    final GenericRSnippetNodeModel2 nodeModel) {
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
        return new RSnippetNodeDialog(GenericRSnippetNodeModel2.GENERIC_SNIPPET_DFT, false, true, false);
    }

}


