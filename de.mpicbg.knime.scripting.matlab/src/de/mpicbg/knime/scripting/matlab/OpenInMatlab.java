package de.mpicbg.knime.scripting.matlab;

import de.mpicbg.knime.knutils.AbstractNodeModel;
import de.mpicbg.knime.scripting.core.rgg.TemplateUtils;
import de.mpicbg.knime.scripting.matlab.srv.MatlabLocal;
import de.mpicbg.knime.scripting.matlab.srv.MatlabUtilities;
import matlabcontrol.MatlabConnectionException;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.defaultnodesettings.SettingsModelString;

import java.io.*;
import java.util.LinkedHashMap;


/**
 * Node to open a KNIME table in MATLAB
 * 
 * @author Felix Meyenhofer
 */
public class OpenInMatlab extends AbstractNodeModel {

	/** KNIME setting name for the MATLAB variable type */
	static String MATLAB_TYPE_SETTING_NAME = "matlab.type";
	
    /** name of the MATLAB script that parses the hash map to MATLAB objects */
    public final String resourceFilePath = "hashmaputils.m";
    /** name of the temp-file that holds the table data */
    public final String binaryFileName = "knime-table-dump.tmp";
    /** get the JVM temp-directory */
    public final String tmpPath = System.getProperty("java.io.tmpdir") + "/";

    /** Object to access the MATLAB session */
    private MatlabLocal matlab;
    

    /**
     * Constructor for the node model.
     */
    public OpenInMatlab() {
    	// Define the ports and use a hashmap for setting models 
        super(1, 0, true);
        
        // Instantiate settings
        addModelSetting(MATLAB_TYPE_SETTING_NAME, createMatlabTypeSetting());
        
        // Instantiate the local MATLAB server
        try {
        	logger.info("Starting MATLAB...");
			matlab = new MatlabLocal();
		} catch (MatlabConnectionException e) {
			logger.error("MATLAB could not be started. You have to install MATLAB on you computer" +
					" to use KNIME's MATLAB scripting integration.");
			e.printStackTrace();
		}
    }
    
    
    /**
     * Create the SettingModel for the MATLAB type
     * 
     * @return MATLAB type Setting
     */
     static SettingsModelString createMatlabTypeSetting() {
        return new SettingsModelString(MATLAB_TYPE_SETTING_NAME, "dataset");
    }


    /**
     * {@inheritDoc}
     */
    @Override
    protected BufferedDataTable[] execute(final BufferedDataTable[] inData, final ExecutionContext exec) throws Exception {
        // Get the table
        BufferedDataTable data = inData[0];

        // Make it serialisable
        @SuppressWarnings("rawtypes")
		LinkedHashMap serializableTable = TableConverter.convertKnimeTableToLinkedHashMap(data);

        // Save the object
        TableConverter.writeHashMapToTempFolder(tmpPath + binaryFileName, serializableTable);       
        
        // Copy the MATLAB script to the temp-directory
        File resourceFile = copyResourceToFolder(resourceFilePath, tmpPath);
        
        // Get the file name with the random string in it
        String functionName = MatlabUtilities.getFileNameTrunk(resourceFile.getName());
        
        // Generate the string that will be evaluated in MATLAB
        String type = ((SettingsModelString)getModelSetting(MATLAB_TYPE_SETTING_NAME)).getStringValue();
        String cmd = "cd " + tmpPath + ";disp(' ');disp('Thread "+ this.matlab.getThreadNumber() +":');[kIn names]=" + functionName + "('" + binaryFileName + "','" + type + "','showMessage');";
        
        // Execute the command in MATLAB
        matlab.execute(cmd);
        logger.info("The data is now loaded in MATLAB. Switch to the MATLAB command window.");

        return new BufferedDataTable[0];
    }

    
    /**
     * Copy a package resource/script to a folder. 
     * 
     * @param resourcePath Path to a script or resource of this package
     * @param outputPath Absolute path in the local file-system
     * @return Output file name
     * @throws IOException
     */
    public File copyResourceToFolder(String resourcePath, String outputPath) throws IOException {
        File ouputFile = new File(outputPath + resourcePath);
        ouputFile.deleteOnExit();
        InputStream resourceStream = getClass().getResourceAsStream(resourcePath);
        TemplateUtils.writeStreamToFile(resourceStream, new FileOutputStream(ouputFile));
        return ouputFile;
    }

}