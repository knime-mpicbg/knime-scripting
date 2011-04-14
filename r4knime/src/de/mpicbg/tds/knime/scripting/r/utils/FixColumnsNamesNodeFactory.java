package de.mpicbg.tds.knime.scripting.r.utils;

import de.mpicbg.tds.knime.knutils.AbstractConfigDialog;
import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeFactory;
import org.knime.core.node.NodeView;
import org.knime.core.node.defaultnodesettings.DialogComponentBoolean;
import org.knime.core.node.defaultnodesettings.SettingsModelBoolean;


/**
 * <code>NodeFactory</code> for the "FixColumnsNamesNodeModel" Node. Improved R Integration for Knime
 *
 * @author Holger Brandl (MPI-CBG)
 */
public class FixColumnsNamesNodeFactory extends NodeFactory<FixColumnsNamesNodeModel> {

    /**
     * {@inheritDoc}
     */
    @Override
    public FixColumnsNamesNodeModel createNodeModel() {
        return new FixColumnsNamesNodeModel();
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
    public NodeView<FixColumnsNamesNodeModel> createNodeView(final int viewIndex, final FixColumnsNamesNodeModel nodeModel) {
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
                addDialogComponent(new DialogComponentBoolean(createPropStrictRNames(), "Use strict name matching"));
            }
        };
    }


    public static SettingsModelBoolean createPropStrictRNames() {
        return new SettingsModelBoolean("use.strict.names", false);
    }


}