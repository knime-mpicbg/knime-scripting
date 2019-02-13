package de.mpicbg.knime.scripting.matlab.snippet;

import java.nio.file.Path;
import java.nio.file.Paths;

import org.osgi.framework.Bundle;
import org.osgi.framework.FrameworkUtil;

import de.mpicbg.knime.scripting.core.ScriptingNodeDialog;
import de.mpicbg.knime.scripting.core.utils.ScriptingUtils;
import de.mpicbg.knime.scripting.matlab.MatlabColumnSupport;
import de.mpicbg.knime.scripting.matlab.MatlabScriptingBundleActivator;
import de.mpicbg.knime.scripting.matlab.prefs.MatlabPreferenceInitializer;

/**
 * Created by IntelliJ IDEA.
 * User: Antje Niederlein
 * Date: 10/27/11
 * Time: 2:03 PM
 */
public class MatlabSnippetNodeDialog extends ScriptingNodeDialog {
    public MatlabSnippetNodeDialog(String defaultScript, boolean useTemplateRepository) {
        super(defaultScript, new MatlabColumnSupport(), useTemplateRepository);
    }

    @Override
    public String getTemplatesFromPreferences() {
        return MatlabScriptingBundleActivator.getDefault().getPreferenceStore().getString(MatlabPreferenceInitializer.MATLAB_TEMPLATE_RESOURCES);
    }
    
	@Override
	protected Path getTemplateCachePath() {
		Bundle bundle = FrameworkUtil.getBundle(getClass());
        String bundlePath = ScriptingUtils.getBundlePath(bundle).toOSString();
        return Paths.get(bundlePath, ScriptingUtils.LOCAL_CACHE_FOLDER);
	}
}
