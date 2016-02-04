package de.mpicbg.knime.scripting.r.node.generic.openinr;

import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeFactory;
import org.knime.core.node.NodeView;

import de.mpicbg.knime.scripting.core.ScriptingOpenInDialog;


/**
 * Factory: A generic R node which pushes an R workspace to R
 *
 * @author Holger Brandl, Antje Janosch (MPI-CBG)
 */
public class GenericOpenInRNodeFactory2 extends NodeFactory<GenericOpenInRNodeModel2> {

    /**
     * {@inheritDoc}
     */
    @Override
    public GenericOpenInRNodeModel2 createNodeModel() {
        return new GenericOpenInRNodeModel2();
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public int getNrNodeViews() {
        return 0;
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public NodeView<GenericOpenInRNodeModel2> createNodeView(final int viewIndex,
                                                            final GenericOpenInRNodeModel2 nodeModel) {
        throw null;
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public boolean hasDialog() {
        return false;
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public NodeDialogPane createNodeDialogPane() {
        return null;
    }

}