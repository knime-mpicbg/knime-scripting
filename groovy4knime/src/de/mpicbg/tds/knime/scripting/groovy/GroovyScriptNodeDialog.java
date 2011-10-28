package de.mpicbg.tds.knime.scripting.groovy;

import de.mpicbg.tds.knime.knutils.scripting.ScriptingNodeDialog;
import de.mpicbg.tds.knime.scripting.GroovyScriptingBundleActivator;
import de.mpicbg.tds.knime.scripting.prefs.GroovyScriptingPreferenceInitializer;

/**
 * Created by IntelliJ IDEA.
 * User: Antje Niederlein
 * Date: 10/27/11
 * Time: 2:22 PM
 */
public class GroovyScriptNodeDialog extends ScriptingNodeDialog {
    public GroovyScriptNodeDialog(String defaultScript, boolean hasOutput, boolean useTemplateRepository) {
        super(defaultScript, new GroovyColReformatter(), hasOutput, useTemplateRepository);
    }

    @Override
    public String getTemplatesFromPreferences() {
        return GroovyScriptingBundleActivator.getDefault().getPreferenceStore().getString(GroovyScriptingPreferenceInitializer.GROOVY_TEMPLATE_RESOURCES);
    }
}
