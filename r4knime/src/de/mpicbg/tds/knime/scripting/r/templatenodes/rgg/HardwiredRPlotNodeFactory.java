package de.mpicbg.tds.knime.scripting.r.templatenodes.rgg;

import de.mpicbg.tds.knime.knutils.scripting.ScriptingNodeDialog;
import de.mpicbg.tds.knime.knutils.scripting.templatewizard.ScriptTemplate;
import de.mpicbg.tds.knime.scripting.r.RPlotNodeFactory;
import de.mpicbg.tds.knime.scripting.r.RPlotNodeModel;
import org.knime.core.node.NodeDialogPane;


/**
 * Document me!
 *
 * @author Holger Brandl
 */
public abstract class HardwiredRPlotNodeFactory extends RPlotNodeFactory implements ScriptFileProvider {

    private ScriptTemplate hardwiredTemplate;


    @Override
    public RPlotNodeModel createNodeModel() {
        if (hardwiredTemplate == null) {
            hardwiredTemplate = HardwiredRSnippetNodeFactory.loadTemplate(this);
        }

        RPlotNodeModel model = createNodeModelInternal();
        model.setHardwiredTemplate(hardwiredTemplate);

        return model;
    }


    protected RPlotNodeModel createNodeModelInternal() {
        return new RPlotNodeModel();
    }


    @Override
    public NodeDialogPane createNodeDialogPane() {
        ScriptingNodeDialog configPane = (ScriptingNodeDialog) super.createNodeDialogPane();

        ScriptTemplate template = HardwiredRSnippetNodeFactory.loadTemplate(this);
        configPane.setHardwiredTemplate(template);


        return configPane;
    }


    protected boolean enableTemplateRepository() {
        return false;
    }
}
