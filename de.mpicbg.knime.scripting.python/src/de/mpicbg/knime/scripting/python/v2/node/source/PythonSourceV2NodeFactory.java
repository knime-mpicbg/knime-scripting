package de.mpicbg.knime.scripting.python.v2.node.source;

import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeFactory;
import org.knime.core.node.NodeView;

import de.mpicbg.knime.scripting.python.v2.node.snippet22.PythonSnippet22V2NodeDialog;

/**
 * <code>NodeFactory</code> for the "Python Source" Node.
 *
 * Antje Janosch (MPI-CBG)
 */
public class PythonSourceV2NodeFactory extends NodeFactory<PythonSourceV2NodeModel> {

	@Override
	public PythonSourceV2NodeModel createNodeModel() {
		// TODO Auto-generated method stub
		return new PythonSourceV2NodeModel();
	}

	@Override
	protected int getNrNodeViews() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public NodeView<PythonSourceV2NodeModel> createNodeView(int viewIndex, PythonSourceV2NodeModel nodeModel) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	protected boolean hasDialog() {
		// TODO Auto-generated method stub
		return true;
	}

	@Override
	protected NodeDialogPane createNodeDialogPane() {
		return new PythonSnippet22V2NodeDialog(
        		PythonSourceV2NodeModel.DFT_SCRIPT,
        		PythonSourceV2NodeModel.nodeModelCfg);
	}

}
