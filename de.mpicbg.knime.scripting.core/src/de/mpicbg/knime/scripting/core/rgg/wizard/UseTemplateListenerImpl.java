package de.mpicbg.knime.scripting.core.rgg.wizard;

import de.mpicbg.knime.scripting.core.ScriptingNodeDialog;


/**
 * @author Holger Brandl
 */
public class UseTemplateListenerImpl implements UseTemplateListener {

    private ScriptingNodeDialog rSnippetNodeDialog;


    public UseTemplateListenerImpl(ScriptingNodeDialog rSnippetNodeDialog) {
        this.rSnippetNodeDialog = rSnippetNodeDialog;
    }


    public void useTemplate(ScriptTemplate template) {
        if (template != null) {
            // not necessary because of undo-option
//                    int status = JOptionPane.showConfirmDialog(scriptEditor, "Are you sure that you would like to replace the existing script with the selected template?");
//                    if(status == JOptionPane.CANCEL_OPTION){
//                        return;
//                    }

            // change the content of the script-editor by using a cloned template (to avoid that template changes are reflected in the repository)
            rSnippetNodeDialog.useTemplate((ScriptTemplate) template.clone());
        }
    }
}
