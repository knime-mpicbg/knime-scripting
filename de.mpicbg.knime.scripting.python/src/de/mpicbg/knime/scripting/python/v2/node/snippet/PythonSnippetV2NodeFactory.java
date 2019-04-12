package de.mpicbg.knime.scripting.python.v2.node.snippet;

import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeFactory;
import org.knime.core.node.NodeView;

import de.mpicbg.knime.scripting.python.v2.AbstractPythonScriptingV2NodeModel;

/**
 * <code>NodeFactory</code> for the "PythonSnippet" Node.
 *
 * @author Antje Janosch (MPI-CBG)
 */
public class PythonSnippetV2NodeFactory extends NodeFactory<PythonSnippetV2NodeModel> {

	/**
	 * {@inheritDoc}
	 */
    @Override
    public PythonSnippetV2NodeModel createNodeModel() {
        return new PythonSnippetV2NodeModel();
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
    public NodeView<PythonSnippetV2NodeModel> createNodeView(final int viewIndex,
                                                      final PythonSnippetV2NodeModel nodeModel) {
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
        return new PythonSnippetV2NodeDialog(
        		AbstractPythonScriptingV2NodeModel.CFG_SCRIPT_DFT, 
        		PythonSnippetV2NodeModel.nodeModelCfg);
    }
}


