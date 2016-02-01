package de.mpicbg.knime.scripting.r.rgg;

import org.knime.core.node.NodeDialogPane;

import de.mpicbg.knime.scripting.core.AbstractScriptingNodeModel;
import de.mpicbg.knime.scripting.core.ScriptFileProvider;
import de.mpicbg.knime.scripting.core.ScriptingNodeDialog;
import de.mpicbg.knime.scripting.core.rgg.wizard.ScriptTemplate;
import de.mpicbg.knime.scripting.r.node.plot.RPlotNodeDialog;
import de.mpicbg.knime.scripting.r.node.plot.RPlotNodeFactory;
import de.mpicbg.knime.scripting.r.node.plot.RPlotNodeModel;


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
            hardwiredTemplate = AbstractScriptingNodeModel.loadTemplate(this);
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
    	ScriptingNodeDialog configPane =  new RPlotNodeDialog(createNodeModel().getDefaultScript(""), false, false);

        ScriptTemplate template = AbstractScriptingNodeModel.loadTemplate(this);
        configPane.setHardwiredTemplate(template);

        return configPane;
    }


    protected boolean enableTemplateRepository() {
        return false;
    }
}
