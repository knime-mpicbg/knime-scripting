package de.mpicbg.knime.scripting.r.plots;

import java.awt.Image;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.text.SimpleDateFormat;
import java.util.Date;

import javax.imageio.ImageIO;

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
import org.rosuda.REngine.REXP;
import org.rosuda.REngine.REXPMismatchException;
import org.rosuda.REngine.Rserve.RConnection;
import org.rosuda.REngine.Rserve.RserveException;

import de.mpicbg.knime.scripting.core.AbstractScriptingNodeModel;
import de.mpicbg.knime.scripting.core.FlowVarUtils;
import de.mpicbg.knime.scripting.core.ScriptingModelConfig;
import de.mpicbg.knime.scripting.core.TemplateConfigurator;
import de.mpicbg.knime.scripting.core.exceptions.KnimeScriptingException;
import de.mpicbg.knime.scripting.r.AbstractRScriptingNodeModel;
import de.mpicbg.knime.scripting.r.R4KnimeBundleActivator;
import de.mpicbg.knime.scripting.r.RUtils;
import de.mpicbg.knime.scripting.r.node.plot.RPlotCanvas;
import de.mpicbg.knime.scripting.r.prefs.RPreferenceInitializer;


/**
 * Document me!
 *
 * @author Holger Brandl
 */
public abstract class AbstractRPlotNodeModel extends AbstractRScriptingNodeModel {
	
	protected static final ImagePortObjectSpec IM_PORT_SPEC = new ImagePortObjectSpec(PNGImageContent.TYPE);

    public BufferedImage image;						// image created by R
    public File nodeImageFile;				// image file (internals)
    private File rWorkspaceFile;			// workspace file (internals)
    private File nodeRWorkspaceFile;		// is that necessary??
    
    /**
     * MODEL - SETTINGS
     */
    public static final String CFG_WIDTH = "figure.width";
    public static final int CFG_WIDTH_DFT = 1000;
    
    public static final String CFG_HEIGHT = "figure.height";
    public static final int CFG_HEIGHT_DFT = 700;
    
    public static final String CFG_OUTFILE = "figure.output.file";
    
    public static final String CFG_OVERWRITE = "overwrite.ok";
    public static final boolean CFG_OVERWRITE_DFT = false;
    
    public static final String CFG_WRITE = "write.output.file";
    public static final boolean CFG_WRITE_DFT = true;
    
    public static final String CFG_IMGTYPE = "figure.ouput.type";
    public static final String CFG_IMGTYPE_DFT = "png";
   

    public static final String DEFAULT_R_PLOTCMD = "plot(1:10)";

    public static String TODAY = new SimpleDateFormat("yyMMdd").format(new Date(System.currentTimeMillis()));

    public AbstractRPlotNodeModel(ScriptingModelConfig nodeModelConfig) {
		super(nodeModelConfig);
		
		this.addModelSetting(CFG_HEIGHT, createHeightSM());
        this.addModelSetting(CFG_WIDTH, createWidthSM());
        this.addModelSetting(CFG_IMGTYPE, createImgTypeSM());
        this.addModelSetting(CFG_OUTFILE, createOutputFileSM());
        this.addModelSetting(CFG_OVERWRITE, createOverwriteSM());
        this.addModelSetting(CFG_WRITE, createWriteFileSM());
	}

	@Override
	protected PortObject[] pullOutputFromR(ExecutionContext exec) throws KnimeScriptingException {
		
		try {
			createInternals(m_con);
			saveFigureAsFile();
		} catch (KnimeScriptingException e) {
			closeRConnection();
			throw e;
		}
		
		// retrieve table + generic outputs if present
		PortObject[] outPorts = super.pullOutputFromR(exec);
		
		for(int i = 0; i < getNrOutPorts(); i++) {
			PortType pType = this.getOutPortType(i);
			
			// create image for image port
			if(pType.equals(ImagePortObject.TYPE)) {
				ByteArrayOutputStream baos = new ByteArrayOutputStream();
				try {
					ImageIO.write(image, "png", baos);
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
	private void saveFigureAsFile() throws KnimeScriptingException {
		boolean enableFileOutput = ((SettingsModelBoolean) getModelSetting(CFG_WRITE)).getBooleanValue();
        // no need to save image to file ?
        if(!enableFileOutput) return;
        
        assert image != null;
        
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
	            ImageIO.write(RPlotCanvas.toBufferedImage(image), "png", fsOut);
	            fsOut.close();
        	} catch (IOException e) {
        		throw new KnimeScriptingException("Failed to sava image to file:\n" + e.getMessage());
        	}

        }
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
     * Creates a figure using the R-variable in the current connection's workspace as input.
	 * @throws IOException 
     * @throws KnimeScriptingException 
     */
    protected void createInternals(RConnection connection) throws KnimeScriptingException {

        // workspace file saved as internal and used to recreate image 
    	try {
    		RUtils.saveWorkspaceToFile(getTempWSFile(), connection, RUtils.getHost());
    	} catch (IOException e) {
    		throw new KnimeScriptingException(e.getMessage());
    	}

        // create the image the script
        String script = prepareScript();
        image = createImage(connection, script, getDefWidth(), getDefHeight(), getDevice());
    }
    
    /**
     * run R script to save the plot as a temporary file
     * note: connection is not closed when exceptions occur
     * @param connection
     * @param script
     * @param width
     * @param height
     * @param device
     * @return
     * @throws KnimeScriptingException
     */
    public static BufferedImage createImage(RConnection connection, String script, int width, int height, String device) 
			throws KnimeScriptingException {

		// check preferences
		boolean useEvaluate = R4KnimeBundleActivator.getDefault().getPreferenceStore().getBoolean(RPreferenceInitializer.USE_EVALUATE_PACKAGE);

		String tempFileName = "rmPlotFile." + device;

		// create plot device on R side
		String deviceArgs = device.equals("jpeg") ? "quality=97," : "";
		REXP xp = null;
		String openDevice = "try(" + device + "('" + tempFileName + "'," + deviceArgs + " width = " + width + ", height = " + height + "))";
		try {
			xp = connection.eval(openDevice);
			if (xp.inherits("try-error")) { // if the result is of the class try-error then there was a problem
				// this is analogous to 'warnings', but for us it's sufficient to get just the 1st warning
				REXP w = connection.eval("if (exists('last.warning') && length(last.warning)>0) names(last.warning)[1] else 0");
				if (w.isString()) System.err.println(w.asString()); {
					throw new KnimeScriptingException("Can't open " + device + " graphics device:\n" + xp.asString());
				}
			}
		} catch (RserveException | REXPMismatchException e) {
			throw new KnimeScriptingException("Failed to open image device from R:\n" + openDevice);
		}
		// the device should be fine

		// parse script
		String preparedScript = AbstractScriptingNodeModel.fixEncoding(script);
		try {
			parseScript(connection, preparedScript);
		} catch (RserveException | KnimeScriptingException | REXPMismatchException e) {
			throw new KnimeScriptingException("Failed to parse the script:\n" + e.getMessage());
		}

		// evaluate script
		try {
			if(useEvaluate) {
				// parse and run script
				// evaluation list, can be used to create a console view
				evaluateScript(preparedScript, connection);
			} else {
				// parse and run script
				evalScript(connection, preparedScript);
			}
		} catch (RserveException | REXPMismatchException | KnimeScriptingException e) {
        	throw new KnimeScriptingException("Failed to evaluate the script:\n" + e.getMessage());
        }

		// close the image
		byte[] image = null;
		try {
			connection.eval("dev.off();");
			// check if the plot file has been written
			int xpInt = connection.eval("file.access('" + tempFileName + "',0)").asInteger();
			if(xpInt == -1) throw new KnimeScriptingException("Plot could not be created. Please check your script");
	
			// we limit the file size to 1MB which should be sufficient and we delete the file as well
			xp = connection.eval("try({ binImage <- readBin('" + tempFileName + "','raw',2024*2024); unlink('" + tempFileName + "'); binImage })");
	
			if (xp.inherits("try-error")) { // if the result is of the class try-error then there was a problem
				throw new KnimeScriptingException(xp.asString());
			}
			
			image = xp.asBytes();
			
		} catch (RserveException | REXPMismatchException e) {
			throw new KnimeScriptingException("Failed to close image device and to read in plot as binary:+\n" + e.getMessage());
		}
		
		BufferedImage img = null;
		try {
			img = ImageIO.read(new ByteArrayInputStream(image));
		} catch (IOException e) {
			throw new KnimeScriptingException(e.getMessage());
		}

		// now this is pretty boring AWT stuff - create an image from the data and display it ...
		//return Toolkit.getDefaultToolkit().createImage(image);
		return img;
	}

    /**
     * replace placeholders in filename with appropriate values
     * @param fileName
     * @return final filename
     */
    private String prepareOutputFileName(String fileName) {
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
     * @return file handle for temporary workspace file
     * @throws IOException
     */
    private File getTempWSFile() throws IOException {

        if (rWorkspaceFile == null) {
            // note: this 'R' is not a workspace variable name but a file suffix
            rWorkspaceFile = File.createTempFile("genericR", ".RData");
        }
        return rWorkspaceFile;
    }


    public String getDevice() {
        return ((SettingsModelString) getModelSetting(CFG_IMGTYPE)).getStringValue();
    }


    public int getDefHeight() {
        return ((SettingsModelIntegerBounded) getModelSetting(CFG_HEIGHT)).getIntValue();
    }


    public int getDefWidth() {
    	return ((SettingsModelIntegerBounded) getModelSetting(CFG_WIDTH)).getIntValue();
    }


    @Override
    public String getDefaultScript() {
        if (getHardwiredTemplate() == null) {
            return DEFAULT_R_PLOTCMD;
        } else {
            return TemplateConfigurator.generateScript(getHardwiredTemplate());
        }
    }


    public File getWSFile() {
        if (rWorkspaceFile == null && nodeRWorkspaceFile != null && nodeRWorkspaceFile.isFile()) {
            logger.warn("Using persisted data from disk. This might take a few seconds...");
            return nodeRWorkspaceFile;
        }

        return rWorkspaceFile;
    }


    public Image getImage() {
        try {
            if (image == null && nodeImageFile != null && nodeImageFile.isFile()) {
                logger.warn("Restoring image from disk. This might take a few seconds...");
                deserializeImage();
            }
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }

        return image;
    }


    public void setPlotWarning() {
        setWarningMessage("You need to re-execute the node before the view will show up");
    }


    @Override
    protected void saveInternals(File nodeDir, ExecutionMonitor executionMonitor) throws IOException, CanceledExecutionException {
        if (rWorkspaceFile != null) {
            File f = new File(nodeDir, "pushtable.R");

            Files.copy(rWorkspaceFile.toPath(), f.toPath());
        }

        if (image != null) {
            File imageFile = new File(nodeDir, "image.bin");
            
            ImageIO.write(image, "png", imageFile);
        }
    }


    @Override
    protected void loadInternals(File nodeDir, ExecutionMonitor executionMonitor) throws IOException, CanceledExecutionException {
        super.loadInternals(nodeDir, executionMonitor);

        try {

            nodeRWorkspaceFile = new File(nodeDir, "pushtable.R");
            nodeImageFile = new File(nodeDir, "image.bin");

        } catch (Throwable ignored) {
        }
    }


    private void deserializeImage() throws IOException, ClassNotFoundException {
        if (nodeImageFile.isFile()) {

            image = ImageIO.read(nodeImageFile);
        }
    }
    
}
