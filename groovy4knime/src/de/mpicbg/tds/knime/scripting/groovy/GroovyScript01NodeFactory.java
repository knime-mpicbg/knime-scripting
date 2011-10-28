/* @(#)$RCSfile$
 * $Revision$ $Date$ $Author$
 *
 */
package de.mpicbg.tds.knime.scripting.groovy;

import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeFactory;
import org.knime.core.node.NodeModel;
import org.knime.core.node.NodeView;


/**
 * Note: Has been replaced with GroovyScriptingNodeFactory
 *
 * @author Holger Brandl
 */
@Deprecated
public class GroovyScript01NodeFactory extends NodeFactory {

    /**
     * {@inheritDoc}
     */
    public NodeModel createNodeModel() {
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
    public NodeView createNodeView(final int viewIndex,
                                   final NodeModel nodeModel) {
        return null;
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
        //String templateResources = GroovyScriptingBundleActivator.getDefault().getPreferenceStore().getString(GroovyScriptingPreferenceInitializer.GROOVY_TEMPLATE_RESOURCES);
        return new GroovyScriptNodeDialog(GroovyScriptNodeModel.DEFAULT_SCRIPT, true, true);
    }

}