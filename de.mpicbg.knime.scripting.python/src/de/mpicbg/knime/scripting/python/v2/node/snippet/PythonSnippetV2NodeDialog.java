package de.mpicbg.knime.scripting.python.v2.node.snippet;

import java.nio.file.Path;
import java.nio.file.Paths;

import org.osgi.framework.Bundle;
import org.osgi.framework.FrameworkUtil;

import de.mpicbg.knime.scripting.core.ScriptingModelConfig;
import de.mpicbg.knime.scripting.core.ScriptingNodeDialog;
import de.mpicbg.knime.scripting.core.utils.ScriptingUtils;
import de.mpicbg.knime.scripting.python.PythonColumnSupport;
import de.mpicbg.knime.scripting.python.PythonScriptingBundleActivator;
import de.mpicbg.knime.scripting.python.prefs.PythonPreferenceInitializer;


/**
 * <code>NodeDialog</code> for the "Python nippet" Node.
 *
 * Antje Janosch (MPI-CBG)
 */
public class PythonSnippetV2NodeDialog extends ScriptingNodeDialog {


	/**
	 * constructor for python snippet node dialog
	 * @param defaultScript
	 * @param smc	{@link ScriptingModelConfig}
	 */
	public PythonSnippetV2NodeDialog(String defaultScript, ScriptingModelConfig smc) {
		super(defaultScript, smc);
	}
    
    /**
     * configuration dialog for generic nodes
     * 
     * @param defaultScript
     * @param useTemplateRepository
     * @param useOpenIn
     */
    public PythonSnippetV2NodeDialog(String defaultScript, 
    		boolean useTemplateRepository, 
    		boolean useOpenIn,
    		boolean useChunkSettings) {
		super(defaultScript, new PythonColumnSupport(), useTemplateRepository, useOpenIn, useChunkSettings);
	}

    /**
     * {@inheritDoc}
     */
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
