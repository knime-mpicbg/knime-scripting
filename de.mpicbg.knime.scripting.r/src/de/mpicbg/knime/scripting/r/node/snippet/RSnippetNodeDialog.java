package de.mpicbg.knime.scripting.r.node.snippet;

import java.nio.file.Path;
import java.nio.file.Paths;

import org.osgi.framework.Bundle;
import org.osgi.framework.FrameworkUtil;

import de.mpicbg.knime.scripting.core.ScriptingNodeDialog;
import de.mpicbg.knime.scripting.core.utils.ScriptingUtils;
import de.mpicbg.knime.scripting.r.R4KnimeBundleActivator;
import de.mpicbg.knime.scripting.r.RColumnSupport;
import de.mpicbg.knime.scripting.r.prefs.RPreferenceInitializer;


/**
 * <code>NodeDialog</code> for the "RSnippet" Node.
 *
 * @author Holger Brandl, Antje Janosch (MPI-CBG)
 */
public class RSnippetNodeDialog extends ScriptingNodeDialog {

    /**
     * New pane for configuring ScriptedNode node dialog
     *
     * @param defaultScript
     * @param useTemplateRepository
     */
    public RSnippetNodeDialog(String defaultScript, boolean useTemplateRepository) {
        super(defaultScript, new RColumnSupport(), useTemplateRepository);
    }
    
    /**
     * configuration dialog for generic nodes
     * 
     * @param defaultScript
     * @param useTemplateRepository
     * @param useOpenIn
     */
    public RSnippetNodeDialog(String defaultScript, 
    		boolean useTemplateRepository, 
    		boolean useOpenIn,
    		boolean useChunkSettings) {
		super(defaultScript, new RColumnSupport(), useTemplateRepository, useOpenIn, useChunkSettings);
	}

    /**
     * {@inheritDoc}
     */
    @Override
    public String getTemplatesFromPreferences() {
        return R4KnimeBundleActivator.getDefault().getPreferenceStore().getString(RPreferenceInitializer.R_SNIPPET_TEMPLATES);
    }

	@Override
	protected Path getTemplateCachePath() {
		Bundle bundle = FrameworkUtil.getBundle(getClass());
        String bundlePath = ScriptingUtils.getBundlePath(bundle).toOSString();
        return Paths.get(bundlePath, ScriptingUtils.LOCAL_CACHE_FOLDER);
	}
}
