package de.mpicbg.tds.knime.scripting.python;

import de.mpicbg.tds.knime.knutils.scripting.ScriptingNodeDialog;
import de.mpicbg.tds.knime.scripting.python.prefs.PythonPreferenceInitializer;

/**
 * Created by IntelliJ IDEA.
 * User: Antje Niederlein
 * Date: 10/27/11
 * Time: 1:59 PM
 */
public class PythonSnippetNodeDialog extends ScriptingNodeDialog {

    /**
     * New pane for configuring ScriptedNode node dialog
     */
    public PythonSnippetNodeDialog(String defaultScript, boolean hasOutput, boolean enableTemplateRepository) {
        super(defaultScript, new PythonColReformatter(), hasOutput, enableTemplateRepository);
    }

    @Override
    public String getTemplatesFromPreferences() {
        return PythonScriptingBundleActivator.getDefault().getPreferenceStore().getString(PythonPreferenceInitializer.PYTHON_TEMPLATE_RESOURCES);
    }
}
