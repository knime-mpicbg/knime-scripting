package de.mpicbg.knime.scripting.matlab.snippet;

import de.mpicbg.knime.scripting.matlab.MatlabScriptingBundleActivator;
import de.mpicbg.knime.scripting.matlab.prefs.MatlabPreferenceInitializer;
import de.mpicbg.knime.scripting.matlab.srv.Matlab;

import org.eclipse.jface.preference.IPreferenceStore;
import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeFactory;
import org.knime.core.node.NodeView;


/**
 * <code>NodeFactory</code> for the "MatlabSnippet" Node. Matlab integration for Knime.
 *
 * @author Holger Brandl (MPI-CBG)
 */
public class MatlabSnippetNodeFactory
        extends NodeFactory<MatlabSnippetNodeModel> {

    @Override
    public MatlabSnippetNodeModel createNodeModel() {
        return new MatlabSnippetNodeModel();
    }


    @Override
    public int getNrNodeViews() {
        return 0;
    }


    @Override
    public NodeView<MatlabSnippetNodeModel> createNodeView(final int viewIndex,
                                                           final MatlabSnippetNodeModel nodeModel) {
        return null;
    }


    @Override
    public boolean hasDialog() {
        return true;
    }


    @Override
    public NodeDialogPane createNodeDialogPane() {
        IPreferenceStore prefStore = MatlabScriptingBundleActivator.getDefault().getPreferenceStore();
        String templateResources = prefStore.getString(MatlabPreferenceInitializer.MATLAB_TEMPLATE_RESOURCES);

        return new MatlabSnippetNodeDialog(Matlab.DEFAULT_SNIPPET, true, true);
    }
}

