package de.mpicbg.knime.scripting.python;

import de.mpicbg.knime.scripting.core.ScriptingNodeDialog;
import de.mpicbg.knime.scripting.python.prefs.PythonPreferenceInitializer;

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
    public PythonSnippetNodeDialog(String defaultScript, boolean enableTemplateRepository) {
        super(defaultScript, new PythonColumnSupport(), enableTemplateRepository);
    }

    @Override
    public String getTemplatesFromPreferences() {
        return PythonScriptingBundleActivator.getDefault().getPreferenceStore().getString(PythonPreferenceInitializer.PYTHON_TEMPLATE_RESOURCES);
    }
}
