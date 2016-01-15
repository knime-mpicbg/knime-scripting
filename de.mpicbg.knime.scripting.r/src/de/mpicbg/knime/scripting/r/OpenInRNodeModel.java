package de.mpicbg.knime.scripting.r;

import de.mpicbg.knime.knutils.Utils;
import de.mpicbg.knime.scripting.core.AbstractScriptingNodeModel;
import de.mpicbg.knime.scripting.core.exceptions.KnimeScriptingException;
import de.mpicbg.knime.scripting.r.node.openinr.OpenInRNodeModel2;
import de.mpicbg.knime.scripting.r.prefs.RPreferenceInitializer;

import org.eclipse.jface.preference.IPreferenceStore;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.port.PortObject;
import org.rosuda.REngine.REXP;
import org.rosuda.REngine.Rserve.RConnection;

import java.awt.*;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Map;


/**
 * This is the model implementation of RSnippet. Improved R Integration for Knime
 *
 * @author Holger Brandl (MPI-CBG)
 * @deprecated use {@link OpenInRNodeModel2} instead.
 */
public class OpenInRNodeModel extends AbstractRScriptingNodeModel {


    /**
     * Constructor for the node model.
     */
    protected OpenInRNodeModel() {
        super(createPorts(3, 2, 3), createPorts(0), new RColumnSupport());
    }


    public static void openWSFileInR(File workspaceFile, String script) throws IOException {
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

        // copy the script in the clipboard
        if (!script.isEmpty()) {
            StringSelection data = new StringSelection(script);
            Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
            clipboard.setContents(data, data);
        }
    }


    /**
     * {@inheritDoc}
     */
	@Override
	protected PortObject[] executeImpl(PortObject[] inData,
			ExecutionContext exec) throws Exception {
        /*try {

            logger.info("Creating R-connection");
            RConnection connection = RUtils.createConnection();

            // 1) convert exampleSet ihnto data-frame and put into the r-workspace
            logger.info("Pushing inputs to R...");
            File workspaceFile = null;
            
            try {

            Map<String, Object> pushTable = pushToR(inData, connection, exec, AbstractScriptingNodeModel.CHUNK_IN_DFT);

            // save the work-space to a temporary file and open R

            String allParams = pushTable.keySet().toString().replace("[", "").replace("]", "").replace(" ", "");

            connection.voidEval("tmpwfile = tempfile('openinrnode', fileext='.RData');");
            connection.voidEval("save(" + allParams + ", file=tmpwfile); ");

            // 2) transfer the file to the local computer if necessary
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
            
            } catch(Exception e) {
            	connection.close();
            	throw e;
            }
            connection.close();

            logger.info("Spawning R-instance ...");
            openWSFileInR(workspaceFile, prepareScript());


            setWarningMessage("To push the node's input to R again, you need to reset and re-execute it.");

        } catch (Throwable e) {
            throw new RuntimeException("spawning of R-process failed", e);
        }
*/
        return new BufferedDataTable[0];
	}

    /**
     * {@inheritDoc}
     * @throws KnimeScriptingException 
     */
	@Override
	protected void openIn(PortObject[] inData, ExecutionContext exec)
			throws KnimeScriptingException {
		try {
			executeImpl(inData, exec);
		} catch (Exception e) {
			throw new KnimeScriptingException(e.getMessage());
		}
	}
}