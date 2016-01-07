package de.mpicbg.knime.scripting.r;

import de.mpicbg.knime.scripting.r.node.plot.RPlotNodeModel;
import de.mpicbg.knime.scripting.r.plots.AbstractRPlotNodeFactory;


/**
 * @author Holger Brandl (MPI-CBG)
 */
public class RPlotWithImPortNodeFactory extends AbstractRPlotNodeFactory<RPlotNodeModel> {

    @Override
    public RPlotNodeModel createNodeModel() {
        return new RPlotWithImPortNodeModel();
    }
}