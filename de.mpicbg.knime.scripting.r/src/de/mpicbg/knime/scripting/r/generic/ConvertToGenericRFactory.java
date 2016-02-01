package de.mpicbg.knime.scripting.r.generic;

import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeFactory;
import org.knime.core.node.NodeView;


/**
 * <code>NodeFactory</code> for the "RSnippet" Node. Improved R Integration for Knime
 *
 * @author Holger Brandl (MPI-CBG)
 */
public class ConvertToGenericRFactory extends NodeFactory<ConvertToGenericR> {

    @Override
    public ConvertToGenericR createNodeModel() {
        return new ConvertToGenericR();
    }


    @Override
    public int getNrNodeViews() {
        return 0;
    }


    @Override
    public NodeView<ConvertToGenericR> createNodeView(final int viewIndex, final ConvertToGenericR nodeModel) {
        return null;
    }


    @Override
    public boolean hasDialog() {
        return false;
    }


    @Override
    public NodeDialogPane createNodeDialogPane() {
        return null;
    }

}


