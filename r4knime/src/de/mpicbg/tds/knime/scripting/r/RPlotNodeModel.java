package de.mpicbg.tds.knime.scripting.r;

import de.mpicbg.tds.knime.knutils.scripting.ScriptProvider;
import de.mpicbg.tds.knime.scripting.r.plots.AbstractRPlotNodeModel;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.port.PortType;
import org.rosuda.REngine.Rserve.RConnection;


/**
 * This is the model implementation of a plot panel that requires a data-table and a script as input.
 *
 * @author Holger Brandl (MPI-CBG)
 */
public class RPlotNodeModel extends AbstractRPlotNodeModel {


    public RPlotNodeModel() {
        super(createPorts(1));
    }


    public RPlotNodeModel(PortType[] inPorts, PortType[] outports) {
        super(inPorts, outports);
    }


    @Override
    protected BufferedDataTable[] execute(final BufferedDataTable[] inData,
                                          final ExecutionContext exec) throws Exception {

        logger.info("Render the R Plot");

        RConnection connection = RUtils.createConnection();

        // 1) convert exampleSet ihnto data-frame and put into the r-workspace
        RUtils.pushToR(inData, connection, exec);

        adaptHardwiredTemplateToContext(ScriptProvider.unwrapPortSpecs(inData));
        createFigure(connection);

        BufferedDataTable[] result = prepareOutput(exec, connection);

        // close the connection to R
        connection.close();

        return result;
    }


    /**
     * Prepares the ouput tables of this nodes. As most plot-nodes won't have any data output, this method won't be
     * overridden in most cases. Just in case a node should have both (an review and a data table output), you may
     * override it.
     */
    protected BufferedDataTable[] prepareOutput(ExecutionContext exec, RConnection connection) {
        return new BufferedDataTable[0];
    }
}