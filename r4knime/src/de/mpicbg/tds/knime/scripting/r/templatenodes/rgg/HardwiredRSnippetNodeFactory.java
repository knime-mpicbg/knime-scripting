package de.mpicbg.tds.knime.scripting.r.templatenodes.rgg;

import de.mpicbg.tds.knime.knutils.scripting.ScriptingNodeDialog;
import de.mpicbg.tds.knime.knutils.scripting.TemplateConfigurator;
import de.mpicbg.tds.knime.knutils.scripting.rgg.TemplateUtils;
import de.mpicbg.tds.knime.knutils.scripting.templatewizard.ScriptTemplate;
import de.mpicbg.tds.knime.scripting.r.RSnippetNodeFactory;
import de.mpicbg.tds.knime.scripting.r.RSnippetNodeModel;
import org.knime.core.node.NodeDialogPane;

import java.io.InputStream;


/**
 * @author Holger Brandl
 */
public abstract class HardwiredRSnippetNodeFactory extends RSnippetNodeFactory implements ScriptFileProvider {

    private ScriptTemplate hardwiredTemplate;


    @Override
    public RSnippetNodeModel createNodeModel() {
        if (hardwiredTemplate == null) {
            hardwiredTemplate = loadTemplate(this);
        }

        RSnippetNodeModel snippetNodeModel = createNodeModelInternal();
        snippetNodeModel.setHardwiredTemplate(hardwiredTemplate);

        return snippetNodeModel;
    }


    protected RSnippetNodeModel createNodeModelInternal() {
        return new RSnippetNodeModel(getNumberOfInputPorts(), 1);

        // before we were defining the default in an anonumous class like that
//         public String getDefaultScript() {
//              return HardwiredRSnippetNodeFactory.getDefaultScriptForModel(scriptFileProvider);
//         }
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
        ScriptTemplate template = loadTemplate(fileProvider);

        return TemplateConfigurator.generateScript(template);
    }


    public static ScriptTemplate loadTemplate(ScriptFileProvider scriptFileProvider) {
        String templateFileName = scriptFileProvider.getTemplateFileName();
        InputStream scriptStream = scriptFileProvider.getClass().getResourceAsStream(templateFileName);
        String unparsedTemplate = TemplateUtils.convertStreamToString(scriptStream);

        ScriptTemplate scriptTemplate = new ScriptTemplate();
        scriptTemplate.setTemplate(unparsedTemplate);
        return scriptTemplate;
    }
}
