package de.mpicbg.tds.knime.scripting.python;

import de.mpicbg.sweng.pythonserver.LocalPythonClient;
import de.mpicbg.tds.knime.knutils.Utils;
import de.mpicbg.tds.knime.scripting.python.prefs.PythonPreferenceInitializer;
import org.eclipse.jface.preference.IPreferenceStore;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.port.PortType;

import java.io.BufferedWriter;
import java.io.FileWriter;
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

        try {
            Writer writer = new BufferedWriter(new FileWriter(scriptFile.getClientFile()));
            try {
                // Write a shebang to invoke the python interpreter 
                writer.write("#! " + pythonExecPath + " -i\n");
                super.prepareScript(writer);
            } finally {
                writer.close();
            }

            scriptFile.getClientFile().setExecutable(true);

            // Run the script
            if (Utils.isMacOSPlatform()) {
                Runtime.getRuntime().exec("open -a Terminal " + scriptFile.getClientPath());
            } else if (Utils.isWindowsPlatform()) {
                Runtime.getRuntime().exec(pythonExecPath + " " + scriptFile.getClientPath());
            } else logger.error("Unsupported platform");
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        return new BufferedDataTable[0];
    }
}