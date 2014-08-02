package de.mpicbg.knime.scripting.matlab.srv;

import java.io.File;

import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.ExecutionContext;


/**
 * This interface defines on one hand some conventions in the MATLAB scripting integration
 * for KNIME, such as input and output variable names, default ports etc.
 * On the other hand it defines the tasks that the nodes what to get the done by MATLAB.
 * This is important, because of the way the KNIME-MATLAB-interaction works: We want each
 * task to be an atomic operation, that can not be interrupted or interfered by 
 * another task from another node.
 * 
 * @author Felix Meyenhofer
 *
 */
public interface Matlab {
	
	/** Name of the variable in the MATLAB workspace where the KNIME input table will be stored in */
	public final static String INPUT_VARIABLE_NAME = "kIn";
	
	/** Name of the variable in the MATLAB workspace where the output data will be stored in */
	public final static String OUTPUT_VARIABLE_NAME = "mOut";
	
	/** Default MATLAB snippet script */
	public final static String DEFAULT_SNIPPET = OUTPUT_VARIABLE_NAME + " = " + INPUT_VARIABLE_NAME; 
	
	/** Default MATLAB plot script */
    public final static String DEFAULT_PLOTCMD = "% The command 'figureHandle = figure(...)' will be run prior to these commands.\nplot(kIn);";
	
	/** Default MATLAB type to hold the KNIME table data */
	public final static String DEFAULT_TYPE = "dataset";
	
	/** Default port the MATLAB server will listen to */
	public int DEFAULT_PORT = 1198;
	
	/** Registry name */
	public String REGISTRY_NAME = "MatlabServer";
	
	/** Temp-path of the JVM (used to store KNIME-MATLAB transfer data) */
	public final String TEMP_PATH = System.getProperty("java.io.tmpdir") + "/";

	/** Name of the m-file to load the hashmap object dump from KNIME */
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
	
	
	/**
	 * This methods includes all the necessary actions to make a KNIME
	 * table available in the MATLAB application.
	 * Note that this task on makes sense to be executed on the local machine.
	 * 
	 * @param inputTable KNIME input table
	 * @throws Exception 
	 */
	public void openTask(BufferedDataTable inputTable, String matlabType) 
			throws Exception;

	/**
	 * This method includes all the necessary actions to process a MATLAB
	 * snippet.
	 * 
	 * @param inputTable KNIME input table
	 * @param snippet MATLAB code snippet
	 * @return KNIME table produced by the MATLAB snippet
	 * @throws Exception 
	 */
	public BufferedDataTable snippetTask(BufferedDataTable inputTable, ExecutionContext exec, String snippet, String matlabType) 
			throws Exception;

	/**
	 * This method includes all the necessary actions to process a MATLAB 
	 * plot snippet.
	 * 
	 * @param inputTable KNIME input table
	 * @param snippet MATLAB code snippet
	 * @return File object of the temporary png-file of the plot 
	 * @throws Exception
	 */
	public File plotTask(BufferedDataTable inputTable, String snippet, Integer plotWidth, Integer plotHeight, String matlabType) 
			throws Exception;
	
	/**
	 * This method needs to be implemented by the clients to allow reacting
	 * on the cancellation of the KNIME workflow execution or other un-anticipated
	 * errors (that do not crash KNIME entirely). The method includes the necessary
	 * steps to restore the MATLAB server to a point that it can be executed again.
	 * 
	 * @throws InterruptedException
	 */
	public void rollback() throws InterruptedException;

	
	/**
	 * This method is to liberate disk and memory space after the completion
	 * of a task.
	 */
	public void cleanup();
	
}
