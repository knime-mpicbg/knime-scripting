package de.mpicbg.knime.scripting.r.generic;

import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeFactory;
import org.knime.core.node.NodeView;


/**
 * <code>NodeFactory</code> for the "RSnippet" Node. Improved R Integration for Knime
 *
 * @author Holger Brandl (MPI-CBG)
 */
public class ConvertToTableFactory extends NodeFactory<ConvertToTable> {

    @Override
    public ConvertToTable createNodeModel() {
        return new ConvertToTable();
    }


    @Override
    public int getNrNodeViews() {
        return 0;
    }


    @Override
    public NodeView<ConvertToTable> createNodeView(final int viewIndex, final ConvertToTable nodeModel) {
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


