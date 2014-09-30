package de.mpicbg.knime.knutils;

import org.knime.core.node.NodeModel;
import org.knime.core.node.NodeView;

import java.awt.*;


/**
 * Document me!
 *
 * @author Holger Brandl
 */
public abstract class AbstractNodeView<I extends NodeModel> extends NodeView<I> {

    protected AbstractNodeView(final I nodeModel) {
        super(nodeModel);

        Component viewComponent = createViewComponent();

        if (viewComponent != null) {
            setComponent(viewComponent);
        }
    }


    /**
     * Creates the view component of the view
     */
    protected abstract Component createViewComponent();


    @Override
    protected void onClose() {

        // TODO things to do when closing the view
    }


    @Override
    protected void onOpen() {

        // TODO things to do when opening the view
    }
}