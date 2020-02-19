package de.mpicbg.knime.scripting.python.v2.plots;

import java.awt.image.BufferedImage;
import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;

import javax.imageio.ImageIO;

import org.apache.commons.io.FilenameUtils;
import org.knime.core.data.image.png.PNGImageContent;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.defaultnodesettings.SettingsModelBoolean;
import org.knime.core.node.defaultnodesettings.SettingsModelIntegerBounded;
import org.knime.core.node.defaultnodesettings.SettingsModelString;
import org.knime.core.node.port.PortObject;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.core.node.port.PortType;
import org.knime.core.node.port.image.ImagePortObject;
import org.knime.core.node.port.image.ImagePortObjectSpec;

import de.mpicbg.knime.scripting.core.FlowVarUtils;
import de.mpicbg.knime.scripting.core.ScriptingModelConfig;
import de.mpicbg.knime.scripting.core.exceptions.KnimeScriptingException;
import de.mpicbg.knime.scripting.python.v2.AbstractPythonScriptingV2NodeModel;

public class AbstractPythonPlotV2NodeModel extends AbstractPythonScriptingV2NodeModel {
	
	protected static final ImagePortObjectSpec IM_PORT_SPEC = new ImagePortObjectSpec(PNGImageContent.TYPE);

    public BufferedImage m_image;				// image created by Python(not required?)
    
	private File m_nodeImageFile;				// image file (png, temp-location => copy to internals)
    private File m_pyScriptFile;				// file contains python code to recreate the image (temp-location => copy to internals)
    private File m_shelveFile;					// file which stores the data needed to recreate the image (temp-location => copy to internals)	
	
    private static final String SHELVEFILE_LABEL = "shelveFile";
    private static final String IMGFILE_LABEL = "imgFile";
    
    private static final int EXECUTION_MODE = 0;
    private static final int REPAINT_MODE = 1;
    
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
		"# the following import is not required, as the node takes care of it \n" + 
		"#import matplotlib.pyplot as plt\n" + 
		"\n" + 
		"X = range(10)\n" + 
		"plt.plot(X, [x*x for x in X])\n" + 
		"plt.show()";
    
    public static final List<String> SUPPORTED_FORMATS = new LinkedList<String>(
    		Arrays.asList("png", "jpeg", "svg", "pdf", "tif"));

    
    
    
	public AbstractPythonPlotV2NodeModel(ScriptingModelConfig nodeModelConfig) {
		super(nodeModelConfig);
		
		this.addModelSetting(CFG_DPI, createDpiSM());
		this.addModelSetting(CFG_HEIGHT, createHeightSM());
		this.addModelSetting(CFG_WIDTH, createWidthSM());
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
	
	protected int getConfigDpi() {
		return ((SettingsModelIntegerBounded) getModelSetting(CFG_DPI)).getIntValue();
	}
	
	protected int getConfigWidth() {
		return ((SettingsModelIntegerBounded) getModelSetting(CFG_WIDTH)).getIntValue();
	}
	
	protected int getConfigHeight() {
		return ((SettingsModelIntegerBounded) getModelSetting(CFG_HEIGHT)).getIntValue();
	}
	
	protected String getConfigOutFileName() {
		return ((SettingsModelString) getModelSetting(CFG_OUTFILE)).getStringValue();
	}
	
	protected String getConfigImgFormat() {
		return ((SettingsModelString) getModelSetting(CFG_IMGTYPE)).getStringValue();
	}
	
	protected boolean getConfigWriteFlag() {
		return ((SettingsModelBoolean) getModelSetting(CFG_WRITE)).getBooleanValue();
	}
	
	protected boolean getConfigOverwriteFlag() {
		return ((SettingsModelBoolean) getModelSetting(CFG_OVERWRITE)).getBooleanValue();
	}
	

	/**
     * {@inheritDoc}
     * <br>
     * adds functionality to create + save image and to provide image output
	 * @throws CanceledExecutionException 
     */
	@Override
	protected PortObject[] pullOutputFromPython(ExecutionContext exec) throws KnimeScriptingException, CanceledExecutionException {
				
		PortObject[] outPorts = super.pullOutputFromPython(exec);
		
		for(int i = 0; i < getNrOutPorts(); i++) {
			PortType pType = this.getOutPortType(i);
			
			// create image for image port
			if(pType.equals(ImagePortObject.TYPE)) {
				try {
					// example
					BufferedImage buffered_image= ImageIO.read(m_nodeImageFile);
					ByteArrayOutputStream output_stream= new ByteArrayOutputStream();
					ImageIO.write(buffered_image, "png", output_stream);
					byte[] byte_array = output_stream.toByteArray();
					PNGImageContent content = new PNGImageContent(byte_array);
					outPorts[i] = new ImagePortObject(content, IM_PORT_SPEC);
				} catch (IOException ioe) {
					throw new KnimeScriptingException("Failed to create image out port: " + ioe.getMessage());
				}
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
        		throw new KnimeScriptingException("Failed to save image to file:\n" + e.getMessage());
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

        // copy temp-image
    	/*File imgFile = super.getTempFile(IMGFILE_LABEL);
    	
    	Files.copy(imgFile.toPath(), target, options)

        // create the image
        String script = prepareScript();
        m_image = createImage(script, getDefWidth(), getDefHeight(), getImageType());*/
    }
    
   

	@Override
	protected void loadInternals(File internDir, ExecutionMonitor exec) throws IOException, CanceledExecutionException {
		
		File internalImgfile = internDir.toPath().resolve("image.png").toFile();
		File internalShelveFile = internDir.toPath().resolve("shelve.db").toFile();
		
		if(!internalImgfile.canRead() || !internalShelveFile.canRead())
			throw new IOException("Failed to load node internals. Files missing or without read access.");
		
		try {
			createTempFiles(true);
		} catch (KnimeScriptingException kse) {
			throw new IOException(kse.getMessage());
		}
		
		//copy to temp location
		Files.copy(internalImgfile.toPath(), m_nodeImageFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
		Files.copy(internalShelveFile.toPath(), m_shelveFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
	}

	@Override
	protected void saveInternals(File internDir, ExecutionMonitor exec) throws IOException, CanceledExecutionException {
	
		Path imgInternal = internDir.toPath().resolve("image.png");// + FilenameUtils.getExtension(m_nodeImageFile.toString()));  	
    	Files.copy(m_nodeImageFile.toPath(), imgInternal, StandardCopyOption.REPLACE_EXISTING);
    	
    	// note: opening the shelve file with python adds a '.db' to the filename
		Path shelveInternal = internDir.toPath().resolve("shelve.db");  
		String shelvePath = m_shelveFile.getAbsolutePath() + ".db";
    	Files.copy(Paths.get(shelvePath), shelveInternal, StandardCopyOption.REPLACE_EXISTING);
    	
    	// create script for recreating the image
    	int dpi = getConfigDpi();
    	try {
			createScriptFor(REPAINT_MODE, dpi, (double)getConfigWidth()/dpi, (double)getConfigHeight()/dpi);
		} catch (KnimeScriptingException kse) {
			throw new IOException(kse);
		}
	}

	@Override
	protected PortObjectSpec[] configure(PortObjectSpec[] inSpecs) throws InvalidSettingsException {
		PortObjectSpec[] specs = super.configure(inSpecs);
		
		String format = getConfigImgFormat();
		String outFile = getConfigOutFileName();
		boolean write = getConfigWriteFlag();
		
		if(outFile != null && write) {
			String ext = FilenameUtils.getExtension(outFile);
			if(!ext.equals(format))
				this.setWarningMessage("Plot Export Settings: Selected file format '" + format + "' vs. file extension '" + ext +"'");
		}
		
		return specs;
	}

	@Override
	protected void onDispose() {
		super.onDispose();
		removeTempFiles();
	}
	
	@Override
	protected void removeTempFiles() {
		
		super.removeTempFiles();
		
		if(m_nodeImageFile != null)
			try {
				Files.deleteIfExists(m_nodeImageFile.toPath());
			} catch (IOException e) {
				e.printStackTrace();
			}
		if(m_pyScriptFile != null)
			try {
				Files.deleteIfExists(m_pyScriptFile.toPath());
			} catch (IOException e) {
				e.printStackTrace();
			}
		if(m_shelveFile != null)
			try {
				String shelvePath = m_shelveFile.getAbsolutePath() + ".db";
				Files.deleteIfExists(Paths.get(shelvePath));
			} catch (IOException e) {
				e.printStackTrace();
			}
	}

	protected BufferedImage createImage(String script, int defWidth, int defHeight, String imageType) {
		// TODO Auto-generated method stub
		return null;
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
  
    
    @Override
	protected void reset() {
		super.reset();
		
		removeTempFiles();
	}
    

	@Override
	protected void prepareScriptFile() throws KnimeScriptingException {
		
		int width = getConfigWidth();
		int height = getConfigHeight();
		int dpi = getConfigDpi();
		
		double width_inch = (double)width/(double)dpi;
		double height_inch = (double)height/(double)dpi;
		
		createTempFiles(false);
		
		createScriptFor(EXECUTION_MODE, dpi, width_inch, height_inch);

	}

	private void createScriptFor(int mode, int dpi, double width_inch, double height_inch)
			throws KnimeScriptingException {
		
		try(BufferedWriter scriptWriter = new BufferedWriter(new FileWriter(getScriptFile(), true))) {
			
			// additional imports
			String importString = "import matplotlib\n" + 
					"matplotlib.use('Agg')\n" + 
					"import matplotlib.pyplot as plt";
			
			scriptWriter.write(importString);
			scriptWriter.newLine();
			
			// basic imports, defintions, script
			if(mode == EXECUTION_MODE)
				super.prepareScript(scriptWriter, true, new PythonInputMode(m_shelveFile, PythonInputMode.Flag.WRITE));
			else
				super.prepareScript(scriptWriter, true, new PythonInputMode(m_shelveFile, PythonInputMode.Flag.READ));
			
			scriptWriter.newLine();
			scriptWriter.newLine();
			
			// plots for view and (optional) export
			
			String writeImageToFile = "";
			// if image should be exported as file
			if(mode == EXECUTION_MODE && getConfigWriteFlag() && getConfigOutFileName() != null) {
				String destination = getConfigOutFileName();
				boolean overwrite = getConfigOverwriteFlag();
				if(!overwrite) {
					if(new File(destination).exists())
						throw new KnimeScriptingException("Plot file already exists and may not be overwritten. Please check output settings.");
				}
				writeImageToFile = "plt.savefig(\"" + destination + "\", format=\"" + getConfigImgFormat() + "\")\n";	
			}
			
			String plotString = "F = plt.gcf()\n" + 
					"\n" + 
					"F.set_dpi(" + dpi + ")\n" + 
					"F.set_size_inches(" + width_inch + "," + height_inch + ")\n" + 
					"\n" + 
					writeImageToFile +
					"plt.savefig(\"" + m_nodeImageFile + "\", format=\"png\")\n" +
					"plt.close()";
			scriptWriter.write(plotString);
			
		} catch (IOException ioe) {
			throw new KnimeScriptingException("Failed to write script file: ", ioe.getMessage());
		}
	}

	protected void createTempFiles(boolean internal) throws KnimeScriptingException {
		
		// if called during execution => get random string from parent class
		// if called when loading internals => set random string to "internal"
		String randomPart = internal ? "internal" : getRandomPart();
		
		try {
			Path shelveFile = Files.createTempFile(randomPart + "_" + SHELVEFILE_LABEL + "_knime2python_", ".csv");
			Files.deleteIfExists(shelveFile);
			m_shelveFile = shelveFile.toFile();
			//addTempFile(SHELVEFILE_LABEL, shelveFile.toFile());
			
			Path imgFile = Files.createTempFile(randomPart + "_" + IMGFILE_LABEL + "_img2knime_", ".png");
			Files.deleteIfExists(imgFile);
			m_nodeImageFile = imgFile.toFile();
			//addTempFile(IMGFILE_LABEL, imgFile.toFile());
			
		} catch (IOException ioe) {
			removeTempFiles();
	    	throw new KnimeScriptingException("Failed to create temporary files: " + ioe.getMessage());
		}
	}

}
