package de.mpicbg.knime.scripting.python.open;

//import de.mpicbg.tds.knime.scripting.r.prefs.RPreferenceInitializer;

import de.mpicbg.knime.knutils.AbstractConfigDialog;

import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeFactory;
import org.knime.core.node.NodeView;
import org.knime.core.node.defaultnodesettings.SettingsModelBoolean;

/**
 * @author Felix Meyenhofer (MPI-CBG)
 */

public class OpenInPythonFactory extends NodeFactory<OpenInPythonNodeModel> {

    /**
     * {@inheritDoc}
     */
    @Override
    public OpenInPythonNodeModel createNodeModel() {
        return new OpenInPythonNodeModel();
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
    public NodeView<OpenInPythonNodeModel> createNodeView(final int viewIndex,
                                                          final OpenInPythonNodeModel nodeModel) {
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

        return new AbstractConfigDialog() {

            @Override
            protected void createControls() {
            }
        };
    }

    public static SettingsModelBoolean executionModeSetting() {
        return new SettingsModelBoolean("new.instance", false);
    }
}
