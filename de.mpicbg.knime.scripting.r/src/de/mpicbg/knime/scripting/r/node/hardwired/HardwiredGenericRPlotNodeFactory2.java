package de.mpicbg.knime.scripting.r.node.hardwired;

import org.knime.core.node.NodeDialogPane;

import de.mpicbg.knime.scripting.core.ScriptFileProvider;
import de.mpicbg.knime.scripting.core.ScriptingNodeDialog;
import de.mpicbg.knime.scripting.core.rgg.wizard.ScriptTemplate;
import de.mpicbg.knime.scripting.r.AbstractRScriptingNodeModel;
import de.mpicbg.knime.scripting.r.node.generic.plot.GenericRPlotNodeModel2;
import de.mpicbg.knime.scripting.r.node.plot.RPlotNodeDialog;
import de.mpicbg.knime.scripting.r.plots.AbstractRPlotNodeFactory;


/**
 * Document me!
 *
 * @author Holger Brandl
 */
public abstract class HardwiredGenericRPlotNodeFactory2 extends AbstractRPlotNodeFactory implements ScriptFileProvider {

    //private ScriptTemplate hardwiredTemplate;


    @Override
    public GenericRPlotNodeModel2 createNodeModel() {
        //if (hardwiredTemplate == null) {
        ScriptTemplate hardwiredTemplate = AbstractRScriptingNodeModel.loadTemplate(this);
        //}

        GenericRPlotNodeModel2 model = createNodeModelInternal();
        model.setHardwiredTemplate(hardwiredTemplate);

        return model;
    }


    protected abstract GenericRPlotNodeModel2 createNodeModelInternal();


    @Override
    public NodeDialogPane createNodeDialogPane() {
        ScriptingNodeDialog configPane = new RPlotNodeDialog(createNodeModel().getDefaultScript(""), false, false);

        ScriptTemplate hardwiredTemplate = AbstractRScriptingNodeModel.loadTemplate(this);
        configPane.setHardwiredTemplate(hardwiredTemplate);


        return configPane;
    }
}
