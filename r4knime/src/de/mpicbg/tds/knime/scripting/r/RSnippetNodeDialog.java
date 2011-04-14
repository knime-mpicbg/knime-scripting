package de.mpicbg.tds.knime.scripting.r;

import de.mpicbg.tds.knime.knutils.scripting.ColNameReformater;
import de.mpicbg.tds.knime.knutils.scripting.ScriptingNodeDialog;
import org.knime.core.data.DataType;
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
     * @param templateResources
     * @param hasOutput
     * @param useTemplateRepository
     */
    public RSnippetNodeDialog(String defaultScript, String templateResources, boolean hasOutput, boolean useTemplateRepository) {
        super(defaultScript, new RColNameReformater(), templateResources, hasOutput, useTemplateRepository);
    }


    private static class RColNameReformater implements ColNameReformater {

        public String reformat(String name, DataType type, boolean altDown) {

            if (altDown) {
                if (name.contains(" ")) {
                    return RSnippetNodeModel.R_INVAR_BASE_NAME + "$\"" + name + "\"";
                } else {
                    return RSnippetNodeModel.R_INVAR_BASE_NAME + "$" + name + "";
                }

            } else {
                return "\"" + name + "\"";
            }
        }
    }
}
