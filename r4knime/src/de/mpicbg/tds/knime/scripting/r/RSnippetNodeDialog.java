package de.mpicbg.tds.knime.scripting.r;

import de.mpicbg.tds.knime.knutils.scripting.ScriptingNodeDialog;
import de.mpicbg.tds.knime.scripting.r.prefs.RPreferenceInitializer;
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
     * @param hasOutput
     * @param useTemplateRepository
     */
    public RSnippetNodeDialog(String defaultScript, boolean hasOutput, boolean useTemplateRepository) {
        super(defaultScript, new RColNameReformater(), hasOutput, useTemplateRepository);
    }

    @Override
    public String getTemplatesFromPreferences() {
        return R4KnimeBundleActivator.getDefault().getPreferenceStore().getString(RPreferenceInitializer.R_SNIPPET_TEMPLATES);
    }


}
