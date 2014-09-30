package de.mpicbg.knime.scripting.r.rgg;

import de.mpicbg.knime.scripting.core.ScriptingNodeDialog;
import de.mpicbg.knime.scripting.core.rgg.wizard.ScriptTemplate;
import de.mpicbg.knime.scripting.r.generic.GenericRPlotNodeModel;
import de.mpicbg.knime.scripting.r.plots.AbstractRPlotNodeFactory;

import org.knime.core.node.NodeDialogPane;


/**
 * Document me!
 *
 * @author Holger Brandl
 */
public abstract class HardwiredGenericRPlotNodeFactory extends AbstractRPlotNodeFactory<GenericRPlotNodeModel> implements ScriptFileProvider {

    private ScriptTemplate hardwiredTemplate;


    @Override
    public GenericRPlotNodeModel createNodeModel() {
        if (hardwiredTemplate == null) {
            hardwiredTemplate = HardwiredRSnippetNodeFactory.loadTemplate(this);
        }

        GenericRPlotNodeModel model = createNodeModelInternal();
        model.setHardwiredTemplate(hardwiredTemplate);

        return model;
    }


    protected abstract GenericRPlotNodeModel createNodeModelInternal();


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
