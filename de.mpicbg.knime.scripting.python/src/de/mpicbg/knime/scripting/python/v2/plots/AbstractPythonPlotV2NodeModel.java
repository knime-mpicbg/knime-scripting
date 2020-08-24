package de.mpicbg.knime.scripting.python.v2.plots;

import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Arrays;
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

import de.mpicbg.knime.scripting.core.ScriptingModelConfig;
import de.mpicbg.knime.scripting.core.exceptions.KnimeScriptingException;
import de.mpicbg.knime.scripting.python.v2.AbstractPythonScriptingV2NodeModel;

public class AbstractPythonPlotV2NodeModel extends AbstractPythonScriptingV2NodeModel {
	
	protected static final ImagePortObjectSpec IM_PORT_SPEC = new ImagePortObjectSpec(PNGImageContent.TYPE);

	private File m_nodeImageFile;				// image file (png, temp-location => copy to internals)
    private File m_pyScriptFile;				// file contains python code to recreate the image (temp-location => copy to internals)
    private File m_pickleFile;					// file which stores the data needed to recreate the image (temp-location => copy to internals)	
	
    private static final String PICKLEFILE_LABEL = "pickleFile";
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
		"# the following tow lines are only required in 'open external' mode \n" + 
    	"#%matplotlib inline\n" +
		"#import matplotlib.pyplot as plt\n" + 
		"\n" + 
		"X = range(10)\n" + 
		"plt.plot(X, [x*x for x in X])\n";
    
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
     */
    @Override
    public String getDefaultScript(String defaultScript) {
        return super.getDefaultScript(DEFAULT_PYTHON_PLOTCMD);
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
	

	   

	@Override
	protected void loadInternals(File internDir, ExecutionMonitor exec) throws IOException, CanceledExecutionException {
		
		File internalImgfile = internDir.toPath().resolve("image.png").toFile();
		File internalPickleFile = internDir.toPath().resolve("pickle.p").toFile();
		
		if(!internalImgfile.canRead() || !internalPickleFile.canRead())
			throw new IOException("Failed to load node internals. Files missing or without read access.");
		
		try {
			createTempFiles(true);
		} catch (KnimeScriptingException kse) {
			throw new IOException(kse.getMessage());
		}
		
		//copy to temp location
		Files.copy(internalImgfile.toPath(), m_nodeImageFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
		Files.copy(internalPickleFile.toPath(), m_pickleFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
		
		Path inKeys = internDir.toPath().resolve("inputKeys.csv");
		try( FileReader reader = new FileReader(inKeys.toFile()); BufferedReader br = new BufferedReader(reader); ) {
			String[] keys = br.readLine().split(",");
			
			super.setInputKeys(new ArrayList<String>(Arrays.asList(keys)));
		}
	}

	@Override
	protected void saveInternals(File internDir, ExecutionMonitor exec) throws IOException, CanceledExecutionException {
	
		Path imgInternal = internDir.toPath().resolve("image.png");// + FilenameUtils.getExtension(m_nodeImageFile.toString()));  	
    	Files.copy(m_nodeImageFile.toPath(), imgInternal, StandardCopyOption.REPLACE_EXISTING);
    	
		Path pickleInternal = internDir.toPath().resolve("pickle.p");  
    	Files.copy(m_pickleFile.toPath(), pickleInternal, StandardCopyOption.REPLACE_EXISTING);
    	
    	Path inKeys = internDir.toPath().resolve("inputKeys.csv");
    	List<String> inList = super.getInputKeys();
    	try( FileWriter writer = new FileWriter(inKeys.toFile()); ) {
    		writer.write(String.join(",", inList));
    		writer.flush();
    		writer.close();
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
		if(m_pickleFile != null)
			try {
				Files.deleteIfExists(m_pickleFile.toPath());
			} catch (IOException e) {
				e.printStackTrace();
			}
	}
    
    public BufferedImage getImage() throws IOException {
    	
    	BufferedImage image = null;
    	
    	if (m_nodeImageFile != null && m_nodeImageFile.isFile()) {
    		logger.warn("Restoring image from disk. This might take a few seconds...");
    		image = ImageIO.read(m_nodeImageFile);
    	}

        return image;
    }
 
    @Override
	protected void reset() {
		super.reset();
		//removeTempFiles();	
		// would make problems when 'open external' as 
		// temp files would be removed too early
	}
    

	@Override
	protected void prepareScriptFile() throws KnimeScriptingException {
		
		int width = getConfigWidth();
		int height = getConfigHeight();
		int dpi = getConfigDpi();
		
		double width_inch = (double)width/(double)dpi;
		double height_inch = (double)height/(double)dpi;
		
		createTempFiles(false);
		
		createScriptFor(getScriptFile(), m_nodeImageFile, EXECUTION_MODE, dpi, width_inch, height_inch);

	}

	private void createScriptFor(File scriptFile, File imgFile, int mode, int dpi, double width_inch, double height_inch)
			throws KnimeScriptingException {
		
		try(BufferedWriter scriptWriter = new BufferedWriter(new FileWriter(scriptFile, true))) {
			
			// additional imports
			String importString = "import matplotlib\n" + 
					"matplotlib.use('Agg')\n" + 
					"import matplotlib.pyplot as plt";
			
			scriptWriter.write(importString);
			scriptWriter.newLine();
			
			// basic imports, defintions, script
			if(mode == EXECUTION_MODE)
				super.prepareScript(scriptWriter, true, new PythonInputMode(m_pickleFile, PythonInputMode.Flag.WRITE));
			else
				super.prepareScript(scriptWriter, true, new PythonInputMode(m_pickleFile, PythonInputMode.Flag.READ));
			
			scriptWriter.newLine();
			scriptWriter.newLine();
			
			// plots for view and (optional) export
			
			String writeImageToFile = "";
			// if image should be exported as file
			if(mode == EXECUTION_MODE && getConfigWriteFlag() && getConfigOutFileName() != null) {
				
				String destination = getConfigOutFileName();
				destination = prepareOutputFileName(destination);
				
				boolean overwrite = getConfigOverwriteFlag();
				if(!overwrite) {
					if(new File(destination).exists())
						throw new KnimeScriptingException("Plot file already exists and may not be overwritten. Please check output settings.");
				}
				
				if(isWindowsPlatform)
					destination  = destination.replace('\\', '/');
				writeImageToFile = "plt.savefig(\"" + destination + "\", format=\"" + getConfigImgFormat() + "\")\n";	
			}
			
			String imgFilename = imgFile.getAbsolutePath();
			if(isWindowsPlatform)
				imgFilename  = imgFilename.replace('\\', '/');
			
			String plotString = "F = plt.gcf()\n" + 
					"\n" + 
					"F.set_dpi(" + dpi + ")\n" + 
					"F.set_size_inches(" + width_inch + "," + height_inch + ")\n" + 
					"\n" + 
					writeImageToFile +
					"plt.savefig(\"" + imgFilename + "\", format=\"png\")\n" +
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
			Path pickleFile = Files.createTempFile(randomPart + "_" + PICKLEFILE_LABEL + "_knime2python_", ".p");
			Files.deleteIfExists(pickleFile);
			m_pickleFile = pickleFile.toFile();
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

	public BufferedImage getRecreatedImage(int width, int height) throws IOException, KnimeScriptingException{
		
		Path recreateScriptFile = null;
		Path recreatedImageFile = null;
		BufferedImage recreatedImage = null;
		
    	int dpi = getConfigDpi();
    	
    	try {
    		// create temp files - Python script to repaint image, image file
			recreateScriptFile = Files.createTempFile("knime2python_recreateScript", ".py");
			recreatedImageFile = Files.createTempFile("knime2python_recreatedImage", ".png");
			
			createScriptFor(recreateScriptFile.toFile(), recreatedImageFile.toFile(), REPAINT_MODE, dpi, (double)width/dpi, (double)height/dpi);
			
	    	// run python script
			runScriptImpl(recreateScriptFile.toFile());
			
			recreatedImage = ImageIO.read(recreatedImageFile.toFile());
			
		} finally {
			// delete temp files if they exist
			if(recreatedImageFile != null) {
				try {
					Files.deleteIfExists(recreatedImageFile);
				} catch (IOException ioe) {
					ioe.printStackTrace();
				}
			}
			if(recreateScriptFile != null) {
				try {
					Files.deleteIfExists(recreateScriptFile);
				} catch (IOException ioe) {
					ioe.printStackTrace();
				}
			}
		}
    	
		return recreatedImage;
	}

	@Override
	protected PortObject[] executeImpl(PortObject[] inData, ExecutionContext exec) throws Exception {
		
		removeTempFiles();
		return super.executeImpl(inData, exec);
	}

}
