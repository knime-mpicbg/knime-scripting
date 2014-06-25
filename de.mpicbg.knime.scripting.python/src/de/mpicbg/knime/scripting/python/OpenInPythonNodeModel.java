package de.mpicbg.knime.scripting.python;

import de.mpicbg.knime.knutils.Utils;
import de.mpicbg.knime.scripting.python.prefs.PythonPreferenceInitializer;
import de.mpicbg.knime.scripting.python.srv.LocalPythonClient;
import org.eclipse.jface.preference.IPreferenceStore;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.port.PortType;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.InputStreamReader;
import java.io.Writer;


public class OpenInPythonNodeModel extends AbstractPythonScriptingNodeModel {

    /**
     * Constructor for the node model.
     */
    public OpenInPythonNodeModel() {
        super(createPorts(1), new PortType[0]);
    }


    /**
     * Constructor for the node model.
     */
    protected OpenInPythonNodeModel(PortType[] inPorts, PortType[] outports) {
        super(inPorts, outports);
    }


    /**
     * {@inheritDoc}
     */
    @Override
    protected BufferedDataTable[] execute(final BufferedDataTable[] inData, final ExecutionContext exec) throws Exception {
        IPreferenceStore preferences = PythonScriptingBundleActivator.getDefault().getPreferenceStore();

//        boolean local = preferences.getBoolean(PythonPreferenceInitializer.PYTHON_LOCAL);
//        if (!local) throw new RuntimeException("This node can only be used with a local python executable");

        python = new LocalPythonClient();

        createTempFiles();
        pyOutFile = null;

        // Write data into csv
        logger.info("Writing table to CSV file");
        PythonTableConverter.convertTableToCSV(exec, inData[0], kInFile.getClientFile(), logger);

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
                super.prepareScript(writer);
            } finally {
                writer.close();
            }

            scriptFile.getClientFile().setExecutable(true);

            // Run the script
            if (Utils.isMacOSPlatform()) {
                Runtime.getRuntime().exec("open -a Terminal " + " " + scriptFile.getClientPath());
            } else if (Utils.isWindowsPlatform()) {
                Runtime.getRuntime().exec(pythonExecPath + " " + scriptFile.getClientPath());
            } else logger.error("Unsupported platform");
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        return new BufferedDataTable[0];
    }
}