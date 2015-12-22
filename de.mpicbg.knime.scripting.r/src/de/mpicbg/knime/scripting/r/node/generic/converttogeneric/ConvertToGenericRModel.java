package de.mpicbg.knime.scripting.r.node.generic.converttogeneric;

import de.mpicbg.knime.knutils.AbstractNodeModel;
import de.mpicbg.knime.scripting.core.AbstractScriptingNodeModel;
import de.mpicbg.knime.scripting.r.RSnippetNodeModel;
import de.mpicbg.knime.scripting.r.RUtils;
import de.mpicbg.knime.scripting.r.generic.RPortObject;
import de.mpicbg.knime.scripting.r.generic.RPortObjectSpec;

import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.port.PortObject;
import org.knime.core.node.port.PortObjectSpec;
import org.rosuda.REngine.Rserve.RConnection;

import java.io.File;

import static de.mpicbg.knime.scripting.r.RSnippetNodeModel.R_INVAR_BASE_NAME;
import static de.mpicbg.knime.scripting.r.RSnippetNodeModel.R_OUTVAR_BASE_NAME;


/**
 * A generic R node which transforms generic R objects and not just BufferedDataTables (aka. data frames)
 *
 * @author Holger Brandl
 */
public class ConvertToGenericRModel extends AbstractNodeModel {

    private File rWorkspaceFile;


    public ConvertToGenericRModel() {
        // the input port is optional just to allow generative R nodes
        super(createPorts(1, BufferedDataTable.TYPE, BufferedDataTable.class), createPorts(1, RPortObject.TYPE, RPortObject.class));
    }


    @Override
    protected PortObject[] execute(PortObject[] inObjects, ExecutionContext exec) throws Exception {

        RConnection connection = RUtils.createConnection();

        try {
        // 1) onvert the data and push them to R
        RUtils.pushToR(inObjects, connection, exec, AbstractScriptingNodeModel.CHUNK_IN_DFT);

        connection.voidEval(R_OUTVAR_BASE_NAME + " <- " + R_INVAR_BASE_NAME);


        // 2) write a local workspace file which contains the input table of the node
        if (rWorkspaceFile == null) {
            rWorkspaceFile = File.createTempFile("genericR", "R");  //Note: this r is just a filename suffix
        }

        RUtils.saveToLocalFile(rWorkspaceFile, connection, RUtils.getHost(), RSnippetNodeModel.R_OUTVAR_BASE_NAME);
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


    @Override
    protected BufferedDataTable[] execute(BufferedDataTable[] inData, ExecutionContext exec) throws Exception {
        throw new RuntimeException("fake implementation: This method should be never called");
    }
}
