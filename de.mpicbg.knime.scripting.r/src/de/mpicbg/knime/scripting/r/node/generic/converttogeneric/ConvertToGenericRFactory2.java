package de.mpicbg.knime.scripting.r.node.generic.converttogeneric;

import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeFactory;
import org.knime.core.node.NodeView;

import de.mpicbg.knime.scripting.core.ScriptingOpenInDialog;


/**
 * Factory: A generic R node which pushes a KNIME table into an R workspace
 *
 * @author Holger Brandl (MPI-CBG)
 */
public class ConvertToGenericRFactory2 extends NodeFactory<ConvertToGenericRModel2> {

    @Override
    public ConvertToGenericRModel2 createNodeModel() {
        return new ConvertToGenericRModel2();
    }


    @Override
    public int getNrNodeViews() {
        return 0;
    }


    @Override
    public NodeView<ConvertToGenericRModel2> createNodeView(final int viewIndex, final ConvertToGenericRModel2 nodeModel) {
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


