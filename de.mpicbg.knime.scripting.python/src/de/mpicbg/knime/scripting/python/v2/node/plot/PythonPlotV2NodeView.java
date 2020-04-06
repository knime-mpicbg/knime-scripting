package de.mpicbg.knime.scripting.python.v2.node.plot;

import org.knime.core.node.NodeView;

import de.mpicbg.knime.scripting.core.panels.ScriptingPlotPanel;
import de.mpicbg.knime.scripting.python.v2.plots.AbstractPythonPlotV2NodeModel;
import de.mpicbg.knime.scripting.python.v2.plots.PythonPlotCanvasV2;


public class PythonPlotV2NodeView<PythonPlotModel extends AbstractPythonPlotV2NodeModel> extends NodeView<PythonPlotModel> {

    /**
     * Creates a new view.
     *
     * @param nodeModel The model (class: {@link de.mpicbg.knime.scripting.r.node.snippet.RSnippetNodeModel})
     */
    public PythonPlotV2NodeView(final PythonPlotModel nodeModel) {
        super(nodeModel);

        updateView(nodeModel);

    }

    private void updateView(PythonPlotModel nodeModel) {
        
    	PythonPlotCanvasV2 plotCanvas = new PythonPlotCanvasV2(nodeModel);
        setComponent(new ScriptingPlotPanel(plotCanvas));
    }

	@Override
	protected void onClose() {
		// TODO Auto-generated method stub
		
	}

	@Override
	protected void onOpen() {
		// TODO Auto-generated method stub
		
	}

	@Override
	protected void modelChanged() {
        //  retrieve the new model from your nodemodel and
        // update the view.
        PythonPlotModel nodeModel = getNodeModel();
        assert nodeModel != null;

        // be aware of a possibly not executed nodeModel! The data you retrieve
        // from your nodemodel could be null, emtpy, or invalid in any kind.

        updateView(getNodeModel());
	}


}
