package de.mpicbg.knime.scripting.r.node.generic.converttogeneric;

import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeFactory;
import org.knime.core.node.NodeView;

import de.mpicbg.knime.scripting.core.ScriptingOpenInDialog;


/**
 * <code>NodeFactory</code> for the "RSnippet" Node. Improved R Integration for Knime
 *
 * @author Holger Brandl (MPI-CBG)
 */
public class ConvertToGenericRFactory extends NodeFactory<ConvertToGenericRModel> {

    @Override
    public ConvertToGenericRModel createNodeModel() {
        return new ConvertToGenericRModel();
    }


    @Override
    public int getNrNodeViews() {
        return 0;
    }


    @Override
    public NodeView<ConvertToGenericRModel> createNodeView(final int viewIndex, final ConvertToGenericRModel nodeModel) {
        return null;
    }


    @Override
    public boolean hasDialog() {
        return true;
    }


    @Override
    public NodeDialogPane createNodeDialogPane() {
    	return new ScriptingOpenInDialog();
    }

}


