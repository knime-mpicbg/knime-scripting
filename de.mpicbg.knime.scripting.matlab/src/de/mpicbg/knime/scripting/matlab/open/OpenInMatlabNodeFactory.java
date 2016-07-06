package de.mpicbg.knime.scripting.matlab.open;

import de.mpicbg.knime.knutils.AbstractConfigDialog;

import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeFactory;
import org.knime.core.node.NodeView;


/**
 * @author Felix Meyenhofer
 */
public class OpenInMatlabNodeFactory extends NodeFactory<OpenInMatlabNodeModel> {

    /**
     * {@inheritDoc}
     */
    @Override
    public OpenInMatlabNodeModel createNodeModel() {
        return new OpenInMatlabNodeModel();
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
    public NodeView<OpenInMatlabNodeModel> createNodeView(final int viewIndex,
                                                 final OpenInMatlabNodeModel nodeModel) {
        throw null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean hasDialog() {
        return true;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public NodeDialogPane createNodeDialogPane() {

        return new AbstractConfigDialog() {
            @Override
            protected void createControls() {
            	removeTab("Options");
            }
        };
    }
}
