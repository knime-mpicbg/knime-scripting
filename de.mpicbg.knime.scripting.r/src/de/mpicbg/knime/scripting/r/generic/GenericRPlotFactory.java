package de.mpicbg.knime.scripting.r.generic;

import de.mpicbg.knime.scripting.r.plots.AbstractRPlotNodeFactory;


/**
 * <code>NodeFactory</code> for the "RSnippet" Node. Improved R Integration for Knime
 *
 * @author Holger Brandl (MPI-CBG)
 */
public class GenericRPlotFactory extends AbstractRPlotNodeFactory<GenericRPlotNodeModel> {

    @Override
    public GenericRPlotNodeModel createNodeModel() {
        return new GenericRPlotNodeModel();
    }
}