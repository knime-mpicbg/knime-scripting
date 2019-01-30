package de.mpicbg.knime.scripting.python.snippet;

import java.nio.file.Path;
import java.nio.file.Paths;

import org.osgi.framework.Bundle;
import org.osgi.framework.FrameworkUtil;

import de.mpicbg.knime.scripting.core.ScriptingNodeDialog;
import de.mpicbg.knime.scripting.core.utils.ScriptingUtils;
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
    
	@Override
	protected Path getTemplateCachePath() {
		Bundle bundle = FrameworkUtil.getBundle(getClass());
        String bundlePath = ScriptingUtils.getBundlePath(bundle).toOSString();
        return Paths.get(bundlePath, ScriptingUtils.LOCAL_CACHE_FOLDER);
	}
}
