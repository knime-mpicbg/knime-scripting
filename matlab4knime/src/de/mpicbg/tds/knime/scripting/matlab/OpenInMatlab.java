package de.mpicbg.tds.knime.scripting.matlab;

import de.mpicbg.tds.knime.knutils.AbstractNodeModel;
import de.mpicbg.tds.knime.knutils.scripting.rgg.TemplateUtils;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.defaultnodesettings.SettingsModelBoolean;
import org.knime.core.node.defaultnodesettings.SettingsModelString;

import java.io.*;
import java.util.ArrayList;
import java.util.LinkedHashMap;


/**
 * @author Felix Meyenhofer(MPI-CBG)
 *         <p/>
 *         TODO
 *         - Talking to matlab works only in Mac OSX so far. methods for windows have to be implementet
 */

public class OpenInMatlab extends AbstractNodeModel {


    // Hard coded parameter
    public final String resourceFilePath = "hashmaputils.m";
    public final String binaryFileName = "knime-table-dump.tmp";
    public final String tmpPath = System.getProperty("java.io.tmpdir") + "/";

    // Get the node settings
    public SettingsModelString matlabType = OpenInMatlabFactory.matlabTypeSetting();
    public SettingsModelBoolean openMatlab = OpenInMatlabFactory.executionModeSetting();


    /**
     * Constructor for the node model.
     */
    public OpenInMatlab() {
        super(1, 0);
        addSetting(matlabType);
        addSetting(openMatlab);
    }


    /**
     * {@inheritDoc}
     */
    @Override
    protected BufferedDataTable[] execute(final BufferedDataTable[] inData, final ExecutionContext exec) throws Exception {

        // Get the table
        BufferedDataTable data = inData[0];

        // Make it searializable
        LinkedHashMap serializableTable = TableConverter.convertKnimeTableToLinkedHashMap(data);

        // Save the object
        TableConverter.writeHashMapToTempFolder(tmpPath + binaryFileName, serializableTable);

        // Launch Matlab
        if (openMatlab.getBooleanValue() || (checkMacOsxForRunningMatlabInstance() < 1)) {
            // Copy the matlab file
            File resourceFile = copyResourceToFolder(resourceFilePath, tmpPath);
            String functionName = TemplateUtils.fileNameTrunk(resourceFile.getName());
            // Launch matlab.
            launchMacOsxMatlab(tmpPath, functionName, binaryFileName, matlabType.getStringValue());
        }

        return new BufferedDataTable[0];
    }


    public File copyResourceToFolder(String resourcePath, String outputPath) throws IOException {
        File ouputFile = new File(outputPath + resourcePath);
        ouputFile.deleteOnExit();
        InputStream resourceStream = getClass().getResourceAsStream(resourcePath);
        TemplateUtils.writeStreamToFile(resourceStream, new FileOutputStream(ouputFile));
        return ouputFile;
    }


    public static void launchMacOsxMatlab(String path, String scriptName, String fileName, String matlabType) throws IOException {
        Runtime cmd = Runtime.getRuntime();
        int ind = scriptName.lastIndexOf(".");
        String functionName;
        if (ind > 1) {
            functionName = scriptName.substring(0, ind);
        } else {
            functionName = scriptName;
        }
        String[] nargs = {"sh", "-c", getMacOsxMatlabExecutable() + " -desktop -r \"cd " + path + ";[kIn names]=" + functionName + "('" + fileName + "','" + matlabType + "','showMessage');\""};
        Process proc = cmd.exec(nargs);
    }


    public static String getMacOsxMatlabExecutable() throws IOException {
        Process proc = Runtime.getRuntime().exec("ls /Applications");
        BufferedReader stdout = new BufferedReader(new InputStreamReader(proc.getInputStream()));
        String line;
        ArrayList<String> matlabPaths = new ArrayList<String>();
        while ((line = stdout.readLine()) != null) {
            if (line.indexOf("MATLAB") > -1) {
                matlabPaths.add(line);
            }
        }
        String out = matlabPaths.get(matlabPaths.size() - 1);
        return "/Applications/" + out + "/bin/matlab";
    }


    public static int checkMacOsxForRunningMatlabInstance() throws IOException {
        Process proc = Runtime.getRuntime().exec("ps -efa");
        BufferedReader stdout = new BufferedReader(new InputStreamReader(proc.getInputStream()));

        String line;
        while ((line = stdout.readLine()) != null) {
            if (line.indexOf("Applications/MATLAB") > -1) {
                return 1;
            }
        }
        return 0;
    }


}