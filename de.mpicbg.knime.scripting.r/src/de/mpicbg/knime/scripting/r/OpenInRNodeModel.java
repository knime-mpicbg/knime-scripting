package de.mpicbg.knime.scripting.r;

import de.mpicbg.knime.knutils.Utils;
import de.mpicbg.knime.scripting.core.AbstractTableScriptingNodeModel;
import de.mpicbg.knime.scripting.r.prefs.RPreferenceInitializer;

import org.eclipse.jface.preference.IPreferenceStore;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.ExecutionContext;
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
 */
public class OpenInRNodeModel extends AbstractTableScriptingNodeModel {


    /**
     * Constructor for the node model.
     */
    protected OpenInRNodeModel() {
        super(3, 0, 2, 3);
    }


    /**
     * {@inheritDoc}
     */
    @Override
    protected BufferedDataTable[] execute(final BufferedDataTable[] inData,
                                          final ExecutionContext exec) throws Exception {

        try {

            logger.info("Creating R-connection");
            RConnection connection = RUtils.createConnection();

            // 1) convert exampleSet ihnto data-frame and put into the r-workspace
            logger.info("Pushing inputs to R...");

            Map<String, Object> pushTable = RUtils.pushToR(inData, connection, exec);

            // save the work-space to a temporary file and open R

            String allParams = pushTable.keySet().toString().replace("[", "").replace("]", "").replace(" ", "");

            connection.voidEval("tmpwfile = tempfile('openinrnode', fileext='.RData');");
            connection.voidEval("save(" + allParams + ", file=tmpwfile); ");


            // 2) transfer the file to the local computer if necessary
            logger.info("Transferring workspace-file to localhost ...");

            File workspaceFile = null;
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
            connection.close();

            logger.info("Spawning R-instance ...");
            openWSFileInR(workspaceFile, prepareScript());


            setWarningMessage("To push the node's input to R again, you need to reset and re-execute it.");

        } catch (Throwable e) {
            throw new RuntimeException("spawning of R-process failed", e);
        }

        return new BufferedDataTable[0];
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
     * Attempts to find an upper bound of the number of values of node input.
     * USAGE IN 'execute' method HAS BEEN REPLACED BY AN R-COMMAND - CAN BE DELETED AT SOME POINT
     */
    /* private int estimateNumValues(BufferedDataTable[] pushTable) {

        int inputSize = 0;

        for (BufferedDataTable inputTable : pushTable) {
            if (inputTable != null) {
                inputSize += calcTableSize(inputTable);
            }
        }


        return (int) (10.1 * inputSize); // add some size for meta data like table headers

    } */


    /* private int calcTableSize(BufferedDataTable bufferedDataTable) {
        return bufferedDataTable.getDataTableSpec().getNumColumns() * bufferedDataTable.getRowCount();
    } */
}