package de.mpicbg.knime.scripting.r.node.generic.plot;

import de.mpicbg.knime.scripting.r.plots.AbstractRPlotNodeFactory;


/**
 * <code>NodeFactory</code> for the "RSnippet" Node. Improved R Integration for Knime
 *
 * @author Holger Brandl (MPI-CBG)
 */
public class GenericRPlotFactory2 extends AbstractRPlotNodeFactory {

    @Override
    public GenericRPlotNodeModel2 createNodeModel() {
        return new GenericRPlotNodeModel2();
    }
}