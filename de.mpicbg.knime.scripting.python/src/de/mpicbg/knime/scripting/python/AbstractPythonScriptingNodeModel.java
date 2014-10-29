package de.mpicbg.knime.scripting.python;

import de.mpicbg.knime.scripting.core.AbstractScriptingNodeModel;
import de.mpicbg.knime.scripting.core.rgg.TemplateUtils;
import de.mpicbg.knime.scripting.python.scripts.PythonScripts;
import de.mpicbg.knime.scripting.python.srv.Python;
import de.mpicbg.knime.scripting.python.srv.PythonTempFile;
import org.eclipse.jface.preference.IPreferenceStore;
import org.knime.core.node.port.PortType;

import java.io.*;


public abstract class AbstractPythonScriptingNodeModel extends AbstractScriptingNodeModel {
    // Temp files for reading/writing the table and the script
    protected PythonTempFile kInFile;
    protected PythonTempFile pyOutFile;
    protected PythonTempFile scriptFile;

    protected Python python;

    protected IPreferenceStore preferences = PythonScriptingBundleActivator.getDefault().getPreferenceStore();

    protected AbstractPythonScriptingNodeModel(PortType[] inPorts, PortType[] outports) {
        super(inPorts, outports);
    }

    protected void prepareScript(Writer writer) throws IOException {
        // CSV read/write functions
        InputStream utilsStream = PythonScripts.class.getResourceAsStream("PythonCSVUtils.py");

        String readCSVCmd = kInFile != null ? "kIn, columnTypes = read_csv(r\"" + kInFile.getServerPath() + "\", True)" : "";
        String writeCSVCmd = pyOutFile != null ? "write_csv(r\"" + pyOutFile.getServerPath() + "\", pyOut, True)" : "";

        // Write the script file
        writer.write(TemplateUtils.convertStreamToString(utilsStream));
        writer.write("\n" + readCSVCmd + "\n");

        // Insert the user-defined script here
        writer.write("\n" + super.prepareScript() + "\n");

        writer.write("\n" + writeCSVCmd + "\n");

        return;
    }

    @Override
    public String prepareScript() throws RuntimeException {
        try {
            Writer writer = new BufferedWriter(new FileWriter(scriptFile.getClientFile()));
            try {
                prepareScript(writer);
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

}
