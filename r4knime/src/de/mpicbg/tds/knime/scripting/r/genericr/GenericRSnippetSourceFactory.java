package de.mpicbg.tds.knime.scripting.r.genericr;

import de.mpicbg.tds.knime.scripting.r.R4KnimeBundleActivator;
import de.mpicbg.tds.knime.scripting.r.RSnippetNodeDialog;
import de.mpicbg.tds.knime.scripting.r.RUtils;
import de.mpicbg.tds.knime.scripting.r.prefs.RPreferenceInitializer;
import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeFactory;
import org.knime.core.node.NodeView;
import org.knime.core.node.port.PortType;


/**
 * <code>NodeFactory</code> for the "RSnippet" Node. Improved R Integration for Knime
 *
 * @author Holger Brandl (MPI-CBG)
 */
public class GenericRSnippetSourceFactory
        extends NodeFactory<GenericRSnippet> {

    @Override
    public GenericRSnippet createNodeModel() {
        return new GenericRSnippet(new PortType[0], GenericRSnippet.createPorts(1, RPortObject.TYPE, RPortObject.class));
    }


    @Override
    public int getNrNodeViews() {
        return 0;
    }


    @Override
    public NodeView<GenericRSnippet> createNodeView(final int viewIndex,
                                                    final GenericRSnippet nodeModel) {
//        return new RSnippetNodeView(nodeModel);
        return null;
    }


    @Override
    public boolean hasDialog() {
        return true;
    }


    @Override
    public NodeDialogPane createNodeDialogPane() {
        String templateResources = R4KnimeBundleActivator.getDefault().getPreferenceStore().getString(RPreferenceInitializer.R_SNIPPET_TEMPLATES);
        return new RSnippetNodeDialog(RUtils.SCRIPT_PROPERTY_DEFAULT, templateResources, true, true);
    }

}


