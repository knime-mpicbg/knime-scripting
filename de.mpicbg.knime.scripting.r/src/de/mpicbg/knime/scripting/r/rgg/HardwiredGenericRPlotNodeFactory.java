package de.mpicbg.knime.scripting.r.rgg;

import org.knime.core.node.NodeDialogPane;

import de.mpicbg.knime.scripting.core.AbstractScriptingNodeModel;
import de.mpicbg.knime.scripting.core.ScriptFileProvider;
import de.mpicbg.knime.scripting.core.ScriptingNodeDialog;
import de.mpicbg.knime.scripting.core.rgg.wizard.ScriptTemplate;
import de.mpicbg.knime.scripting.r.generic.GenericRPlotNodeModel;
import de.mpicbg.knime.scripting.r.node.plot.RPlotNodeDialog;
import de.mpicbg.knime.scripting.r.plots.AbstractRPlotNodeFactory;


/**
 * Document me!
 *
 * @author Holger Brandl
 */
public abstract class HardwiredGenericRPlotNodeFactory extends AbstractRPlotNodeFactory implements ScriptFileProvider {

    private ScriptTemplate hardwiredTemplate;


    @Override
    public GenericRPlotNodeModel createNodeModel() {
        if (hardwiredTemplate == null) {
            hardwiredTemplate = AbstractScriptingNodeModel.loadTemplate(this);
        }

        GenericRPlotNodeModel model = createNodeModelInternal();
        model.setHardwiredTemplate(hardwiredTemplate);

        return model;
    }


    protected abstract GenericRPlotNodeModel createNodeModelInternal();


    @Override
    public NodeDialogPane createNodeDialogPane() {
        ScriptingNodeDialog configPane = new RPlotNodeDialog(createNodeModel().getDefaultScript(""), false, false);

        ScriptTemplate template = AbstractScriptingNodeModel.loadTemplate(this);
        configPane.setHardwiredTemplate(template);


        return configPane;
    }
}
