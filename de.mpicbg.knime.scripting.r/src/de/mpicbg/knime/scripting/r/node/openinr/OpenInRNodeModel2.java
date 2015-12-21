package de.mpicbg.knime.scripting.r.node.openinr;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Map;

import org.eclipse.jface.preference.IPreferenceStore;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.defaultnodesettings.SettingsModelIntegerBounded;
import org.knime.core.node.port.PortObject;
import org.rosuda.REngine.REXP;
import org.rosuda.REngine.REXPMismatchException;
import org.rosuda.REngine.REngineException;
import org.rosuda.REngine.Rserve.RConnection;

import de.mpicbg.knime.knutils.Utils;
import de.mpicbg.knime.scripting.core.AbstractTableScriptingNodeModel;
import de.mpicbg.knime.scripting.core.exceptions.KnimeScriptingException;
import de.mpicbg.knime.scripting.r.R4KnimeBundleActivator;
import de.mpicbg.knime.scripting.r.RUtils;
import de.mpicbg.knime.scripting.r.prefs.RPreferenceInitializer;


/**
 * This is the model implementation of OpenInR.
 *
 * @author Antje Janosch (MPI-CBG)
 */
public class OpenInRNodeModel2 extends AbstractTableScriptingNodeModel {


    /**
     * Constructor for the node model.
     */
    protected OpenInRNodeModel2() {
        super(true, 3, 0, 2, 3);
    }

    /**
     * {@inheritDoc}
     * @throws KnimeScriptingException 
     */
	@Override
	protected PortObject[] executeImpl(PortObject[] inData,
			ExecutionContext exec) throws KnimeScriptingException  {
        
			exec.setMessage("Creating R-connection");
            RConnection connection = RUtils.createConnection();
            
            try {
            	transferToR(castToBDT(inData), exec, connection);
            } catch (KnimeScriptingException e) {
    			if(connection.isConnected()) {
    				connection.close();
    				throw e;
    			}
    		}

            setWarningMessage("To push the node's input to R again, you need to reset and re-execute it.");
       
        return new BufferedDataTable[0];
	}

	/**
	 * execute node
	 * @param castToBDT
	 * @param exec
	 * @param connection
	 * @throws KnimeScriptingException
	 */
    private void transferToR(BufferedDataTable[] castToBDT, ExecutionContext exec, RConnection connection) throws KnimeScriptingException {
    	
        exec.setMessage("Pushing inputs to R...");
        File workspaceFile = null;
        
        // retrieve chunk settings and push input data to R
        int chunkIn = ((SettingsModelIntegerBounded) this.getModelSetting(CHUNK_IN)).getIntValue();
        int chunkInSize = RUtils.getChunkIn(chunkIn, castToBDT);
        Map<String, Object> pushTable = RUtils.pushToR(castToBDT, connection, exec, chunkInSize);

        String allParams = pushTable.keySet().toString().replace("[", "").replace("]", "").replace(" ", "");

        // save R workspace to disk and transfer to localhost
        exec.setMessage("Write workspace to disk (Cannot be cancelled)");
        try {
        	// save the work-space to a temporary file
        	connection.voidEval("tmpwfile = tempfile('openinrnode', fileext='.RData');");
        	connection.voidEval("save(" + allParams + ", file=tmpwfile); ");

        	// transfer the file to the local computer if necessary
        	logger.info("Transferring workspace-file to localhost ...");

        	if (RUtils.getHost().equals("localhost")) {
        		workspaceFile = new File(connection.eval("tmpwfile").asString());
        	} else {
        		// create another local file  and transfer the workspace file from the rserver
        		workspaceFile = File.createTempFile("rplugin", ".RData");
        		workspaceFile.deleteOnExit();

        		REXP xp = connection.parseAndEval("r=readBin(tmpwfile,'raw',file.info(tmpwfile)$size); unlink(tmpwfile); r");
        		FileOutputStream oo = new FileOutputStream(workspaceFile);
        		oo.write(xp.asBytes());
        		oo.close();
        	}

        	connection.voidEval("rm(list = ls(all = TRUE));");

        } catch (REXPMismatchException | IOException | REngineException e) {
        	connection.close();
        	e.printStackTrace();
        	throw new KnimeScriptingException("Failure in handling temparary workspace file: " + e.getMessage());
        }
        exec.setProgress(0.9);

        connection.close();
        
        // spawn R instance and load workspace
        exec.setMessage("Spawning R-instance ...");
        try {
			openWSFileInR(workspaceFile);
		} catch (IOException e) {
			e.printStackTrace();
			throw new KnimeScriptingException("Spawning of R-process failed: " + e.getMessage());
		}
        exec.setProgress(1.0);
	}
    
    /**
     * spawn new R process and load workspace file
     * @param workspaceFile
     * @param script
     * @throws IOException
     */
    public static void openWSFileInR(File workspaceFile) throws IOException {
        IPreferenceStore prefStore = R4KnimeBundleActivator.getDefault().getPreferenceStore();
        String rExecutable = prefStore.getString(RPreferenceInitializer.LOCAL_R_PATH);

        // 3) spawn a new R process
        if (Utils.isWindowsPlatform()) {
            Runtime.getRuntime().exec(rExecutable + " " + workspaceFile.getAbsolutePath());
        } else if (Utils.isMacOSPlatform()) {

            Runtime.getRuntime().exec("open -n -a " + rExecutable + " " + workspaceFile.getAbsolutePath());

        } else { // linux and the rest of the world
            Runtime.getRuntime().exec(rExecutable + " " + workspaceFile.getAbsolutePath());
        }
    }


	/**
     * {@inheritDoc}
     * @throws KnimeScriptingException 
     */
	@Override
	protected void openIn(PortObject[] inData, ExecutionContext exec)
			throws KnimeScriptingException {
		// nothing to do
	}
}