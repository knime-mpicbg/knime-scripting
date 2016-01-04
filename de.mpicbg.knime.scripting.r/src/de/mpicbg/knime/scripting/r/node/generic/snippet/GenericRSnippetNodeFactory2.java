package de.mpicbg.knime.scripting.r.node.generic.snippet;

import de.mpicbg.knime.scripting.r.RSnippetNodeDialog;
import de.mpicbg.knime.scripting.r.RUtils;

import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeFactory;
import org.knime.core.node.NodeView;


/**
 * <code>NodeFactory</code> for the "RSnippet" Node. Improved R Integration for Knime
 *
 * @author Holger Brandl (MPI-CBG)
 */
public class GenericRSnippetNodeFactory2
        extends NodeFactory<GenericRSnippetNodeModel2> {

    @Override
    public GenericRSnippetNodeModel2 createNodeModel() {
        return new GenericRSnippetNodeModel2();
    }


    @Override
    public int getNrNodeViews() {
        return 0;
    }


    @Override
    public NodeView<GenericRSnippetNodeModel2> createNodeView(final int viewIndex,
                                                    final GenericRSnippetNodeModel2 nodeModel) {
        return null;
    }


    @Override
    public boolean hasDialog() {
        return true;
    }


    @Override
    public NodeDialogPane createNodeDialogPane() {
        return new RSnippetNodeDialog(RUtils.SCRIPT_PROPERTY_DEFAULT, false);
    }

}


