package de.mpicbg.knime.scripting.matlab;

//import de.mpicbg.tds.knime.scripting.r.prefs.RPreferenceInitializer;

import de.mpicbg.knime.knutils.AbstractConfigDialog;

import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeFactory;
import org.knime.core.node.NodeView;
import org.knime.core.node.defaultnodesettings.DialogComponentBoolean;
import org.knime.core.node.defaultnodesettings.DialogComponentStringSelection;
import org.knime.core.node.defaultnodesettings.SettingsModelBoolean;
import org.knime.core.node.defaultnodesettings.SettingsModelString;

import java.util.ArrayList;

/**
 * @author Felix Meyenhofer (MPI-CBG)
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
//                // path to put the temporary data
                DialogComponentStringSelection matlabType = new DialogComponentStringSelection(matlabTypeSetting(), "Choose the MATLAB type", matlabTypeOptions());
                addDialogComponent(matlabType);
                // checkbox to run a new instance or not.
                DialogComponentBoolean newMatlabInstance = new DialogComponentBoolean(executionModeSetting(), "Run in a new MATLAB instance");
                addDialogComponent(newMatlabInstance);
            }
        };
    }


    public static ArrayList matlabTypeOptions() {
        ArrayList<String> list = new ArrayList<String>();
        list.add("dataset");
        list.add("map");
        list.add("structure");
        return list;

    }

    public static SettingsModelString matlabTypeSetting() {
        return new SettingsModelString("matlabType", "dataset");
    }


    public static SettingsModelBoolean executionModeSetting() {
        return new SettingsModelBoolean("new.instance", true);
    }

}