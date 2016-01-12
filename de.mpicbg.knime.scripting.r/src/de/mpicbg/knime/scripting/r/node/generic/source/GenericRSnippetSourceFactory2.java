package de.mpicbg.knime.scripting.r.node.generic.source;

import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeFactory;
import org.knime.core.node.NodeView;
import org.knime.core.node.port.PortType;

import de.mpicbg.knime.scripting.core.ScriptingModelConfig;
import de.mpicbg.knime.scripting.r.RColumnSupport;
import de.mpicbg.knime.scripting.r.node.generic.snippet.GenericRSnippetNodeModel2;
import de.mpicbg.knime.scripting.r.node.snippet.RSnippetNodeDialog;
import de.mpicbg.knime.scripting.r.port.RPortObject;


/**
 * Factory: A generic R node which creates an R workspace
 *
 * @author Holger Brandl, Antje Janosch (MPI-CBG)
 */
public class GenericRSnippetSourceFactory2
        extends NodeFactory<GenericRSnippetNodeModel2> {
	
	private static final ScriptingModelConfig nodeModelConfig = new ScriptingModelConfig(
			new PortType[0],
			GenericRSnippetNodeModel2.createPorts(1, RPortObject.TYPE, RPortObject.class),
			new RColumnSupport(),
			true,
			false,
			true);

	/**
	 * {@inheritDoc}
	 */
    @Override
    public GenericRSnippetNodeModel2 createNodeModel() {
        return new GenericRSnippetNodeModel2(nodeModelConfig);
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
    public boolean hasDialog() {
        return true;
    }

	/**
	 * {@inheritDoc}
	 */
    @Override
    public NodeDialogPane createNodeDialogPane() {
        return new RSnippetNodeDialog(GenericRSnippetNodeModel2.GENERIC_SNIPPET_DFT, false, false, false);
    }

	/**
	 * {@inheritDoc}
	 */
	@Override
	public NodeView<GenericRSnippetNodeModel2> createNodeView(int viewIndex, GenericRSnippetNodeModel2 nodeModel) {
		return null;
	}

}


