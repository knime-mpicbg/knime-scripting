package de.mpicbg.knime.scripting.r.node.generic.converttotable;

import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeFactory;
import org.knime.core.node.NodeView;

import de.mpicbg.knime.scripting.r.node.generic.snippet.GenericRSnippetNodeModel2;
import de.mpicbg.knime.scripting.r.node.snippet.RSnippetNodeDialog;


/**
 * Factory: A generic R node which pulls a an R data frame and returns it as KNIME table
 *
 * @author Holger Brandl, Antje Janosch (MPI-CBG)
 */
public class ConvertToTableNodeFactory2 extends NodeFactory<ConvertToTableNodeModel2> {

    @Override
    public ConvertToTableNodeModel2 createNodeModel() {
        return new ConvertToTableNodeModel2();
    }


    @Override
    public int getNrNodeViews() {
        return 0;
    }


    @Override
    public NodeView<ConvertToTableNodeModel2> createNodeView(final int viewIndex, final ConvertToTableNodeModel2 nodeModel) {
        return null;
    }


    @Override
    public boolean hasDialog() {
        return true;
    }


    @Override
    public NodeDialogPane createNodeDialogPane() {
    	return new RSnippetNodeDialog(GenericRSnippetNodeModel2.GENERIC_SNIPPET_DFT, false, true, true);
    }

}


