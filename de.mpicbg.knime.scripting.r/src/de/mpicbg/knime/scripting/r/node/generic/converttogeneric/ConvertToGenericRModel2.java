package de.mpicbg.knime.scripting.r.node.generic.converttogeneric;

import java.io.File;

import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.port.PortObject;
import org.knime.core.node.port.PortObjectSpec;
import org.rosuda.REngine.Rserve.RConnection;

import de.mpicbg.knime.knutils.AbstractNodeModel;
import de.mpicbg.knime.scripting.core.AbstractScriptingNodeModel;
import de.mpicbg.knime.scripting.r.RUtils;
import de.mpicbg.knime.scripting.r.port.RPortObject;
import de.mpicbg.knime.scripting.r.port.RPortObjectSpec;


/**
 * A generic R node which transforms generic R objects
 *
 * @author Antje Janosch
 */
public class ConvertToGenericRModel2 extends AbstractNodeModel {

    public ConvertToGenericRModel2() {
        // the input port is optional just to allow generative R nodes
        super(createPorts(1, BufferedDataTable.TYPE, BufferedDataTable.class), createPorts(1, RPortObject.TYPE, RPortObject.class));
    }


    @Override
    protected PortObject[] execute(PortObject[] inObjects, ExecutionContext exec) throws Exception {

        RConnection connection = RUtils.createConnection();
        File rWorkspaceFile = null;
        
        try {
        	// 1) convert the data and push them to R
        	// TODO: implement chunk usage
        	RUtils.pushToR(inObjects, connection, exec, AbstractScriptingNodeModel.CHUNK_IN_DFT);

        	// 2) write a local workspace file which contains the input table of the node
        	if (rWorkspaceFile == null) {
        		rWorkspaceFile = File.createTempFile("genericR", "R");  //Note: this r is just a filename suffix
        	}
        	RUtils.saveWorkspaceToFile(rWorkspaceFile, connection, RUtils.getHost());
        } catch(Exception e) {
        	connection.close();
        	throw e;
        }

        connection.close();

        return new PortObject[]{new RPortObject(rWorkspaceFile)};
    }


    @Override
    // note: This is not the usual configure but a more generic one with PortObjectSpec instead of DataTableSpec
    protected PortObjectSpec[] configure(final PortObjectSpec[] inSpecs) throws InvalidSettingsException {
        return new PortObjectSpec[]{RPortObjectSpec.INSTANCE};
    }



}
