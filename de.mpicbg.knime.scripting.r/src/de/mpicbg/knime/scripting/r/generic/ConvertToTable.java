package de.mpicbg.knime.scripting.r.generic;

import de.mpicbg.knime.knutils.AbstractNodeModel;
import de.mpicbg.knime.scripting.core.AbstractScriptingNodeModel;
import de.mpicbg.knime.scripting.core.exceptions.KnimeScriptingException;
import de.mpicbg.knime.scripting.r.AbstractRScriptingNodeModel;
import de.mpicbg.knime.scripting.r.RUtils;
import de.mpicbg.knime.scripting.r.node.snippet.RSnippetNodeModel;

import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.port.PortObject;
import org.knime.core.node.port.PortObjectSpec;
import org.rosuda.REngine.REXP;
import org.rosuda.REngine.Rserve.RConnection;

import java.io.File;


/**
 * A generic R node which converts generic R objects (which are assumed to be data-frames) back into knime
 * BufferedDatatables.
 *
 * @author Holger Brandl
 * {@deprecated}
 */
public class ConvertToTable extends AbstractRScriptingNodeModel {

    private File rWorkspaceFile;


    public ConvertToTable() {
        // the input port is optional just to allow generative R nodes
        super(createPorts(1, ROldPortObject.TYPE, ROldPortObject.class), createPorts(1, BufferedDataTable.TYPE, BufferedDataTable.class));
    }


    @Override
    protected PortObject[] execute(PortObject[] inObjects, ExecutionContext exec) throws Exception {

        RConnection connection = RUtils.createConnection();
        BufferedDataTable dataTable = null;

        try {
        // 1) restore the workspace in a different server session
        //pushToR(inObjects, connection, exec, AbstractScriptingNodeModel.CHUNK_IN_DFT);

        // 2) Make sure that the R-object in the persistied workspace is of type data-frame
        boolean isDataFrame = Boolean.parseBoolean(connection.eval("is.data.frame(" + RSnippetNodeModel.R_INVAR_BASE_NAME + ")").asString());
        if (!isDataFrame) {
            logger.warn("The R object in the input is not a data-frame. Only data frames can be converted into Knime tables. Trying casting to data.frame...");

            REXP xp = connection.parseAndEval("try(" + RSnippetNodeModel.R_INVAR_BASE_NAME + " <- as.data.frame(" + RSnippetNodeModel.R_INVAR_BASE_NAME + " ))");

            if (xp.inherits("try-error")) {
                throw new RuntimeException("Casting to data-frame failed. You need to change your script to return a data-structure that is a data-frame or can be coerced into one. ");
            }
        }


        //3) convert the data frame back into a knime table
        REXP rexp = connection.eval(RSnippetNodeModel.R_INVAR_BASE_NAME);
        String[] rowNames = connection.eval("rownames(" + RSnippetNodeModel.R_INVAR_BASE_NAME + ")").asStrings();
        
        dataTable = RUtils.convert2DataTable(exec, rexp, null);

        connection.voidEval("rm(list = ls(all = TRUE));");
        } catch(Exception e) {
        	connection.close();
        	throw e;
        }
        connection.close();

        return new BufferedDataTable[]{dataTable};
    }


    @Override
    // note: This is not the usual configure but a more generic one with PortObjectSpec instead of DataTableSpec
    protected PortObjectSpec[] configure(final PortObjectSpec[] inSpecs) throws InvalidSettingsException {
        return new PortObjectSpec[1];
    }


    @Override
    protected BufferedDataTable[] execute(BufferedDataTable[] inData, ExecutionContext exec) throws Exception {
        throw new RuntimeException("fake implementation: This method should be never called");
    }


	@Override
	protected PortObject[] executeImpl(PortObject[] inData, ExecutionContext exec) throws Exception {
		// TODO Auto-generated method stub
		return null;
	}


	@Override
	protected void openIn(PortObject[] inData, ExecutionContext exec) throws KnimeScriptingException {
		// TODO Auto-generated method stub
		
	}
}
