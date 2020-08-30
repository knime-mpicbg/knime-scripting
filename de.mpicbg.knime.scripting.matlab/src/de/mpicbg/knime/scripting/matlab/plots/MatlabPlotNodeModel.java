package de.mpicbg.knime.scripting.matlab.plots;

import de.mpicbg.knime.scripting.core.FlowVarUtils;
import de.mpicbg.knime.scripting.core.exceptions.KnimeScriptingException;
import de.mpicbg.knime.scripting.matlab.AbstractMatlabScriptingNodeModel;
import de.mpicbg.knime.scripting.matlab.ctrl.MatlabCode;
import de.mpicbg.knime.scripting.matlab.ctrl.MatlabFileTransfer;
import de.mpicbg.knime.scripting.matlab.ctrl.MatlabTable;

import org.knime.core.data.image.png.PNGImageContent;
import org.knime.core.node.*;
import org.knime.core.node.defaultnodesettings.SettingsModelBoolean;
import org.knime.core.node.defaultnodesettings.SettingsModelInteger;
import org.knime.core.node.defaultnodesettings.SettingsModelIntegerBounded;
import org.knime.core.node.defaultnodesettings.SettingsModelString;
import org.knime.core.node.port.PortObject;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.core.node.port.PortType;
import org.knime.core.node.port.image.ImagePortObject;
import org.knime.core.node.port.image.ImagePortObjectSpec;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.text.SimpleDateFormat;
import java.util.Date;


/**
 * Node model for the MATLAB plot.
 *
 * @author Tom Haux, Felix Meyenhofer
 */
public class MatlabPlotNodeModel extends AbstractMatlabScriptingNodeModel {

	/** Setting name for the plot width */
	private static final String FIGURE_WIDTH_SETTING_NAME = "figure.width";
	
	/** Setting name for the plot height */
	private static final String FIGURE_HEIGHT_SETTING_NAME = "figure.height";

	/** Setting name for the plot output file path */
	private static final String OUTPUT_FILE_SETTING_NAME = "figure.output.file";

	/** Setting name for the plot file overwrite option */
	private static final String OVERWRITE_SETTING_NAME = "overwrite.ok";
    
    /** Date string for the output file name generation */
    private static String TODAY = new SimpleDateFormat("yyMMdd").format(new Date(System.currentTimeMillis()));

	/** Default image port type */
    protected static final ImagePortObjectSpec IM_PORT_SPEC = new ImagePortObjectSpec(PNGImageContent.TYPE);

    /** Default plot width */
	private static final int PLOT_DEFAULT_WIDTH = 800;
	
	/** Default plot height */
	private static final int PLOT_DEFAULT_HEIGHT = 600;


    /** MATLAB plot image holder */
    private Image image;


    /**
     * Default Constructor
     */
    public MatlabPlotNodeModel() {
        this(createPorts(1), new PortType[]{ImagePortObject.TYPE});
    }

    /**
     * Constructor imposing the input port number
     * 
     * @param inPorts
     */
    public MatlabPlotNodeModel(PortType[] inPorts) {
        this(inPorts, new PortType[]{ImagePortObject.TYPE});
    }

    /**
     * Constructor defining input and output ports
     * 
     * @param inPorts
     * @param outports
     */
    public MatlabPlotNodeModel(PortType[] inPorts, PortType[] outports) {
        super(inPorts, outports);

        addModelSetting(FIGURE_HEIGHT_SETTING_NAME, createPropFigureHeightSetting());
        addModelSetting(FIGURE_WIDTH_SETTING_NAME, createPropFigureWidthSetting());
        addModelSetting(OUTPUT_FILE_SETTING_NAME, createPropOutputFileSetting());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected PortObjectSpec[] configure(final PortObjectSpec[] inSpecs) throws InvalidSettingsException {
    	super.configure(inSpecs);
        return new PortObjectSpec[]{IM_PORT_SPEC};
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public String getDefaultScript(String defaultScript) {
    	return super.getDefaultScript(AbstractMatlabScriptingNodeModel.DEFAULT_PLOTCMD);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveInternals(File nodeDir, ExecutionMonitor executionMonitor) throws IOException, CanceledExecutionException {
        if (image != null) {
            File imageFile = new File(nodeDir, "image.bin");
            FileOutputStream f_out = new FileOutputStream(imageFile);
            // Write object with ObjectOutputStream
            ObjectOutputStream obj_out = new ObjectOutputStream(new BufferedOutputStream(f_out));
            // Write object out to disk
            obj_out.writeObject(new ImageIcon(image));
            obj_out.close();
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadInternals(File nodeDir, ExecutionMonitor executionMonitor) throws IOException, CanceledExecutionException {
        super.loadInternals(nodeDir, executionMonitor);

        try {
            File nodeImageFile = new File(nodeDir, "image.bin");
            FileInputStream f_in = new FileInputStream(nodeImageFile);
            // Read object using ObjectInputStream
            ObjectInputStream obj_in = new ObjectInputStream(new BufferedInputStream(f_in));
            // Read an object
            image = ((ImageIcon) obj_in.readObject()).getImage();
            obj_in.close();
        } catch (Throwable ignored) {
        }
    }

    /**
     * Getter for the plot height (from node dialog settings)
     * 
     * @return Plot height
     */
    protected int getDefHeight() {
        return ((SettingsModelInteger)getModelSetting(FIGURE_HEIGHT_SETTING_NAME)).getIntValue();//propHeight.getIntValue();
    }

    /**
     * Getter for the plot width (from the node settings)
     * @return Plot width
     */
    protected int getDefWidth() {
        return ((SettingsModelInteger)getModelSetting(FIGURE_WIDTH_SETTING_NAME)).getIntValue();//propWidth.getIntValue();
    }

    /**
     * Getter for the plot image output file path (from node dialog settings)
     * 
     * @return Plot image file path
     */
    private String getOutputFilePath() {
    	return ((SettingsModelString)getModelSetting(OUTPUT_FILE_SETTING_NAME)).getStringValue();
    }
    
    /**
     * Getter for the output image overwrite flag (from the node dialog settings
     * 
     * @return Overwrite flag
     */
    private boolean getOverwriteFlag() {
    	return ((SettingsModelBoolean)getModelSetting(OVERWRITE_SETTING_NAME)).getBooleanValue();
    }
    
    /**
     * Getter for the plot image
     * 
     * @return Plot image object
     */
    public Image getImage() {
        return image;
    }
    
    /**
     * Create the plot width setting
     * 
     * @return Plot width setting
     */
    public static SettingsModelInteger createPropFigureWidthSetting() {
		return new SettingsModelIntegerBounded(FIGURE_WIDTH_SETTING_NAME, PLOT_DEFAULT_WIDTH, 100, 5000);
    }

    /**
     * Create the plot height setting
     * 
     * @return Plot height setting
     */
    public static SettingsModelInteger createPropFigureHeightSetting() {
        return new SettingsModelIntegerBounded(FIGURE_HEIGHT_SETTING_NAME , PLOT_DEFAULT_HEIGHT, 100, 5000);
    }

    /**
     * Create the Overwrite option setting
     * 
     * @return Overwrite setting
     */
    public static SettingsModelBoolean createOverwriteFileSetting() {
        return new SettingsModelBoolean(OVERWRITE_SETTING_NAME , false);
    }

    /**
     * Create the output file path setting
     * 
     * @return Output file path
     */
    public static SettingsModelString createPropOutputFileSetting() {
        return new SettingsModelString(OUTPUT_FILE_SETTING_NAME , "") {
            @Override
            protected void validateSettingsForModel(NodeSettingsRO settings) throws InvalidSettingsException {
            }
        };
    }
    
	/**
	 * {@inheritDoc}
	 */
	@Override
	protected PortObject[] executeImpl(PortObject[] inData,
			ExecutionContext exec) throws Exception {
    	PortObject[] outPorts = new PortObject[1];
    	
    	try {
	    	// Get the input table
	    	BufferedDataTable inputTable = (BufferedDataTable)inData[0];
	    	
	    	// Create the plot script
	        String snippet = prepareScript();
	        exec.checkCanceled();
	
	        // Prepare snippet temp-file
 			codeFile = new MatlabFileTransfer(AbstractMatlabScriptingNodeModel.SNIPPET_TEMP_FILE_PREFIX, 
 					AbstractMatlabScriptingNodeModel.SNIPPET_TEMP_FILE_SUFFIX);
 			// Prepare the plot temp file
 			plotFile = new MatlabFileTransfer(AbstractMatlabScriptingNodeModel.PLOT_TEMP_FILE_PREFIX, 
 					AbstractMatlabScriptingNodeModel.PLOT_TEMP_FILE_SUFFIX);
 			// Get the  input table data ready for processing
 			table = new MatlabTable(inputTable);
 			
 			if (tableTransferMethod.equals("file")) {
 				// Transfer the KNIME table as hash map object dump to the JVM temp-folder
 				table.writeHashMapToTempFolder();
 				
 				// Prepare the parser temp-file
 				parserFile = new MatlabFileTransfer(AbstractMatlabScriptingNodeModel.MATLAB_HASHMAP_SCRIPT);
 				
 				// Prepare the code snippet
 				code = new MatlabCode(snippet, matlabWorkspaceType, 
 						parserFile.getPath(), 
 						codeFile.getPath(), 
 						table.getHashMapTempPath(), 
 						plotFile.getPath(),
 						getDefWidth(), getDefHeight());
 				codeFile.save(new ByteArrayInputStream(code.getScript().getBytes()));
 				String cmd = code.getScriptExecutionCommand(codeFile.getPath(), false, false);
 				
 				// Execute
 				matlabProxy = matlabConnector.acquireProxyFromQueue();
 				MatlabCode.safeEvaluation(matlabProxy, cmd);
 				MatlabCode.checkForScriptErrors(matlabProxy);
 				matlabProxy.eval(MatlabCode.getPlotNodeMessage(false));
// 			    releaseMatlabProxy(proxy);
 				
 			} else if (tableTransferMethod.equals("workspace")) {
 				// Get a proxy (block it)
 				matlabProxy = matlabConnector.acquireProxyFromQueue();

 				// Push the table to the input variable in the MATLAB workspace.
 				table.pushTable2MatlabWorkspace(matlabProxy, matlabWorkspaceType);
 				
 				// Prepare the code snippet.
 				code = new MatlabCode(snippet, matlabWorkspaceType,
 						codeFile.getPath(), 
 						plotFile.getPath(), 
 						getDefWidth(), getDefHeight());
 				codeFile.save(new ByteArrayInputStream(code.getScript().getBytes()));
 				String cmd = code.getScriptExecutionCommand(codeFile.getPath(), true, false);
 				
 				// Execute 
 				MatlabCode.safeEvaluation(matlabProxy, cmd);
 				MatlabCode.checkForScriptErrors(matlabProxy);
 				matlabProxy.eval(MatlabCode.getPlotNodeMessage(false));
 			}
 			
	        exec.checkCanceled();
	        
	        // Fetch the image file form the server and load it
	        image = MatlabPlotCanvas.toBufferedImage(new ImageIcon(plotFile.getPath()).getImage());
	
	        // Prepare the image file for KNIME to display
	        String fileName = prepareOutputFileName(getOutputFilePath());
	        if (!fileName.isEmpty()) {
	            if (!getOverwriteFlag() && new File(fileName).exists()) {
	                throw new RuntimeException("Overwrite file is disabled but image file '" + fileName + "' already exists");
	            }
	            ImageIO.write((BufferedImage) image, "png", new File(fileName));
	        }
	
	        // Create the image port object
	        PNGImageContent content;
	        FileInputStream in = new FileInputStream(plotFile.getPath());
	        content = new PNGImageContent(in);
	        in.close();
	        	
	        outPorts[0] = new ImagePortObject(content, IM_PORT_SPEC);
	        
	        // Housekeeping
	        cleanup();
        
    	} catch (Exception e) {
    		throw e;
    	} finally {
    		if ((matlabConnector != null) && (matlabProxy != null))
    			matlabConnector.returnProxyToQueue(matlabProxy);
    	}
    	
    	return outPorts;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void openIn(PortObject[] inData, ExecutionContext exec)
			throws KnimeScriptingException {
		throw new KnimeScriptingException("The functionality to open data external is not yet implemented");
	}
}
