package de.mpicbg.knime.scripting.r;

import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeFactory;
import org.knime.core.node.NodeView;

import de.mpicbg.knime.scripting.r.node.snippet.RSnippetNodeDialog;
import de.mpicbg.knime.scripting.r.node.snippet.RSnippetNodeModel;

/**
 * <code>NodeFactory</code> for the "RSnippet" Node. Improved R Integration for Knime
 *
 * @author Holger Brandl (MPI-CBG)
 */
public class RSnippetNodeFactory21 extends NodeFactory<RSnippetNodeModel> {

    @Override
    public RSnippetNodeModel createNodeModel() {
        return new RSnippetNodeModel(2, 1);
    }
    
    @Override
    public int getNrNodeViews() {
        return 0;
    }


    @Override
    public NodeView<RSnippetNodeModel> createNodeView(final int viewIndex,
                                                      final RSnippetNodeModel nodeModel) {
//        return new RSnippetNodeView(nodeModel);
        return null;
    }


    @Override
    public boolean hasDialog() {
        return true;
    }


    @Override
    public NodeDialogPane createNodeDialogPane() {
        return new RSnippetNodeDialog(RUtils.SCRIPT_PROPERTY_DEFAULT, true);
    }
}
