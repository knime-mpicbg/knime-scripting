package de.mpicbg.knime.scripting.r.rgg;

import org.knime.core.node.NodeDialogPane;

import de.mpicbg.knime.knutils.AbstractNodeModel;
import de.mpicbg.knime.scripting.core.AbstractScriptingNodeModel;
import de.mpicbg.knime.scripting.core.ScriptFileProvider;
import de.mpicbg.knime.scripting.core.ScriptingModelConfig;
import de.mpicbg.knime.scripting.core.ScriptingNodeDialog;
import de.mpicbg.knime.scripting.core.TemplateConfigurator;
import de.mpicbg.knime.scripting.core.rgg.wizard.ScriptTemplate;
import de.mpicbg.knime.scripting.r.AbstractRScriptingNodeModel;
import de.mpicbg.knime.scripting.r.RColumnSupport;
import de.mpicbg.knime.scripting.r.node.snippet.RSnippetNodeFactory;
import de.mpicbg.knime.scripting.r.node.snippet.RSnippetNodeModel;


/**
 * @author Holger Brandl
 */
public abstract class HardwiredRSnippetNodeFactory extends RSnippetNodeFactory implements ScriptFileProvider {

    private ScriptTemplate hardwiredTemplate;


    @Override
    public RSnippetNodeModel createNodeModel() {
        if (hardwiredTemplate == null) {
            hardwiredTemplate = AbstractScriptingNodeModel.loadTemplate(this);
        }

        RSnippetNodeModel snippetNodeModel = createNodeModelInternal();
        snippetNodeModel.setHardwiredTemplate(hardwiredTemplate);

        return snippetNodeModel;
    }


    protected RSnippetNodeModel createNodeModelInternal() {
    	return new RSnippetNodeModel(new ScriptingModelConfig(
    			AbstractNodeModel.createPorts(getNumberOfInputPorts()), 
    			AbstractNodeModel.createPorts(1),
    			new RColumnSupport(), 
    			true, 
    			false, 
    			true));
    }


    public int getNumberOfInputPorts() {
        return 1;
    }


    @Override
    public NodeDialogPane createNodeDialogPane() {
        ScriptingNodeDialog configPane = (ScriptingNodeDialog) super.createNodeDialogPane();

        configPane.setHardwiredTemplate(hardwiredTemplate);

        return configPane;
    }


    protected boolean enableTemplateRepository() {
        return false;
    }


    // note: here we should simply return null if the node needs to be configured before execution


    public static String getDefaultScriptForModel(ScriptFileProvider fileProvider) {
        ScriptTemplate template = AbstractScriptingNodeModel.loadTemplate(fileProvider);

        return TemplateConfigurator.generateScript(template);
    }


    
}
