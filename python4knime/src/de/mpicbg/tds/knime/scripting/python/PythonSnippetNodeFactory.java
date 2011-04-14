package de.mpicbg.tds.knime.scripting.python;

import de.mpicbg.tds.knime.knutils.scripting.ScriptingNodeDialog;
import de.mpicbg.tds.knime.scripting.python.prefs.PythonPreferenceInitializer;
import org.eclipse.jface.preference.IPreferenceStore;
import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeFactory;
import org.knime.core.node.NodeView;


/**
 * <code>NodeFactory</code> for the "PythonSnippet" Node. Python integration for Knime.
 *
 * @author Tom Haux (MPI-CBG)
 */
public class PythonSnippetNodeFactory
        extends NodeFactory<PythonSnippetNodeModel> {

    @Override
    public PythonSnippetNodeModel createNodeModel() {
        return new PythonSnippetNodeModel();
    }


    @Override
    public int getNrNodeViews() {
        return 0;
    }


    @Override
    public NodeView<PythonSnippetNodeModel> createNodeView(final int viewIndex,
                                                           final PythonSnippetNodeModel nodeModel) {
        return null;
    }


    @Override
    public boolean hasDialog() {
        return true;
    }


    @Override
    public NodeDialogPane createNodeDialogPane() {
        IPreferenceStore prefStore = PythonScriptingBundleActivator.getDefault().getPreferenceStore();
        String templateResources = prefStore.getString(PythonPreferenceInitializer.PYTHON_TEMPLATE_RESOURCES);

        return new ScriptingNodeDialog(PythonSnippetNodeModel.DEFAULT_SCRIPT, new PythonColReformatter(), templateResources, true, true);
    }
}

