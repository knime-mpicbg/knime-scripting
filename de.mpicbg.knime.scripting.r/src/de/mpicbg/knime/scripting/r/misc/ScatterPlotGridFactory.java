package de.mpicbg.knime.scripting.r.misc;

import de.mpicbg.knime.scripting.r.oldhardwired.HardwiredRPlotNodeFactory;

/**
 * @author Holger Brandl (MPI-CBG)
 */
public class ScatterPlotGridFactory extends HardwiredRPlotNodeFactory {


    public String getTemplateFileName() {
        return "ScatterPlotGrid.rgg";
    }

}


