package de.mpicbg.knime.scripting.r.node.snippet;

import de.mpicbg.knime.scripting.core.ScriptingNodeDialog;
import de.mpicbg.knime.scripting.r.R4KnimeBundleActivator;
import de.mpicbg.knime.scripting.r.RColNameReformater;
import de.mpicbg.knime.scripting.r.prefs.RPreferenceInitializer;

import org.knime.core.node.defaultnodesettings.DefaultNodeSettingsPane;


/**
 * <code>NodeDialog</code> for the "RSnippet" Node. Improved R Integration for Knime
 * <p/>
 * This node dialog derives from {@link DefaultNodeSettingsPane} which allows creation of a simple dialog with standard
 * components. If you need a more complex dialog please derive directly from {@link
 * org.knime.core.node.NodeDialogPane}.
 *
 * @author Holger Brandl (MPI-CBG)
 */
public class RSnippetNodeDialog extends ScriptingNodeDialog {

    /**
     * New pane for configuring ScriptedNode node dialog
     *
     * @param defaultScript
     * @param useTemplateRepository
     */
    public RSnippetNodeDialog(String defaultScript, boolean useTemplateRepository) {
        super(defaultScript, new RColNameReformater(), useTemplateRepository);
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
		super(defaultScript, new RColNameReformater(), useTemplateRepository, useOpenIn, useChunkSettings);
	}

    @Override
    public String getTemplatesFromPreferences() {
        return R4KnimeBundleActivator.getDefault().getPreferenceStore().getString(RPreferenceInitializer.R_SNIPPET_TEMPLATES);
    }


}
