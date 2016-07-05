/* @(#)$RCSfile$
 * $Revision$ $Date$ $Author$
 *
 */
package de.mpicbg.knime.scripting.groovy;

import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeFactory;
import org.knime.core.node.NodeView;


/**
 * <code>NodeFactory</code> for the "JPython Script 1:1" Node. Scripting Engine
 *
 * @author Tripos
 */
public class GroovyScriptNodeFactory extends NodeFactory<GroovyScriptNodeModel> {

    /**
     * {@inheritDoc}
     */
    public GroovyScriptNodeModel createNodeModel() {
        return new GroovyScriptNodeModel();
    }


    /**
     * {@inheritDoc}
     */
    public int getNrNodeViews() {
        return 0;
    }


    /**
     * {@inheritDoc}
     */
    public boolean hasDialog() {
        return true;
    }


    /**
     * {@inheritDoc}
     */
    public NodeDialogPane createNodeDialogPane() {
        return new GroovyScriptNodeDialog(GroovyScriptNodeModel.DEFAULT_SCRIPT, true);
    }


	@Override
	public NodeView<GroovyScriptNodeModel> createNodeView(int viewIndex, GroovyScriptNodeModel nodeModel) {
		// TODO Auto-generated method stub
		return null;
	}

}