package de.mpicbg.knime.scripting.r.rgg;

import de.mpicbg.knime.scripting.core.ScriptingNodeDialog;
import de.mpicbg.knime.scripting.core.rgg.wizard.ScriptTemplate;
import de.mpicbg.knime.scripting.r.RPlotNodeFactory;
import de.mpicbg.knime.scripting.r.RPlotNodeModel;
import de.mpicbg.knime.scripting.r.plots.AbstractRPlotNodeFactory;
import de.mpicbg.knime.scripting.r.plots.RPlotNodeDialog;

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
    	ScriptingNodeDialog configPane =  new RPlotNodeDialog(createNodeModel().getDefaultScript(), false, false);

        ScriptTemplate template = HardwiredRSnippetNodeFactory.loadTemplate(this);
        configPane.setHardwiredTemplate(template);

        return configPane;
    }


    protected boolean enableTemplateRepository() {
        return false;
    }
}
