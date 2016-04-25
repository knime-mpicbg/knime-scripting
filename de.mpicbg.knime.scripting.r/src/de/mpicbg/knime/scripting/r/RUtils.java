package de.mpicbg.knime.scripting.r;

import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

import org.eclipse.jface.preference.IPreferenceStore;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnDomainCreator;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataType;
import org.knime.core.data.IntValue;
import org.knime.core.data.RowKey;
import org.knime.core.data.def.DefaultRow;
import org.knime.core.data.def.DoubleCell;
import org.knime.core.data.def.IntCell;
import org.knime.core.data.def.StringCell;
import org.knime.core.node.BufferedDataContainer;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.ExecutionContext;
import org.rosuda.REngine.REXP;
import org.rosuda.REngine.REXPInteger;
import org.rosuda.REngine.REXPLogical;
import org.rosuda.REngine.REXPMismatchException;
import org.rosuda.REngine.REXPString;
import org.rosuda.REngine.REXPVector;
import org.rosuda.REngine.REngineException;
import org.rosuda.REngine.RList;
import org.rosuda.REngine.Rserve.RConnection;
import org.rosuda.REngine.Rserve.RserveException;

import de.mpicbg.knime.knutils.Utils;
import de.mpicbg.knime.scripting.core.exceptions.KnimeScriptingException;
import de.mpicbg.knime.scripting.r.data.RDataFrameContainer;
import de.mpicbg.knime.scripting.r.node.snippet.RSnippetNodeModel;
//import de.mpicbg.knime.scripting.r.generic.RPortObject;
import de.mpicbg.knime.scripting.r.prefs.RPreferenceInitializer;


/**
 * Document me!
 *
 * @author Holger Brandl
 */
public class RUtils {

    public static int MAX_FACTOR_LEVELS = 500;

    /**
     * @deprecated
     * @param exec
     * @param rexp
     * @param typeMapping
     * @return
     */
    public static BufferedDataTable convert2DataTable(ExecutionContext exec, REXP rexp, Map<String, DataType> typeMapping) {
        try {
            RList rList = rexp.asList();


            // create attributes
            DataColumnSpec[] colSpecs = new DataColumnSpec[rList.keySet().size()];
            Map<DataColumnSpec, REXPVector> columnData = new HashMap<DataColumnSpec, REXPVector>();
            Map<DataColumnSpec, Integer> colIndices = new HashMap<DataColumnSpec, Integer>();

            ArrayList colKeys = new ArrayList(Arrays.asList(rList.keys()));

//            long timeBefore = System.currentTimeMillis();


            for (int attrCounter = 0; attrCounter < colKeys.size(); attrCounter++) {
                Object columnKey = colKeys.get(attrCounter);
                REXPVector column = (REXPVector) rList.get(columnKey);

                assert columnKey instanceof String : " key is not a string";
                String columnName = columnKey.toString();


                if (column.isFactor()) {
                    colSpecs[attrCounter] = new DataColumnSpecCreator(columnName, StringCell.TYPE).createSpec();

                } else if (column.isNumeric()) {
                    DataType numColType = DoubleCell.TYPE;
                    if (typeMapping != null && typeMapping.containsKey(columnName)) {
                        numColType = typeMapping.get(columnName);
                    }

                    colSpecs[attrCounter] = new DataColumnSpecCreator(columnName, numColType).createSpec();

                } else if (column.isString()) {
                    colSpecs[attrCounter] = new DataColumnSpecCreator(columnName, StringCell.TYPE).createSpec();

                } else if (column.isLogical()) {
                    colSpecs[attrCounter] = new DataColumnSpecCreator(columnName, IntCell.TYPE).createSpec();
                    //                    colSpecs[specCounter] = new DataColumnSpecCreator(columnName, LogicalCell.TYPE).createSpec();

                } else {
                    throw new RuntimeException("column type not supported");
                }

                DataColumnSpec curSpecs = colSpecs[attrCounter];

                columnData.put(curSpecs, column);
                colIndices.put(curSpecs, attrCounter);
            }

//            NodeLogger.getLogger(RUtils.class).warn(rexp.toString() + "- Time between (R->knime) 1: " + (double) (System.currentTimeMillis() - timeBefore) / 1000);


            // create examples
            DataTableSpec outputSpec = new DataTableSpec(colSpecs);
            BufferedDataContainer container = exec.createDataContainer(outputSpec);
            int numExamples = columnData.isEmpty() ? -1 : columnData.values().iterator().next().length();

            DataCell[][] cells = new DataCell[numExamples][colIndices.size()];

            List<DataColumnSpec> domainSpecs = new ArrayList<DataColumnSpec>();

            for (DataColumnSpec colSpec : colSpecs) {
                REXPVector curColumn = columnData.get(colSpec);
                int colIndex = colIndices.get(colSpec);

                DataColumnSpecCreator specCreator = new DataColumnSpecCreator(colSpec.getName(), colSpec.getType());
                boolean[] isNA = curColumn.isNA();


                if (curColumn.isString() || curColumn.isFactor()) {
                    String[] stringColumn = curColumn.asStrings();

                    LinkedHashSet<DataCell> domain = new LinkedHashSet<DataCell>();

                    for (int i = 0; i < numExamples; i++) {
                        if (isNA[i] || stringColumn[i].equals(RDataFrameContainer.NA_VAL_FOR_R)) {
                            cells[i][colIndex] = DataType.getMissingCell();

                        } else {
                            StringCell stringCell = new StringCell(stringColumn[i]);
                            updateDomain(domain, stringCell);

                            cells[i][colIndex] = stringCell;
                        }
                    }

                    if (domain.size() < MAX_FACTOR_LEVELS) {
                        specCreator.setDomain(new DataColumnDomainCreator(domain).createDomain());
                    }

                } else if (colSpec.getType().isCompatible(IntValue.class) || curColumn.isLogical()) {    // there's no boolean-cell-type in knime, thus we use int
                    int[] intColumn = curColumn.asIntegers();

                    for (int i = 0; i < numExamples; i++) {
                        cells[i][colIndex] = isNA[i] ? DataType.getMissingCell() : new IntCell(intColumn[i]);
                    }

                } else if (curColumn.isNumeric()) {
                    double[] doubleColumn = curColumn.asDoubles();

                    for (int i = 0; i < numExamples; i++) {
                        cells[i][colIndex] = isNA[i] ? DataType.getMissingCell() : new DoubleCell(doubleColumn[i]);
                    }


                } else {
                    throw new RuntimeException("Unexpected type data-frame that has been received from R: " + colSpec.getName());
                }

                //  maybe we have to treat other types here

                domainSpecs.add(specCreator.createSpec());
            }


//            NodeLogger.getLogger(RUtils.class).warn(rexp.toString() + "- Time danach (R->knime) 2: " + (double) (System.currentTimeMillis() - timeBefore) / 1000);


            for (int i = 0; i < numExamples; i++) {
                RowKey key = new RowKey("" + i);

                DataRow row = new DefaultRow(key, cells[i]);
                container.addRowToTable(row);
            }

//            NodeLogger.getLogger(RUtils.class).warn(rexp.toString() + "- Time danach (R->knime) 3: " + (double) (System.currentTimeMillis() - timeBefore) / 1000);


//            exec.checkCanceled();
//            exec.setProgress(i / (double)m_count.getIntValue(), "Adding row " + i);

//        // once we are done, we close the container and return its table
            container.close();

            DataTableSpec domainDataTableSpec = new DataTableSpec(domainSpecs.toArray(new DataColumnSpec[domainSpecs.size()]));

            return exec.createSpecReplacerTable(container.getTable(), domainDataTableSpec);
        } catch (REXPMismatchException e) {
            throw new RuntimeException(e);
        }

    }

    /**
     * @deprecated
     * @param domain
     * @param stringCell
     */
    private static void updateDomain(LinkedHashSet<DataCell> domain, StringCell stringCell) {
        if (!stringCell.isMissing() && domain.size() < MAX_FACTOR_LEVELS) {
            domain.add(stringCell);
        }
    }

    /**
     * create new connection to R server
     * @return
     * @throws KnimeScriptingException
     */
    public static RConnection createConnection() throws KnimeScriptingException {

        String host = getHost();
        int port = getPort();
        
        try {
            return new RConnection(host, port);
        } catch (RserveException e) {
        	e.printStackTrace();
        	throw new KnimeScriptingException("Could not connect to R. Probably, the R-Server is not running.\nHere's what you need to do:\n 1) Check what R-server-host is configured in your Knime preferences.\n 2) If your host is set to be 'localhost' start R and run the following command\n library(Rserve); Rserve(args = \"--vanilla\")");
        }
    }

    /**
     * @return host setting from R-scripting preferences
     */
    public static String getHost() {
        return R4KnimeBundleActivator.getDefault().getPreferenceStore().getString(RPreferenceInitializer.R_HOST);
    }

    /**
     * @return port setting from R-scripting preferences
     */
    public static int getPort() {
        return R4KnimeBundleActivator.getDefault().getPreferenceStore().getInt(RPreferenceInitializer.R_PORT);
    }

    /**
     * @deprecated
     * @param varFileMapping
     * @param connection
     * @throws RserveException
     * @throws REXPMismatchException
     * @throws IOException
     */
    public static void loadGenericInputs(Map<String, File> varFileMapping, RConnection connection) throws RserveException, REXPMismatchException, IOException {

    	for (String varName : varFileMapping.keySet()) {

            // summary of happens below: create a copy of the current workspace file in r, load it in r and rename the content to match the current nodes connectivity pattern

            // mirror the ws-file on the server side
            connection.voidEval("tmpwfile <- 'tempRws';");
            connection.voidEval("file.create(tmpwfile);");
            File serverWSFile = new File(connection.eval("tmpwfile").asString());

            writeFile(varFileMapping.get(varName), serverWSFile, connection);

            // load the workspace on the server side
            connection.voidEval("load(tmpwfile);");

            // rename the input structure to match the current environment ones

            // rename kIn if file is still using the old naming scheme
            List<String> wsVariableNames = Arrays.asList(connection.eval("ls()").asStrings());

            if (wsVariableNames.contains("R")) {
                // rename the old R-variable to the new format
                connection.voidEval(RSnippetNodeModel.R_OUTVAR_BASE_NAME + " <- R;");
            }

            // if its the plotting node the input is still names kIn, so just rename if there no such variable already present
            if (!wsVariableNames.contains(varName)) {
                connection.eval(varName + " <- " + RSnippetNodeModel.R_OUTVAR_BASE_NAME + ";");

                // do some cleanup
                connection.voidEval("rm(" + RSnippetNodeModel.R_OUTVAR_BASE_NAME + ");");
            }
        }
    }

    /**
     * write local workspace to remote workspace
     * @param wsFile
     * @param serverWSFile
     * @param connection
     */
    private static void writeFile(File wsFile, File serverWSFile, RConnection connection) {
        try {
            assert wsFile.isFile();

            InputStream is = new BufferedInputStream(new FileInputStream(wsFile));
            OutputStream os = new BufferedOutputStream(connection.createFile(serverWSFile.getPath()));

            byte[] buf = new byte[1024];
            int len;
            while ((len = is.read(buf)) > 0) {
                os.write(buf, 0, len);
            }

            is.close();
            os.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * {@deprecated}
     * @param rWorkspaceFile
     * @param connection
     * @param host
     * @param objectNames
     * @throws RserveException
     * @throws IOException
     * @throws REXPMismatchException
     * @throws REngineException
     * @throws KnimeScriptingException
     */
    public static void saveToLocalFile(File rWorkspaceFile, RConnection connection, String host, String... objectNames) 
    		throws RserveException, IOException, REXPMismatchException, REngineException, KnimeScriptingException {

        connection.voidEval("tmpwfile = tempfile('tempRws');");
        
        //check whether named objects exist in R workspace
        for(int i = 0; i < objectNames.length; i++) {
        	String evalExistense = "exists(\"" + objectNames[i] + "\")";
        	if(((REXPLogical)connection.eval(evalExistense)).isFALSE()[0])
        		throw new KnimeScriptingException("Try to save object " + objectNames[i] + ". Object does not exist in R workspace.");
        }

        String allParams = Arrays.toString(objectNames).replace("[", "").replace("]", "").replace(" ", "");
        connection.voidEval("save(" + allParams + ", file=tmpwfile); ");


        // 2) transfer the file to the local computer if necessary
//        logger.info("Transferring workspace-file to localhost ...");


        // to furthe improve performance we simply copy the file on a local host
        if (host != null && host.equals("localhost")) {
            File localRWSFile = new File(connection.eval("tmpwfile").asString());
             copyFile(localRWSFile, rWorkspaceFile);

        } else {
            // create the local file and transfer the workspace file from the rserver

            if (rWorkspaceFile.isFile()) {
                rWorkspaceFile.delete();
            }

            rWorkspaceFile.createNewFile();

            REXP xp = connection.parseAndEval("r=readBin(tmpwfile,'raw'," + 1E7 + "); unlink(tmpwfile); r");
            FileOutputStream oo = new FileOutputStream(rWorkspaceFile);
            oo.write(xp.asBytes());
            oo.close();
        }

        //remove the temporary workspace file
        connection.voidEval("rm(tmpwfile);");

    }

    /**
     * {@deprecated} 
     * use {@link Files.copy(...)} instead
     * @param sourceFile
     * @param destFile
     * @throws IOException
     */
    public static void copyFile(File sourceFile, File destFile) throws IOException {
        if (!destFile.exists()) {
            destFile.createNewFile();
        }

        FileChannel source = null;
        FileChannel destination = null;
        try {
            source = new FileInputStream(sourceFile).getChannel();
            destination = new FileOutputStream(destFile).getChannel();
            destination.transferFrom(source, 0, source.size());
        } finally {
            if (source != null) {
                source.close();
            }
            if (destination != null) {
                destination.close();
            }
        }
    }










    

    /**
     * {@deprecated}
     * @param script
     * @return
     * @throws RserveException
     */
    public static String supportOldVarNames(String script) throws RserveException {
        if (script.contains("(R)") ||
                script.contains("R[") ||
                script.contains("R$") ||
                script.contains("<- R") ||
                script.contains("R <-")) {

            String renamePrefix = "R <- " + RSnippetNodeModel.R_INVAR_BASE_NAME;
            String renameSuffix = RSnippetNodeModel.R_OUTVAR_BASE_NAME + " <- R";

            return renamePrefix + "; " + script + "\n" + renameSuffix;
        }

        return script;
    }

	/**
	 * assumes in R workspace an objects resulting from 'evaluate'-function call
	 * retrieves a list of error messages from this object
	 * 
	 * @param connection
	 * @return list with error messages
	 * @throws RserveException
	 * @throws REXPMismatchException
	 */
	public static ArrayList<String> checkForErrors(RConnection connection) 
			throws RserveException, REXPMismatchException {
		
		ArrayList<String> errorMessages = new ArrayList<String>();
		if (((REXPLogical) connection.eval("exists(\"knime.eval.obj\")")).isFALSE()[0])
			return errorMessages;
		
		// check for errors
    	int[] errIdx = ((REXPInteger) connection.eval("which(sapply(knime.eval.obj, inherits, \"error\") == TRUE, arr.ind = TRUE)")).asIntegers();
    	if(errIdx.length > 0) {
    		for(int i=0; i < errIdx.length; i++){
    			REXPString error = (REXPString) connection.eval("knime.eval.obj[[" + errIdx[i] + "]]$message");
    			errorMessages.add(error.asString());
    		}
    	}
    	return errorMessages;
	}

	/**
	 * assumes in R workspace an objects resulting from 'evaluate'-function call
	 * retrieves a list of error messages from this object
	 * 
	 * @param connection
	 * @return list with warning messages
	 * @throws RserveException
	 * @throws REXPMismatchException
	 */
	public static ArrayList<String> checkForWarnings(RConnection connection) 
			throws RserveException, REXPMismatchException {
		
		ArrayList<String> warnMessages = new ArrayList<String>();
		if (((REXPLogical) connection.eval("exists(\"knime.eval.obj\")")).isFALSE()[0])
			return warnMessages;
		
		//check for warnings
    	int[] warnIdx = ((REXPInteger) connection.eval("which(sapply(knime.eval.obj, inherits, \"warning\") == TRUE, arr.ind = TRUE)")).asIntegers();
    	if(warnIdx.length > 0) {
    		for(int i=0; i < warnIdx.length; i++){
    			String singleWarning;
    			REXPString warn = (REXPString) connection.eval("deparse(knime.eval.obj[[" + warnIdx[i] + "]]$call)");
    			singleWarning = warn.asString() + " : ";
    			warn = (REXPString) connection.eval("knime.eval.obj[[" + warnIdx[i] + "]]$message");
    			singleWarning = singleWarning + warn.asString() + "\n";
    			warnMessages.add(singleWarning);
    		}
    	}
    	return warnMessages;
	}
	

	/**
	 * run the actual external call to open R with Knime data
	 * @param workspaceFile
	 * @param script
	 * @throws IOException
	 */
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
	 * save R workspace to file
	 * @param rWorkspaceFile needs to have '/' as folder separator
	 * @param connection
	 * @param host
	 * @throws KnimeScriptingException 
	 */
	public static void saveWorkspaceToFile(File rWorkspaceFile, RConnection connection, String host) throws KnimeScriptingException 
	{
		assert host != null;
		// (Do not create new R objects in workspace before saving!)

		if(host.equals("localhost") || host.equals("127.0.0.1")) {
			// save workspace to local file
			try {
				connection.voidEval("save.image(file=\"" + rWorkspaceFile.getAbsolutePath().replace("\\", "/") + "\")");
			} catch (RserveException e) {
				throw new KnimeScriptingException("Failed to save R workspace: " + e.getMessage());
			}
		} else {
			// create temporary file name on server side 
			String tempfile = null;
			try {
				tempfile = ((REXPString) connection.eval("tempfile(pattern = \"R-ws-\");")).asString();
				tempfile = tempfile.replace("\\", "\\\\");
				connection.voidEval("unlink(\"" + tempfile + "\")");
				// save R workspace 
				connection.voidEval("save.image(file=\"" + tempfile + "\")");
			} catch (RserveException | REXPMismatchException e) {
				throw new KnimeScriptingException("Failed to save R workspace: " + e.getMessage());
			}

			// if the file already exists, delete it
			if (rWorkspaceFile.isFile())
				rWorkspaceFile.delete();
			try {
				rWorkspaceFile.createNewFile();
			} catch (IOException e) {
				throw new KnimeScriptingException("Failed to create workspace file: " + e.getMessage());
			}

			// get binary representation of remote workspace file, delete it and write bytes to local
			REXP xp;
			try {
				System.out.println("write process");
				xp = connection.parseAndEval(
						"r=readBin(\"" + tempfile + "\",'raw',file.size(\"" + tempfile + "\")); unlink(\"" + tempfile + "\"); r");
				FileOutputStream oo = new FileOutputStream(rWorkspaceFile);
				connection.voidEval("unlink(\"" + tempfile + "\")");
				oo.write(xp.asBytes());
				oo.close();
			} catch (REngineException | REXPMismatchException | IOException e) {
				throw new KnimeScriptingException("Faile to transfer workspace file to localhost: " + e.getMessage());
			}
		}
	}

	/**
	 * loads R workspace data into R session
	 * @param workspaceFile
	 * @param connection
	 * @throws KnimeScriptingException
	 */
	public static void loadWorkspace(File workspaceFile, RConnection connection) 
			throws KnimeScriptingException {
		// (Do not create new R objects in workspace before loading!)
		
		// create temporary workspace file on server side
		File serverWSFile = null;
		String fileName = null;
		try {
			fileName = ((REXPString) connection.eval("tempfile(pattern = \"R-ws-\")")).asString();
			fileName = fileName.replace("\\", "\\\\");
			connection.voidEval("file.create(\"" + fileName + "\")");
			serverWSFile = new File(fileName);
		} catch (RserveException | REXPMismatchException e) {
			throw new KnimeScriptingException("Failed to create temporary workspace file on server side: " + e.getMessage());
		}

        // transfer workspace from local to remote
        writeFile(workspaceFile, serverWSFile, connection);

        // load the workspace on the server side within a new environment
        try {
			connection.voidEval("load(\"" + fileName + "\")");
			connection.voidEval("unlink(\"" + fileName + "\")");
		} catch (RserveException e) {
			throw new KnimeScriptingException("Failed to load the workspace: " + e.getMessage());
		}
	}
	
}