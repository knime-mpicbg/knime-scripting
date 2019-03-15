package de.mpicbg.knime.scripting.python.v2.node.snippet;

import java.nio.file.Path;
import java.nio.file.Paths;

import org.osgi.framework.Bundle;
import org.osgi.framework.FrameworkUtil;

import de.mpicbg.knime.scripting.core.ScriptingNodeDialog;
import de.mpicbg.knime.scripting.core.utils.ScriptingUtils;
import de.mpicbg.knime.scripting.python.PythonColumnSupport;
import de.mpicbg.knime.scripting.python.PythonScriptingBundleActivator;
import de.mpicbg.knime.scripting.python.prefs.PythonPreferenceInitializer;


/**
 * <code>NodeDialog</code> for the "RSnippet" Node.
 *
 * @author Holger Brandl, Antje Janosch (MPI-CBG)
 */
public class PythonSnippetV2NodeDialog extends ScriptingNodeDialog {

    /**
     * New pane for configuring ScriptedNode node dialog
     *
     * @param defaultScript
     * @param useTemplateRepository
     */
    public PythonSnippetV2NodeDialog(String defaultScript, boolean useTemplateRepository) {
        super(defaultScript, new PythonColumnSupport(), useTemplateRepository);
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
