package de.mpicbg.knime.scripting.matlab.open;

import de.mpicbg.knime.knutils.AbstractConfigDialog;

import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeFactory;
import org.knime.core.node.NodeView;


/**
 * @author Felix Meyenhofer
 */
public class OpenInMatlabFactory extends NodeFactory<OpenInMatlab> {

    /**
     * {@inheritDoc}
     */
    @Override
    public OpenInMatlab createNodeModel() {
        return new OpenInMatlab();
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
    public NodeView<OpenInMatlab> createNodeView(final int viewIndex,
                                                 final OpenInMatlab nodeModel) {
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
