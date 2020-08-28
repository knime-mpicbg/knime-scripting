package de.mpicbg.knime.scripting.python.v2.node.openinpython;

import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeFactory;
import org.knime.core.node.NodeView;

/**
 * Node Factory for 'Open in Python' node
 * 
 * @author Antje Janosch
 *
 */
public class OpenInPythonV2NodeFactory extends NodeFactory<OpenInPythonV2NodeModel> {

	@Override
	public OpenInPythonV2NodeModel createNodeModel() {
		return new OpenInPythonV2NodeModel();
	}

	@Override
	protected int getNrNodeViews() {
		return 0;
	}

	@Override
	public NodeView<OpenInPythonV2NodeModel> createNodeView(int viewIndex, OpenInPythonV2NodeModel nodeModel) {
		return null;
	}

	@Override
	protected boolean hasDialog() {
		return false;
	}

	@Override
	protected NodeDialogPane createNodeDialogPane() {
		return null;
	}

}
