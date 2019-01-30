package de.mpicbg.knime.scripting.groovy;

import java.nio.file.Path;
import java.nio.file.Paths;

import org.osgi.framework.Bundle;
import org.osgi.framework.FrameworkUtil;

import de.mpicbg.knime.scripting.core.ScriptingNodeDialog;
import de.mpicbg.knime.scripting.core.utils.ScriptingUtils;
import de.mpicbg.knime.scripting.groovy.prefs.GroovyScriptingPreferenceInitializer;

/**
 * Created by IntelliJ IDEA.
 * User: Antje Niederlein
 * Date: 10/27/11
 * Time: 2:22 PM
 */
public class GroovyScriptNodeDialog extends ScriptingNodeDialog {
    public GroovyScriptNodeDialog(String defaultScript, boolean useTemplateRepository) {
        super(defaultScript, new GroovyColumnSupport(), useTemplateRepository, false, false);
    }

    @Override
    public String getTemplatesFromPreferences() {
        return GroovyScriptingBundleActivator.getDefault().getPreferenceStore().getString(GroovyScriptingPreferenceInitializer.GROOVY_TEMPLATE_RESOURCES);
    }
    
	@Override
	protected Path getTemplateCachePath() {
		Bundle bundle = FrameworkUtil.getBundle(getClass());
        String bundlePath = ScriptingUtils.getBundlePath(bundle).toOSString();
        return Paths.get(bundlePath, ScriptingUtils.LOCAL_CACHE_FOLDER);
	}
}
