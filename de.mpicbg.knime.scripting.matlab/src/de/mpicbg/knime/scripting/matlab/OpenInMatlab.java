package de.mpicbg.knime.scripting.matlab;

import de.mpicbg.knime.knutils.AbstractNodeModel;
import de.mpicbg.knime.scripting.core.rgg.TemplateUtils;
import de.mpicbg.knime.scripting.matlab.prefs.MatlabPreferenceInitializer;

import matlabcontrol.MatlabProxy;
import matlabcontrol.MatlabProxyFactory;
import matlabcontrol.MatlabProxyFactoryOptions;

import org.eclipse.jface.preference.IPreferenceStore;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.defaultnodesettings.SettingsModelBoolean;
import org.knime.core.node.defaultnodesettings.SettingsModelString;

import java.io.*;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.concurrent.atomic.AtomicReference;


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

    /** Fields containing the MATLAB proxy object */
//    private MatlabProxy matlabProxy;
    private final AtomicReference<MatlabProxy> matlabProxyHolder = new AtomicReference<MatlabProxy>();
    
    /** Field holding the port number from the preference panel */
    protected int port = MatlabScriptingBundleActivator
    		.getDefault()
    		.getPreferenceStore()
    		.getInt(MatlabPreferenceInitializer.MATLAB_PORT);
    

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
        
        // Create a local MATLAB proxy.
        
        //Create proxy factory
        MatlabProxyFactoryOptions options = new MatlabProxyFactoryOptions.Builder()
                .setUsePreviouslyControlledSession(true)
                .setPort(port)
                .build();
        MatlabProxyFactory matlabProxyFactory = new MatlabProxyFactory(options);
        
        
      //Request a proxy
        matlabProxyFactory.requestProxy(new MatlabProxyFactory.RequestCallback()
        {
            @Override
            public void proxyCreated(final MatlabProxy proxy)
            {
                matlabProxyHolder.set(proxy);
            
                proxy.addDisconnectionListener(new MatlabProxy.DisconnectionListener()
                {
                    @Override
                    public void proxyDisconnected(MatlabProxy proxy)
                    {
                        matlabProxyHolder.set(null); 
                    }
                });
        
		        if (proxy.isExistingSession())
		        {
		            System.out.println("Connected to existing MATLAB session\n");
		        }
		        else
		        {
		            System.out.println("Launed new MATLAB session\n");
		        }
            }
        });
        
//        matlabProxy = matlabProxyFactory.getProxy();
        
        // Copy the MATLAB script to the temp folder
        File resourceFile = copyResourceToFolder(resourceFilePath, tmpPath);
        
        // Get the file name with the random string in it
        String functionName = TemplateUtils.fileNameTrunk(resourceFile.getName());
        
        // Generate the string that will be evaluated in MATLAB
        String cmd = "cd " + tmpPath + ";[kIn names]=" + functionName + "('" + binaryFileName + "','" + matlabType.getStringValue() + "','showMessage');";
        
        matlabProxyHolder.get().eval(cmd);
        matlabProxyHolder.get().disconnect();
//        matlabProxy.exit();
        
        

//        // Launch Matlab
//        if (openMatlab.getBooleanValue() || (checkMacOsxForRunningMatlabInstance() < 1)) {
//            // Copy the matlab file
//            File resourceFile = copyResourceToFolder(resourceFilePath, tmpPath);
//            String functionName = TemplateUtils.fileNameTrunk(resourceFile.getName());
//            // Launch matlab.
//            launchMacOsxMatlab(tmpPath, functionName, binaryFileName, matlabType.getStringValue());
//        }

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