package de.mpicbg.knime.scripting.python;

import de.mpicbg.knime.knutils.Utils;
import de.mpicbg.knime.scripting.core.AbstractScriptingNodeModel;
import de.mpicbg.knime.scripting.core.exceptions.KnimeScriptingException;
import de.mpicbg.knime.scripting.core.rgg.TemplateUtils;
import de.mpicbg.knime.scripting.python.prefs.PythonPreferenceInitializer;
import de.mpicbg.knime.scripting.python.scripts.PythonScripts;
import de.mpicbg.knime.scripting.python.srv.LocalPythonClient;
import de.mpicbg.knime.scripting.python.srv.Python;
import de.mpicbg.knime.scripting.python.srv.PythonTempFile;
import org.eclipse.jface.preference.IPreferenceStore;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.port.PortObject;
import org.knime.core.node.port.PortType;

import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.io.*;


public abstract class AbstractPythonScriptingNodeModel extends AbstractScriptingNodeModel {
    // Temp files for reading/writing the table and the script
    protected PythonTempFile kInFile;
    protected PythonTempFile pyOutFile;
    protected PythonTempFile scriptFile;

    protected Python python;

    protected IPreferenceStore preferences = PythonScriptingBundleActivator.getDefault().getPreferenceStore();

    protected AbstractPythonScriptingNodeModel(PortType[] inPorts, PortType[] outports) {
        super(inPorts, outports, new PythonColumnSupport());
    }

    protected void prepareScript(Writer writer, boolean useScript) throws IOException {
        // CSV read/write functions
        InputStream utilsStream = PythonScripts.class.getResourceAsStream("PythonCSVUtils.py");

        String readCSVCmd = kInFile != null ? "kIn = read_csv(r\"" + kInFile.getServerPath() + "\", True)" : "";
        String writeCSVCmd = pyOutFile != null ? "write_csv(r\"" + pyOutFile.getServerPath() + "\", pyOut, True)" : "";

        // Write the script file
        writer.write(TemplateUtils.convertStreamToString(utilsStream));
        writer.write("\n" + readCSVCmd + "\n");

        // Insert the user-defined script here
        if(useScript)
        	writer.write("\n" + super.prepareScript() + "\n");

        writer.write("\n" + writeCSVCmd + "\n");

        return;
    }

    @Override
    public String prepareScript() throws RuntimeException {
        try {
            Writer writer = new BufferedWriter(new FileWriter(scriptFile.getClientFile()));
            try {
                prepareScript(writer, true);
            } finally {
                writer.close();
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        scriptFile.upload();
        return scriptFile.getServerPath();
    }

    /**
     * Create necessary temp files
     */
    protected void createTempFiles() throws RuntimeException {
        try {
            // Delete the previous set if they're still around
            deleteTempFiles();

            // Create a new set
            kInFile = new PythonTempFile(python, "knime2python", ".csv");
            pyOutFile = new PythonTempFile(python, "python2knime", ".csv");
            scriptFile = new PythonTempFile(python, "analyze", ".py");
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Delete all temp files if they exist and the node is so configured
     */
    protected void deleteTempFiles() {
        if (kInFile != null) kInFile.delete();
        if (pyOutFile != null) pyOutFile.delete();
        if (scriptFile != null) scriptFile.delete();
    }

    /**
     * opens python externally and loads KNIME input data, script is put into clipboard
     * @param inData
     * @param exec
     * @param logger
     * @throws KnimeScriptingException
     */
    protected void openInPython(PortObject[] inData, ExecutionContext exec, NodeLogger logger) throws KnimeScriptingException {
    	IPreferenceStore preferences = PythonScriptingBundleActivator.getDefault().getPreferenceStore();

    	//      boolean local = preferences.getBoolean(PythonPreferenceInitializer.PYTHON_LOCAL);
    	//      if (!local) throw new RuntimeException("This node can only be used with a local python executable");

    	python = new LocalPythonClient();

    	createTempFiles();
    	pyOutFile = null;

    	// Write data into csv
    	BufferedDataTable[] inTables = castToBDT(inData);
    	logger.info("Writing table to CSV file");
    	PythonTableConverter.convertTableToCSV(exec, inTables[0], kInFile.getClientFile(), logger);

    	// Create and execute script
    	String pythonExecPath = preferences.getString(PythonPreferenceInitializer.PYTHON_EXECUTABLE);

    	// get the full path of the python executable for MacOS
    	String pythonExecPathFull = pythonExecPath;
    	try {
    		if (Utils.isMacOSPlatform()) {
    			Runtime r = Runtime.getRuntime();
    			Process p = r.exec("which " + pythonExecPath);
    			p.waitFor();
    			BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream()));
    			pythonExecPathFull = reader.readLine();
    		}
    	} catch (Exception e) {
    		logger.error(e);
    	}

    	try {
    		Writer writer = new BufferedWriter(new FileWriter(scriptFile.getClientFile()));
    		try {
    			// Write a shebang to invoke the python interpreter 
    			writer.write("#! " + pythonExecPathFull + " -i\n");
    			prepareScript(writer, false);
    		} finally {
    			writer.close();
    		}

    		scriptFile.getClientFile().setExecutable(true);

    		// Run the script
    		if (Utils.isMacOSPlatform()) {
    			Runtime.getRuntime().exec("open -a Terminal " + " " + scriptFile.getClientPath());
    		} else if (Utils.isWindowsPlatform()) {
    			Runtime.getRuntime().exec(new String[] {
    					"cmd",
    					"/k",
    					"start",
    					pythonExecPath,
    					"-i",
    					"\"" + scriptFile.getClientPath() + "\""
    			});
    		} else logger.error("Unsupported platform");
    		
    		// copy the script in the clipboard
    		String actualScript = super.prepareScript();
            if (!actualScript.isEmpty()) {
                StringSelection data = new StringSelection(actualScript);
                Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
                clipboard.setContents(data, data);
            }
    		
    	} catch (Exception e) {
    		throw new KnimeScriptingException("Failed to open in Python\n" + e);
    	}
    }
}
