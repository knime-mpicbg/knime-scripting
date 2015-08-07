package de.mpicbg.knime.scripting.r.generic;

import de.mpicbg.knime.scripting.core.ScriptProvider;
import de.mpicbg.knime.scripting.core.exceptions.KnimeScriptingException;
import de.mpicbg.knime.scripting.r.RUtils;
import de.mpicbg.knime.scripting.r.plots.AbstractRPlotNodeModel;

import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.port.PortObject;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.core.node.port.PortType;
import org.rosuda.REngine.Rserve.RConnection;


/**
 * A generic R node which creates a figure from a genericR input.
 *
 * @author Holger Brandl
 */
public class GenericRPlotNodeModel extends AbstractRPlotNodeModel {


    public GenericRPlotNodeModel() {
        super(createPorts(1, RPortObject.TYPE, RPortObject.class));
    }


    /**
     * This constructor is just necessary to allow superclasses to create plot nodes with ouputs
     */
    public GenericRPlotNodeModel(PortType[] inPorts, PortType[] outports) {
        super(inPorts, outports);
    }


    @Override
    protected PortObject[] execute(PortObject[] inData, ExecutionContext exec) throws Exception {
        RConnection connection = RUtils.createConnection();
        PortObject[] nodeOutput = null;
        try {
	        // 1) restore the workspace in a different server session
	        RUtils.pushToR(inData, connection, exec);
	
	        // 2) create the figure
	        adaptHardwiredTemplateToContext(ScriptProvider.unwrapPortSpecs(inData));
	        createFigure(connection);
	
	
	        // 3) prepare the output tables (which will do nothing in most cases, as most plot nodes don't have output)
	        nodeOutput = prepareOutput(exec, connection);
	
	        // 3) close the connection to R (which will also delete the temporary workspace on the server)
	        connection.close();
        } catch(Exception e) {
        	connection.close();
        	throw e;
        }

        return nodeOutput;
    }


    @Override
    protected PortObjectSpec[] configure(final PortObjectSpec[] inSpecs) throws InvalidSettingsException {
        return new PortObjectSpec[0];
    }


    /**
     * Prepares the ouput tables of this nodes. As most plot-nodes won't have any data output, this method won't be
     * overridden in most cases. Just in case a node should have both (an review and a data table output), you may
     * override it.
     */
    protected PortObject[] prepareOutput(ExecutionContext exec, RConnection connection) {
        return new BufferedDataTable[0];
    }
    
	@Override
	protected void openIn(BufferedDataTable[] inData, ExecutionContext exec) throws KnimeScriptingException {
		throw new KnimeScriptingException("not yet implemented");
	}


	@Override
	protected BufferedDataTable[] executeImpl(BufferedDataTable[] inData,
			ExecutionContext exec) throws Exception {
		// TODO Auto-generated method stub
		return null;
	}
}
