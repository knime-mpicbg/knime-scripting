package de.mpicbg.knime.scripting.r.node.generic.source;

import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeFactory;
import org.knime.core.node.NodeView;
import org.knime.core.node.port.PortType;

import de.mpicbg.knime.scripting.r.RSnippetNodeDialog;
import de.mpicbg.knime.scripting.r.node.generic.snippet.GenericRSnippetNodeModel2;
import de.mpicbg.knime.scripting.r.port.RPortObject;


/**
 *
 * @author Holger Brandl, Antje Janosch (MPI-CBG)
 */
public class GenericRSnippetSourceFactory2
        extends NodeFactory<GenericRSnippetNodeModel2> {

    @Override
    public GenericRSnippetNodeModel2 createNodeModel() {
        return new GenericRSnippetNodeModel2(new PortType[0], GenericRSnippetNodeModel2.createPorts(1, RPortObject.TYPE, RPortObject.class), false);
    }


    @Override
    public int getNrNodeViews() {
        return 0;
    }


    @Override
    public boolean hasDialog() {
        return true;
    }


    @Override
    public NodeDialogPane createNodeDialogPane() {
        //String templateResources = R4KnimeBundleActivator.getDefault().getPreferenceStore().getString(RPreferenceInitializer.R_SNIPPET_TEMPLATES);
        return new RSnippetNodeDialog(GenericRSnippetNodeModel2.GENERIC_SNIPPET_DFT, false, false, false);
    }

	@Override
	public NodeView<GenericRSnippetNodeModel2> createNodeView(int viewIndex, GenericRSnippetNodeModel2 nodeModel) {
		// TODO Auto-generated method stub
		return null;
	}

}


