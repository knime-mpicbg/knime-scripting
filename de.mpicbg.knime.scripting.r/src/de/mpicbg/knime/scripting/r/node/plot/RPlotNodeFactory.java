package de.mpicbg.knime.scripting.r.node.plot;

import de.mpicbg.knime.scripting.r.plots.AbstractRPlotNodeFactory;


/**
 * @author Holger Brandl (MPI-CBG)
 */
public class RPlotNodeFactory extends AbstractRPlotNodeFactory<RPlotNodeModel> {

    @Override
    public RPlotNodeModel createNodeModel() {
        return new RPlotNodeModel();
    }
}