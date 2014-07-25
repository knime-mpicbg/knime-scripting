package de.mpicbg.knime.scripting.matlab.snippet;

import org.knime.core.data.DataTableSpec;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.InvalidSettingsException;

import de.mpicbg.knime.scripting.matlab.AbstractMatlabScriptingNodeModel;
import de.mpicbg.knime.scripting.matlab.TableConverter;


/**
 * This is the model implementation of MatlabSnippet. Matlab integration for Knime.
 *
 * @author Holger Brandl, Felix Meyenhofer (MPI-CBG)
 */

public class MatlabSnippetNodeModel extends AbstractMatlabScriptingNodeModel {

    public static final String DEFAULT_SCRIPT = "mOut = kIn;";


    /**
     * Constructor for the node model.
     */
    protected MatlabSnippetNodeModel() {
        super(createPorts(1), createPorts(1));
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

        establishConnection();

        // 1) Lock webserver and assess the initial workspace
        String[] initialWorkspace = matlab.assessWorkspace();
        matlab.lockServer();

        try {
            BufferedDataTable[] result = null;

            // convert exampleSet into matlab-structures and put into the matlab-workspace
            logger.info("Converting inputs into matlab-format");

            // Method 1
            TableConverter.pushData2Matlab(matlab, inData[0], "kIn");
            String script = prepareScript();

            // Method 2
//                    uploadData(inData[0]);
//                    transferHashMapUtils();
//                    // The load statement for matlab
//                    String script = "cd " + workingDir + ";[kIn names]=" +functionName + "('" + transferFile.getServerPath() + "','" + dataType + "');\n\n";
//                    // The acutal script
//                    script += prepareScript() + "\n\n";
//                    // The save statement for matlab
//                    script += "cd " + workingDir + ";" + functionName + "('" + transferFile.getServerPath() + "',mOut);\n";


            // run the script
            executeMatlabScript(script);

            // Get back the processed data.

            // Method 1
            BufferedDataTable outputs = TableConverter.pullDataFromMatlab(exec, matlab, "mOut");

//                    // Method 2
//                    BufferedDataTable outputs = downloadData(exec);

            result = new BufferedDataTable[]{outputs};
            return result;

        } catch (Throwable e) {
            throw new Exception(e);
        } finally {
            // Always leave the server in it's initial state
            matlab.unlockServer();
            matlab.recessWorkspace(initialWorkspace);
        }
    }


}
