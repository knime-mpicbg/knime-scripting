package de.mpicbg.knime.scripting.python.v2.node.snippet22;

import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeFactory;
import org.knime.core.node.NodeView;

import de.mpicbg.knime.scripting.python.v2.AbstractPythonScriptingV2NodeModel;

/**
 * <code>NodeFactory</code> for the "PythonSnippet" Node.
 *
 * @author Antje Janosch (MPI-CBG)
 */
public class PythonSnippet22V2NodeFactory extends NodeFactory<PythonSnippet22V2NodeModel> {

	/**
	 * {@inheritDoc}
	 */
    @Override
    public PythonSnippet22V2NodeModel createNodeModel() {
        return new PythonSnippet22V2NodeModel();
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
    public NodeView<PythonSnippet22V2NodeModel> createNodeView(final int viewIndex,
                                                      final PythonSnippet22V2NodeModel nodeModel) {
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
        return new PythonSnippet22V2NodeDialog(
        		//"pyOut = kIn.copy()", 
        		PythonSnippet22V2NodeModel.DFT_SCRIPT,
        		PythonSnippet22V2NodeModel.nodeModelCfg);
    }
}


