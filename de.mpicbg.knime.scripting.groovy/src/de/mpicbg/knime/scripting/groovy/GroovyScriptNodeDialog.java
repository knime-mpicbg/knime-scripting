package de.mpicbg.knime.scripting.groovy;

import de.mpicbg.knime.scripting.core.ScriptingNodeDialog;
import de.mpicbg.knime.scripting.groovy.prefs.GroovyScriptingPreferenceInitializer;

/**
 * Created by IntelliJ IDEA.
 * User: Antje Niederlein
 * Date: 10/27/11
 * Time: 2:22 PM
 */
public class GroovyScriptNodeDialog extends ScriptingNodeDialog {
    public GroovyScriptNodeDialog(String defaultScript, boolean useTemplateRepository) {
        super(defaultScript, new GroovyColReformatter(), useTemplateRepository);
    }

    @Override
    public String getTemplatesFromPreferences() {
        return GroovyScriptingBundleActivator.getDefault().getPreferenceStore().getString(GroovyScriptingPreferenceInitializer.GROOVY_TEMPLATE_RESOURCES);
    }
}
