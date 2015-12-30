package de.mpicbg.knime.scripting.r.node.generic.converttogeneric;

import java.io.File;

import org.knime.core.data.DataTableSpec;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.defaultnodesettings.SettingsModelIntegerBounded;
import org.knime.core.node.port.PortObject;
import org.knime.core.node.port.PortObjectSpec;
import org.rosuda.REngine.Rserve.RConnection;

import de.mpicbg.knime.knutils.AbstractNodeModel;
import de.mpicbg.knime.scripting.core.AbstractScriptingNodeModel;
import de.mpicbg.knime.scripting.r.RUtils;
import de.mpicbg.knime.scripting.r.port.RPortObject;
import de.mpicbg.knime.scripting.r.port.RPortObjectSpec;


/**
 * A generic R node which transforms KNIME tables to generic R objects
 *
 * @author Antje Janosch
 */
public class ConvertToGenericRModel2 extends AbstractNodeModel {

	/**
	 * constructor
	 */
    public ConvertToGenericRModel2() {
        // the input port is optional just to allow generative R nodes
        super(createPorts(1, BufferedDataTable.TYPE, BufferedDataTable.class), createPorts(1, RPortObject.TYPE, RPortObject.class));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected PortObject[] execute(PortObject[] inObjects, ExecutionContext exec) throws Exception {

        RConnection connection = RUtils.createConnection();
        File rWorkspaceFile = null;
        RPortObject outPort = null;
        
        BufferedDataTable[] inTables = AbstractScriptingNodeModel.castToBDT(inObjects);
        DataTableSpec inSpec = inTables[0].getDataTableSpec();
        
        int chunkInSize = RUtils.getChunkIn(
        		((SettingsModelIntegerBounded) this.getModelSetting(AbstractScriptingNodeModel.CHUNK_IN)).getIntValue(), inTables);

        try {
        	// push color/size/shape model to R
    		RUtils.pushColorModelToR(inSpec, connection, exec);
    		RUtils.pushShapeModelToR(inSpec, connection, exec);
    		RUtils.pushSizeModelToR(inSpec, connection, exec);
    		
    		// push flow variables to R
    		RUtils.pushFlowVariablesToR(getAvailableInputFlowVariables(), connection, exec);

        	// 1) convert the data and push them to R
        	// TODO: implement chunk usage
        	RUtils.pushToR(inTables, connection, exec, chunkInSize);
        	
        	exec.setMessage("Save R-workspace (cannot be cancelled)");

        	// 2) write a local workspace file which contains the input table of the node
        	if (rWorkspaceFile == null) {
        		rWorkspaceFile = File.createTempFile("genericR", ".RData");  //Note: this r is just a filename suffix
        	}
        	RUtils.saveWorkspaceToFile(rWorkspaceFile, connection, RUtils.getHost());
        	
        	outPort = new RPortObject(connection, rWorkspaceFile);
        	exec.setProgress(1.0);
        	
        } catch(Exception e) {
        	connection.close();
        	throw e;
        }

        connection.close();

        return new PortObject[]{outPort};
    }

    /**
     * {@inheritDoc}
     */
    @Override
    // note: This is not the usual configure but a more generic one with PortObjectSpec instead of DataTableSpec
    protected PortObjectSpec[] configure(final PortObjectSpec[] inSpecs) throws InvalidSettingsException {
        return new PortObjectSpec[]{RPortObjectSpec.INSTANCE};
    }



}
