/*
 * Copyright (c) 2011. 
 * Max Planck Institute of Molecular Cell Biology and Genetics, Dresden
 *
 * This module is distributed under the BSD-License. For details see the license.txt.
 *
 * It is the obligation of every user to abide terms and conditions of The MathWorks, Inc. Software License Agreement. In particular Article 8 “Web Applications”: it is permissible for an Application created by a Licensee of the NETWORK CONCURRENT USER ACTIVATION type to use MATLAB as a remote engine with static scripts.
 */

package de.mpicbg.knime.scripting.matlab;

import de.mpicbg.knime.scripting.core.AbstractScriptingNodeModel;
import de.mpicbg.knime.scripting.core.rgg.TemplateUtils;
import de.mpicbg.knime.scripting.matlab.prefs.MatlabPreferenceInitializer;
import de.mpicbg.knime.scripting.matlab.srv.MatlabTempFile;
import de.mpicbg.knime.scripting.matlab.srv.MatlabUtilities;
import de.mpicbg.knime.scripting.matlab.srv.MatlabWebClient;
import org.eclipse.jface.preference.IPreferenceStore;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.port.PortType;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.LinkedHashMap;


/**
 * 
 * @author Holger Brandl, Felix Meyenhofer
 *
 */
public abstract class AbstractMatlabScriptingNodeModel extends AbstractScriptingNodeModel {


    // Temp files for reading/writing the table and the script
    protected MatlabWebClient matlab;
    protected IPreferenceStore preferences = MatlabScriptingBundleActivator.getDefault().getPreferenceStore();

    protected MatlabTempFile transferFile;
    protected MatlabTempFile hashMapScript;
    protected String workingDir;
    protected String functionName;

    protected String dataType = "dataset";    // TODO make a tab in the configuration dialog to expose choices for different matlab types.


    //
    // Constructor
    //
    protected AbstractMatlabScriptingNodeModel(PortType[] inPorts, PortType[] outports) {
        super(inPorts, outports);
    }


    protected void establishConnection() {
        if (matlab == null || !matlab.isConnected()) {
            String host = preferences.getString(MatlabPreferenceInitializer.MATLAB_HOST);
            int port = preferences.getInt(MatlabPreferenceInitializer.MATLAB_PORT);
            matlab = new MatlabWebClient(host, port);
        }
        if (matlab.isLocked()) {
            throw new RuntimeException("MATLAB server is busy. Try again later.");
        }
    }


    protected void executeMatlabScript(String script) throws Exception {
        String errorMessage = matlab.executeScript(script);
        if (!errorMessage.isEmpty()) {
            logger.error(errorMessage);
            throw new Exception(new RuntimeException("Matlab failed to create plot (see error message above)."));
        }
    }


    protected void transferHashMapUtils() throws IOException {
        hashMapScript = new MatlabTempFile(matlab, "hashmaputils", ".m");
        copyResourceToFolder("hashmaputils.m", hashMapScript.getClientPath());
        hashMapScript.upload();
        workingDir = hashMapScript.getServerFile().getParent();
        functionName = MatlabUtilities.fileName2functionName(hashMapScript.getServerFile().getName());
    }


    protected void uploadData(BufferedDataTable inputTable) throws Exception {
        LinkedHashMap table = TableConverter.convertKnimeTableToLinkedHashMap(inputTable);
        transferFile = new MatlabTempFile(matlab, "knime-matlab", ".tmp");
        TableConverter.writeHashMapToTempFolder(transferFile.getClientPath(), table);
        transferFile.upload();
    }


    protected BufferedDataTable downloadData(ExecutionContext exec) throws Exception {
        transferFile.fetch();
        LinkedHashMap scriptOutput = TableConverter.readSerializedHashMap(transferFile.getClientFile());
        BufferedDataTable outputs = TableConverter.convertLinkedHashMapToKnimeTable(exec, scriptOutput);
        return outputs;
    }


    public File copyResourceToFolder(String resourcePath, String outputPath) throws IOException {
        File ouputFile = new File(outputPath);
        ouputFile.deleteOnExit();
        InputStream resourceStream = getClass().getResourceAsStream(resourcePath);
        TemplateUtils.writeStreamToFile(resourceStream, new FileOutputStream(ouputFile));
        return ouputFile;
    }


}
