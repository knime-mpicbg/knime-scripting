package de.mpicbg.knime.scripting.matlab;

import de.mpicbg.knime.scripting.core.AbstractScriptingNodeModel;
import de.mpicbg.knime.scripting.core.utils.ScriptingUtils;
import de.mpicbg.knime.scripting.matlab.ctrl.MatlabConnector;
import de.mpicbg.knime.scripting.matlab.prefs.MatlabPreferenceInitializer;
import de.mpicbg.knime.scripting.matlab.ctrl.MatlabCode;
import de.mpicbg.knime.scripting.matlab.ctrl.MatlabFileTransfer;
import de.mpicbg.knime.scripting.matlab.ctrl.MatlabTable;
import matlabcontrol.MatlabConnectionException;
import matlabcontrol.MatlabInvocationException;
import matlabcontrol.MatlabProxy;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.core.node.port.PortType;
import org.osgi.framework.Bundle;
import org.osgi.framework.FrameworkUtil;

/**
 * This abstract class holds the static variables for conventions (default script, input
 * and output variable names etc.). Furthermore it handles the interactions with the preference store.
 * Finally there are some utility functions for house-keeping.
 * 
 * @author Holger Brandl, Felix Meyenhofer
 */
public abstract class AbstractMatlabScriptingNodeModel extends AbstractScriptingNodeModel {
	
	/** Name of the variable in the MATLAB workspace where the KNIME input table will be stored in */
	public final static String INPUT_VARIABLE_NAME = "kIn";
	
	/** Name of the variable in the MATLAB workspace where the output data will be stored in */
	public final static String OUTPUT_VARIABLE_NAME = "mOut";
	
	/** Name of the variable in the MATLAB workspace where the column mapping will be stored. 
	 * This is necessary for 'struct' MATLAB type that does not allow certain characters. Therefore
	 * we map unique MATLAB variable names to KNIME column names. */
	public final static String COLUMNS_VARIABLE_NAME = "columnMapping";
	
	/** Name of the variable to store the snippet error */
	public final static String ERROR_VARIABLE_NAME = "snippetError";
	
	/** Default MATLAB snippet script */
	public final static String DEFAULT_SNIPPET = "% " + INPUT_VARIABLE_NAME + 
			" contains the input data. After manipulations the output data has to be assigned " + 
			OUTPUT_VARIABLE_NAME + "\n" + OUTPUT_VARIABLE_NAME + " = " + INPUT_VARIABLE_NAME + ";"; 
	
	/** Default MATLAB plot script */
    public final static String DEFAULT_PLOTCMD = "% The command 'figureHandle = figure(...)'" +
    		" will be run prior to these commands to intercept the plot " +
    		"(i.e. do not put the figure command in your snippet).\n" + 
    		"plot(1:size(kIn,1));";
	
	/** Default MATLAB type to hold the KNIME table data */
	public final static String DEFAULT_TYPE = "table";
	
	/** Temp-path of the JVM (used to store KNIME-MATLAB transfer data) */
	public final static String TEMP_PATH = System.getProperty("java.io.tmpdir") + "/";

	/** Name of the m-file to load the hashmap object dump from KNIME (see resource folder) */
	public final static String MATLAB_HASHMAP_SCRIPT = "hashmaputils.m";
	
	/** Prefix for the KNIME table temp-file */
	public final static String TABLE_TEMP_FILE_PREFIX = "knime-table_";
	
	/** Suffix for the KNIME table temp-file */
	public final static String TABLE_TEMP_FILE_SUFFIX = ".tmp";
	
	/** Prefix for the MATLAB snippet temp-file */
	public final static String SNIPPET_TEMP_FILE_PREFIX = "snipped_";
	
	/** Suffix for the MATLAB snippet temp-file */
	public final static String SNIPPET_TEMP_FILE_SUFFIX = ".m";

	/** Prefix for the MATLAB plot temp-image */
	public final static String PLOT_TEMP_FILE_PREFIX = "plot_";
	
	/** Suffix for the MATLAB plot temp-image */
	public final static String PLOT_TEMP_FILE_SUFFIX = ".png";
	

    /** Settings from the KNIME preference dialog */
    protected IPreferenceStore preferences = MatlabScriptingBundleActivator.getDefault().getPreferenceStore();
    
    /** MALTLAB type */
    protected String matlabWorkspaceType = preferences.getString(MatlabPreferenceInitializer.MATLAB_TYPE);
    
    /** KNIME-to-MATLAB data transfer method */
    protected String tableTransferMethod = preferences.getString(MatlabPreferenceInitializer.MATLAB_TRANSFER_METHOD);
    
    
    /** Holds the MATLAB connector object */
    protected MatlabConnector matlabConnector; 
    
    /** Proxy holder, that allows to access the proxy for exception handling */
    protected MatlabProxy matlabProxy;
    
    
    /** Object to generate the MATLAB code needed for a particular task */ 
    protected MatlabCode code;
	
	/** Object to hold the KNIME table and allowing MATLAB compatible transformations */
    protected MatlabTable table;
	
	/** Temp-file holding the plot image */
    protected MatlabFileTransfer plotFile;
	
	/** Temp-file holding the hashmaputils.m for data parsing in MATLAB */
    protected MatlabFileTransfer parserFile;
	
	/** Temp-file holding the snippet code */
    protected MatlabFileTransfer codeFile;

    
    /**
     * Constructor
     * 
     * @param inPorts
     * @param outPorts
     * @throws MatlabConnectionException 
     */
    protected AbstractMatlabScriptingNodeModel(PortType[] inPorts, PortType[] outPorts) {
        super(inPorts, outPorts, new MatlabColumnSupport(),true, false, true);
        
    	matlabWorkspaceType = preferences.getString(MatlabPreferenceInitializer.MATLAB_TYPE);
    	tableTransferMethod = preferences.getString(MatlabPreferenceInitializer.MATLAB_TRANSFER_METHOD);
    	
    	try {
    		int matlabSessionCount = preferences.getInt(MatlabPreferenceInitializer.MATLAB_SESSIONS);
    		matlabConnector = MatlabConnector.getInstance(matlabSessionCount);//new MatlabClient(true, sessions);
    	} catch (MatlabConnectionException e) {
			logger.error("MATLAB could not be started. You have to install MATLAB on you computer" +
					" to use KNIME's MATLAB scripting integration.");
			e.printStackTrace();
		} catch (MatlabInvocationException e) {
			logger.error("MATLAB could not be started. You have to install MATLAB on you computer" +
					" to use KNIME's MATLAB scripting integration.");
			e.printStackTrace();
		} catch (InterruptedException e) {
			logger.error("MATLAB connection establishement was interrupted");
			e.printStackTrace();
		}
        
        // Add a property change listener that re-initializes the MATLAB client if the local flag changes.
        preferences.addPropertyChangeListener(new IPropertyChangeListener() {
			int newSessions = preferences.getInt(MatlabPreferenceInitializer.MATLAB_SESSIONS);
	        
			@Override
			public void propertyChange(PropertyChangeEvent event) {
				String newValue = event.getNewValue().toString();
			
				if (event.getProperty() == MatlabPreferenceInitializer.MATLAB_SESSIONS) {
					newSessions = Integer.parseInt(newValue);
					if (newSessions > 10) { 
						logger.warn("MATLAB: the number of application instances is limited to 10.");
						newSessions = 10;
					}
					MatlabConnector.setProxyQueueSize(newSessions);
				} else if (event.getProperty().equals(MatlabPreferenceInitializer.MATLAB_TRANSFER_METHOD)) {
					tableTransferMethod = event.getNewValue().toString();
				} else if (event.getProperty().equals(MatlabPreferenceInitializer.MATLAB_TYPE)) {
					matlabWorkspaceType = event.getNewValue().toString();
				}
				
//				initializeMatlabConnector(newSessions);
			}
		});
    }
    
	@Override
	protected PortObjectSpec[] configure(PortObjectSpec[] inSpecs)
			throws InvalidSettingsException {
		
		if(!MatlabScriptingBundleActivator.hasTemplateCacheLoaded) {
			try {
				Bundle bundle = FrameworkUtil.getBundle(getClass());
		        
		        List<String> preferenceStrings = new ArrayList<String>();
		        IPreferenceStore prefStore = MatlabScriptingBundleActivator.getDefault().getPreferenceStore();
		        preferenceStrings.add(prefStore.getString(MatlabPreferenceInitializer.MATLAB_TEMPLATE_RESOURCES));
		        preferenceStrings.add(prefStore.getString(MatlabPreferenceInitializer.MATLAB_PLOT_TEMPLATE_RESOURCES));
				
				ScriptingUtils.loadTemplateCache(preferenceStrings, bundle);
			
		    } catch(Exception e) {
		    	NodeLogger logger = NodeLogger.getLogger("scripting template init");
		    	logger.coding(e.getMessage());
		    }
			MatlabScriptingBundleActivator.hasTemplateCacheLoaded = true;
		}
		
		return super.configure(inSpecs);
	}


    
    /**
     * Cleanup temp data
     */
    public void cleanup() {
		if (this.table != null)
			this.table.cleanup();
		if (codeFile !=null)
			codeFile.delete();
//		if (parserFile != null)
//			parserFile.delete();
		if (plotFile != null)
			plotFile.delete();
	} 
}
