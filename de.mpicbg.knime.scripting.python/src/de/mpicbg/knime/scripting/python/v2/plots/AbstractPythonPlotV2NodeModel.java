package de.mpicbg.knime.scripting.python.v2.plots;

import java.awt.image.BufferedImage;
import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.nio.file.Files;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;

import javax.imageio.ImageIO;
import javax.swing.ImageIcon;

import org.knime.core.data.image.png.PNGImageContent;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.defaultnodesettings.SettingsModelBoolean;
import org.knime.core.node.defaultnodesettings.SettingsModelIntegerBounded;
import org.knime.core.node.defaultnodesettings.SettingsModelString;
import org.knime.core.node.port.PortObject;
import org.knime.core.node.port.PortType;
import org.knime.core.node.port.image.ImagePortObject;
import org.knime.core.node.port.image.ImagePortObjectSpec;
import org.rosuda.REngine.Rserve.RConnection;

import de.mpicbg.knime.scripting.core.FlowVarUtils;
import de.mpicbg.knime.scripting.core.ScriptingModelConfig;
import de.mpicbg.knime.scripting.core.exceptions.KnimeScriptingException;
import de.mpicbg.knime.scripting.python.v2.AbstractPythonScriptingV2NodeModel;
import de.mpicbg.knime.scripting.python.v2.AbstractPythonScriptingV2NodeModel.PythonInpuMode.Flag;

public class AbstractPythonPlotV2NodeModel extends AbstractPythonScriptingV2NodeModel {
	
	protected static final ImagePortObjectSpec IM_PORT_SPEC = new ImagePortObjectSpec(PNGImageContent.TYPE);

    public BufferedImage m_image;						// image created by R
    public File m_nodeImageFile;				// image file (internals)
    protected File m_workspaceFile;			// workspace file (internals)
	
    /**
     * MODEL - SETTINGS
     */
    public static final String CFG_WIDTH = "figure.width";
    public static final int CFG_WIDTH_DFT = 1000;
    
    public static final String CFG_HEIGHT = "figure.height";
    public static final int CFG_HEIGHT_DFT = 700;
    
    public static final String CFG_DPI = "figure.dpi";
    public static final int CFG_DPI_DFT = 75;
    
    public static final String CFG_OUTFILE = "figure.output.file";
    
    public static final String CFG_OVERWRITE = "overwrite.ok";
    public static final boolean CFG_OVERWRITE_DFT = false;
    
    public static final String CFG_WRITE = "write.output.file";
    public static final boolean CFG_WRITE_DFT = true;
    
    public static final String CFG_IMGTYPE = "figure.ouput.type";
    public static final String CFG_IMGTYPE_DFT = "png";
    
    public static final String DEFAULT_PYTHON_PLOTCMD = "" +     
		"# the following import is not required, as the node take care of it \n" + 
		"#import matplotlib.pyplot as plt\n" + 
		"\n" + 
		"X = range(10)\n" + 
		"plt.plot(X, [x*x for x in X])\n" + 
		"plt.show()";
    
    public static final List<String> SUPPORTED_FORMATS = new LinkedList<String>(
    		Arrays.asList("png", "jpeg", "svg", "pdf"));

	public AbstractPythonPlotV2NodeModel(ScriptingModelConfig nodeModelConfig) {
		super(nodeModelConfig);
		
		this.addModelSetting(CFG_HEIGHT, createHeightSM());
        this.addModelSetting(CFG_WIDTH, createWidthSM());
        this.addModelSetting(CFG_DPI, createDpiSM());
        this.addModelSetting(CFG_IMGTYPE, createImgTypeSM());
        this.addModelSetting(CFG_OUTFILE, createOutputFileSM());
        this.addModelSetting(CFG_OVERWRITE, createOverwriteSM());
        this.addModelSetting(CFG_WRITE, createWriteFileSM());
	}
	
	/**
     * create settings model: enable output to file yes/no
     * @return
     */
    public static SettingsModelBoolean createWriteFileSM() {
		return new SettingsModelBoolean(CFG_WRITE, CFG_WRITE_DFT);
	}

    /**
     * create settings model: overwrite output to file yes/no
     * @return
     */
	public static SettingsModelBoolean createOverwriteSM() {
		return new SettingsModelBoolean(CFG_OVERWRITE, CFG_OVERWRITE_DFT);
	}

    /**
     * create settings model: output file
     * @return
     */
	public static SettingsModelString createOutputFileSM() {
		return new SettingsModelString(CFG_OUTFILE, null);
	}

    /**
     * create settings model: image type
     * @return
     */
	public static SettingsModelString createImgTypeSM() {
		return new SettingsModelString(CFG_IMGTYPE, CFG_IMGTYPE_DFT);
	}

    /**
     * create settings model: image width in pixels
     * @return
     */
	public static SettingsModelIntegerBounded createWidthSM() {
    	return new SettingsModelIntegerBounded(CFG_WIDTH, CFG_WIDTH_DFT, 0, Integer.MAX_VALUE);
	}

    /**
     * create settings model: image height in pixels
     * @return
     */
	public static SettingsModelIntegerBounded createHeightSM() {
		return new SettingsModelIntegerBounded(CFG_HEIGHT, CFG_HEIGHT_DFT, 0, Integer.MAX_VALUE);
	}
	
    /**
     * create settings model: image height in pixels
     * @return
     */
	public static SettingsModelIntegerBounded createDpiSM() {
		return new SettingsModelIntegerBounded(CFG_DPI, CFG_DPI_DFT, 0, Integer.MAX_VALUE);
	}
	
	

	/**
     * {@inheritDoc}
     * <br>
     * adds functionality to create + save image and to provide image output
	 * @throws CanceledExecutionException 
     */
	@Override
	protected PortObject[] pullOutputFromPython(ExecutionContext exec) throws KnimeScriptingException, CanceledExecutionException {
		
		try {
			createInternals();
			saveFigureAsFile();
		} catch (KnimeScriptingException e) {
			throw e;
		}
		
		// retrieve table + generic outputs if present
		PortObject[] outPorts = super.pullOutputFromPython(exec);
		
		for(int i = 0; i < getNrOutPorts(); i++) {
			PortType pType = this.getOutPortType(i);
			
			// create image for image port
			if(pType.equals(ImagePortObject.TYPE)) {
				ByteArrayOutputStream baos = new ByteArrayOutputStream();
				try {
					ImageIO.write(m_image, "png", baos);
				} catch (IOException e) {
					throw new KnimeScriptingException(e.getMessage());
				}
				PNGImageContent content = new PNGImageContent(baos.toByteArray());
		        
		        outPorts[i] = new ImagePortObject(content, IM_PORT_SPEC);
			}
		}
		
		return outPorts;
	}
	
	/**
	 * method writes image to file
	 * @throws KnimeScriptingException
	 */
	protected void saveFigureAsFile() throws KnimeScriptingException {
		boolean enableFileOutput = ((SettingsModelBoolean) getModelSetting(CFG_WRITE)).getBooleanValue();
        // no need to save image to file ?
        if(!enableFileOutput) return;
        
        assert m_image != null;
        
        String fileName = ((SettingsModelString) getModelSetting(CFG_OUTFILE)).getStringValue();
        boolean overwriteFileOutput = ((SettingsModelBoolean) getModelSetting(CFG_OVERWRITE)).getBooleanValue();
        
        if(fileName == null) return;
        if(fileName.length() == 0) return;

        fileName = prepareOutputFileName(fileName);
        
        // the plot should be written to file
        if (!fileName.isEmpty()) {
        	File imageFile = new File(fileName);
        	// check if the file already exists but should not be overwritten
        	if(imageFile.exists()) {
        		if(!overwriteFileOutput)
        			throw new KnimeScriptingException("Overwrite file is disabled but image file '" + fileName + "' already exsists.");
        	} else {
        		try {
        			imageFile.createNewFile();
        		} catch(IOException e) {
        			throw new KnimeScriptingException("Output file '" + fileName + "' cannot be created. Please check the output path! (" + e.getMessage() + ")");
        		}
        	}

        	FileOutputStream fsOut = null;
        	try {
	            fsOut = new FileOutputStream(new File(fileName));
	            ImageIO.write(m_image, "png", fsOut);
	            fsOut.close();
        	} catch (IOException e) {
        		throw new KnimeScriptingException("Failed to sava image to file:\n" + e.getMessage());
        	}

        }
	}
	
    /**
     * replace placeholders in filename with appropriate values
     * @param fileName
     * @return final filename
     */
    private String prepareOutputFileName(String fileName) {
    	
    	final String TODAY = new SimpleDateFormat("yyMMdd").format(new Date(System.currentTimeMillis()));
        // process flow-variables
        fileName = FlowVarUtils.replaceFlowVars(fileName, this);

        // replace wildcards

        // 1) date
        fileName = fileName.replace("$$DATE$$", TODAY);

        // 2) user
        fileName = fileName.replace("$$USER$$", System.getProperty("user.name"));

        // 3) workspace dir
        if (fileName.contains("$$WS$$")) {
            String wsLocation = getFlowVariable("knime.workspace");
            fileName = fileName.replace("$$WS$$", wsLocation);
        }

        return fileName;
    }
	
	/**
     * Creates a figure using the R-variable in the current connection's workspace as input.
	 * @throws IOException 
     * @throws KnimeScriptingException 
     */
    protected void createInternals() throws KnimeScriptingException {

        // workspace file needs to be saved as internal and will be used to recreate image 
    	// => generated from the script like the image

        // create the image
        String script = prepareScript();
        m_image = createImage(script, getDefWidth(), getDefHeight(), getImageType());
    }
    
    
	protected BufferedImage createImage(String script, int defWidth, int defHeight, String imageType) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	protected void prepareScript(File scriptFile, boolean useScript, PythonInpuMode flag) throws KnimeScriptingException {
		File writeToFile = new File("/somepath");
		super.prepareScript(scriptFile, useScript, new PythonInpuMode(writeToFile, Flag.WRITE));
	}

	/**
	 * @return image height (pixels)
	 */
    public int getDefHeight() {
        return ((SettingsModelIntegerBounded) getModelSetting(CFG_HEIGHT)).getIntValue();
    }

    /**
     * @return image width (pixels)
     */
    public int getDefWidth() {
    	return ((SettingsModelIntegerBounded) getModelSetting(CFG_WIDTH)).getIntValue();
    }
    
    public String getImageType() {
    	return ((SettingsModelString) getModelSetting(CFG_IMGTYPE)).getStringValue();
    }
    
    public BufferedImage getImage() {
        try {
            if (m_image == null && m_nodeImageFile != null && m_nodeImageFile.isFile()) {
                logger.warn("Restoring image from disk. This might take a few seconds...");
                deserializeImage();
            }
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }

        return m_image;
    }
    
	/**
	 * read image from file
	 * @throws IOException
	 */
	protected void deserializeImage() throws IOException {
        if (m_nodeImageFile.isFile()) {
        		m_image = ImageIO.read(m_nodeImageFile);
        }
    }
    
    public File getWSFile() {
        return m_workspaceFile;
    }
    

	
    @Override
    protected void saveInternals(File nodeDir, ExecutionMonitor executionMonitor) throws IOException, CanceledExecutionException {
        if (m_workspaceFile != null) {
            File f = new File(nodeDir, "pushtable.R");

            Files.copy(m_workspaceFile.toPath(), f.toPath());
        }

        if (m_image != null) {
            File imageFile = new File(nodeDir, "image.png");
            
            ImageIO.write(m_image, "png", imageFile);
        }
    }
    
    @Override
	protected void reset() {
		super.reset();
		m_image = null;
	}
    
	@Override
	protected void onDispose() {
		super.onDispose();
		try {
			removeTempWorkspace();
		} catch (IOException e) {
			logger.debug("temporary R workspace file could not be deleted:\t" + e.getMessage());
			e.printStackTrace();
		}
	}
	
    /**
     * remove temporary workspace file if it exists
     * @throws IOException if it fails to delete that file
     */
    private void removeTempWorkspace() throws IOException {
    	// delete temporary R workspace when node is configured
    	if(m_workspaceFile != null) {
    		if(m_workspaceFile.exists())
    			Files.delete(m_workspaceFile.toPath());
    		m_workspaceFile = null;
    	}
    }
}
