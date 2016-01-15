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
import de.mpicbg.knime.scripting.core.ScriptingModelConfig;
import de.mpicbg.knime.scripting.core.exceptions.KnimeScriptingException;
import de.mpicbg.knime.scripting.r.AbstractRScriptingNodeModel;
import de.mpicbg.knime.scripting.r.RColumnSupport;
import de.mpicbg.knime.scripting.r.RUtils;
import de.mpicbg.knime.scripting.r.port.RPortObject;
import de.mpicbg.knime.scripting.r.port.RPortObjectSpec;


/**
 * A generic R node which transforms KNIME tables to generic R objects
 *
 * @author Antje Janosch
 */
public class ConvertToGenericRModel2 extends AbstractRScriptingNodeModel {
	
	private static final ScriptingModelConfig nodeModelConfig = new ScriptingModelConfig(
			createPorts(1, BufferedDataTable.TYPE, BufferedDataTable.class), // 1 data table input
			createPorts(1, RPortObject.TYPE, RPortObject.class), 			// 1 generic output
			new RColumnSupport(), 
			false, 	// no script
			false, 	// no open in R
			true);	// use chunk settings

	/**
	 * constructor
	 */
    public ConvertToGenericRModel2() {
        super(nodeModelConfig);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    // note: This is not the usual configure but a more generic one with PortObjectSpec instead of DataTableSpec
    protected PortObjectSpec[] configure(final PortObjectSpec[] inSpecs) throws InvalidSettingsException {
        return new PortObjectSpec[]{RPortObjectSpec.INSTANCE};
    }

    /**
     * {@inheritDoc}
     */
	/*@Override
	protected PortObject[] executeImpl(PortObject[] inData, ExecutionContext exec) throws Exception {
        RConnection connection = RUtils.createConnection();
        File rWorkspaceFile = null;
        RPortObject outPort = null;
        
        BufferedDataTable[] inTables = AbstractScriptingNodeModel.castToBDT(inData);
        DataTableSpec inSpec = inTables[0].getDataTableSpec();
        
        int chunkInSize = getChunkIn(
        		((SettingsModelIntegerBounded) this.getModelSetting(AbstractScriptingNodeModel.CHUNK_IN)).getIntValue(), inTables);

        try {
        	// push color/size/shape model to R
    		pushColorModelToR(inSpec, connection, exec);
    		pushShapeModelToR(inSpec, connection, exec);
    		pushSizeModelToR(inSpec, connection, exec);
    		
    		// push flow variables to R
    		pushFlowVariablesToR(getAvailableInputFlowVariables(), connection, exec);

        	// convert the data and push them to R
        	//pushToR(inTables, connection, exec, chunkInSize);
        	
        	exec.setMessage("Save R-workspace (cannot be cancelled)");

        	// write a local workspace file which contains the input table of the node
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
	}*/

    /**
     * {@inheritDoc}
     */
	@Override
	protected void openIn(PortObject[] inData, ExecutionContext exec) throws KnimeScriptingException {
		// nothing to do here?
	}

}
