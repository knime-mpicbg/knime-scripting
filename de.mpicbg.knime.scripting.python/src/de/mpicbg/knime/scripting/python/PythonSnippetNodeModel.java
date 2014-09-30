package de.mpicbg.knime.scripting.python;

import de.mpicbg.knime.scripting.python.prefs.PythonPreferenceInitializer;
import de.mpicbg.knime.scripting.python.srv.CommandOutput;
import de.mpicbg.knime.scripting.python.srv.LocalPythonClient;
import de.mpicbg.knime.scripting.python.srv.PythonClient;
import org.eclipse.jface.preference.IPreferenceStore;
import org.knime.core.data.DataTableSpec;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.port.PortType;


/**
 * This is the model implementation of PythonSnippet. Python integration for Knime.
 *
 * @author Tom Haux (MPI-CBG)
 */
public class PythonSnippetNodeModel extends AbstractPythonScriptingNodeModel {
    public static final String DEFAULT_SCRIPT = "pyOut = kIn     # both are assumed to be dictionaries";


    public PythonSnippetNodeModel() {
        super(createPorts(1), createPorts(1));
    }


    /**
     * Constructor for the node model.
     */
    protected PythonSnippetNodeModel(PortType[] inPorts, PortType[] outports) {
        super(inPorts, outports);
    }


    @Override
    public String getDefaultScript() {
        return DEFAULT_SCRIPT;
    }


    @Override
    protected DataTableSpec[] configure(DataTableSpec[] inSpecs) throws InvalidSettingsException {
        return new DataTableSpec[]{null};
    }


    /**
     * {@inheritDoc}
     */
    @Override
    protected BufferedDataTable[] execute(final BufferedDataTable[] inData,
                                          final ExecutionContext exec) throws Exception {
        IPreferenceStore preferences = PythonScriptingBundleActivator.getDefault().getPreferenceStore();

        boolean local = preferences.getBoolean(PythonPreferenceInitializer.PYTHON_LOCAL);

        String host = preferences.getString(PythonPreferenceInitializer.PYTHON_HOST);
        int port = preferences.getInt(PythonPreferenceInitializer.PYTHON_PORT);

        // If the host is empty use a local client, otherwise use the server values
        python = local ? new LocalPythonClient() : new PythonClient(host, port);

        createTempFiles();

        // Write data into csv
        logger.info("Writing table to CSV file");
        PythonTableConverter.convertTableToCSV(exec, inData[0], kInFile.getClientFile(), logger);
        kInFile.upload();

        // Execute script
        logger.info("Creating and executing python script: " + scriptFile.getClientPath());

        String script = prepareScript();

        try {
            // Get the path to the python executable
            String pythonExecPath = local ? preferences.getString(PythonPreferenceInitializer.PYTHON_EXECUTABLE) : "python";

            CommandOutput output = python.executeCommand(new String[]{pythonExecPath, script});
            for (String o : output.getStandardOutput()) {
                logger.info(o);
            }

            for (String o : output.getErrorOutput()) {
                logger.error(o);
            }
        } catch (Throwable t) {
            throw new RuntimeException(t);
        }

        // If an output file wasn't created, error out
        pyOutFile.fetch();
        if (!pyOutFile.getClientFile().exists()) {
            throw new RuntimeException(pyOutFile.getClientPath() + ": file not found");
        }

        // Parse result, if any, back into table
        boolean hasOutput = pyOutFile.getClientFile().exists() && pyOutFile.getClientFile().length() != 0;

        if (!hasOutput) throw new RuntimeException("No python output table found, check script output");

        logger.info("Reading python output into Knime table");
        BufferedDataTable[] resultTable = new BufferedDataTable[]{PythonTableConverter.convertCSVToTable(exec, pyOutFile.getClientFile(), logger)};

        deleteTempFiles();

        return resultTable;
    }

}