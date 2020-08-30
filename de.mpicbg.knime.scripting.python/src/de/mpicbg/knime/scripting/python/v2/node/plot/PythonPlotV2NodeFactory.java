package de.mpicbg.knime.scripting.python.v2.node.plot;

import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeFactory;
import org.knime.core.node.NodeView;


/**
 * 
 * Node Factory class for 'Python Plot' node
 * 
 * @author Antje Janosch
 *
 */
public class PythonPlotV2NodeFactory extends NodeFactory<PythonPlotV2NodeModel> {

    @Override
    public PythonPlotV2NodeModel createNodeModel() {
        return new PythonPlotV2NodeModel();
    }


    @Override
    public int getNrNodeViews() {
        return 1;
    }


	@Override
    public NodeView<PythonPlotV2NodeModel> createNodeView(final int viewIndex, final PythonPlotV2NodeModel nodeModel) {
        return new PythonPlotV2NodeView<PythonPlotV2NodeModel>(nodeModel);
    }


    @Override
    public boolean hasDialog() {
        return true;
    }

    @Override
    public NodeDialogPane createNodeDialogPane() {
        return new PythonPlotV2NodeDialog(PythonPlotV2NodeFactory.this.createNodeModel().getDefaultScript(""), true);
    }
}
