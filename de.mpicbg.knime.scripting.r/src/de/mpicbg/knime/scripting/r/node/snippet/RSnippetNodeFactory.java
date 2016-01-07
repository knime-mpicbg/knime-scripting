package de.mpicbg.knime.scripting.r.node.snippet;

import de.mpicbg.knime.scripting.r.AbstractRSnippetNodeFactory;

/**
 * <code>NodeFactory</code> for the "RSnippet" Node. Improved R Integration for Knime
 *
 * @author Holger Brandl (MPI-CBG)
 */
public class RSnippetNodeFactory
        extends AbstractRSnippetNodeFactory {

    @Override
    public RSnippetNodeModel createNodeModel() {
        return new RSnippetNodeModel(1, 1);
    }
}


