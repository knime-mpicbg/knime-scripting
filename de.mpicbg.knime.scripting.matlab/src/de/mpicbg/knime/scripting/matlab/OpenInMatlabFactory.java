package de.mpicbg.knime.scripting.matlab;

import de.mpicbg.knime.knutils.AbstractConfigDialog;

import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeFactory;
import org.knime.core.node.NodeView;
import org.knime.core.node.defaultnodesettings.DialogComponentStringSelection;
import java.util.ArrayList;


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
                // path to put the temporary data
                addDialogComponent(new DialogComponentStringSelection(
                		OpenInMatlab.createMatlabTypeSetting(), 
                		"Choose the MATLAB type",
                		matlabTypeOptions()));
            }
        };
    }

    
    /**
     * Create the options for the pop-up menu defining the MATLAB type in the configuration dialog
     * 
     * @return Options
     */
    public static ArrayList<String> matlabTypeOptions() {
        ArrayList<String> list = new ArrayList<String>();
        list.add("dataset");
        list.add("map");
        list.add("structure");
        return list;

    }

}
