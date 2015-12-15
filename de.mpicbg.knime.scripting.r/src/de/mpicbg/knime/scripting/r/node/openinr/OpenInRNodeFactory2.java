package de.mpicbg.knime.scripting.r.node.openinr;

import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeFactory;
import org.knime.core.node.NodeView;

import de.mpicbg.knime.scripting.core.ScriptingNodeDialog;
import de.mpicbg.knime.scripting.core.ScriptingOpenInDialog;


/**
 * <code>NodeFactory</code> for the "RSnippet" Node. Improved R Integration for Knime
 *
 * @author Holger Brandl (MPI-CBG)
 */
public class OpenInRNodeFactory2
        extends NodeFactory<OpenInRNodeModel2> {

    /**
     * {@inheritDoc}
     */
    @Override
    public OpenInRNodeModel2 createNodeModel() {
        return new OpenInRNodeModel2();
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
    public NodeView<OpenInRNodeModel2> createNodeView(final int viewIndex,
                                                     final OpenInRNodeModel2 nodeModel) {
        throw null;
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public boolean hasDialog() {
        return true;
    }


	@Override
	protected NodeDialogPane createNodeDialogPane() {
		return new ScriptingOpenInDialog();
	}



}