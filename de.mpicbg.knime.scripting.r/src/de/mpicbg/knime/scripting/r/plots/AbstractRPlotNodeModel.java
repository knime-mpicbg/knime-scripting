package de.mpicbg.knime.scripting.r.plots;

import java.awt.Image;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;

import javax.imageio.ImageIO;
import javax.swing.ImageIcon;

import org.knime.core.data.image.png.PNGImageContent;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.defaultnodesettings.SettingsModelBoolean;
import org.knime.core.node.defaultnodesettings.SettingsModelIntegerBounded;
import org.knime.core.node.defaultnodesettings.SettingsModelString;
import org.knime.core.node.port.PortType;
import org.knime.core.node.port.image.ImagePortObjectSpec;
import org.rosuda.REngine.REXPMismatchException;
import org.rosuda.REngine.REngineException;
import org.rosuda.REngine.Rserve.RConnection;
import org.rosuda.REngine.Rserve.RserveException;

import de.mpicbg.knime.scripting.core.AbstractScriptingNodeModel;
import de.mpicbg.knime.scripting.core.FlowVarUtils;
import de.mpicbg.knime.scripting.core.TemplateConfigurator;
import de.mpicbg.knime.scripting.core.exceptions.KnimeScriptingException;
import de.mpicbg.knime.scripting.r.RColumnSupport;
import de.mpicbg.knime.scripting.r.RUtils;
import de.mpicbg.knime.scripting.r.node.plot.RPlotCanvas;
import de.mpicbg.knime.scripting.r.node.snippet.RSnippetNodeModel;


/**
 * Document me!
 *
 * @author Holger Brandl
 */
public abstract class AbstractRPlotNodeModel extends AbstractScriptingNodeModel {
	
	protected static final ImagePortObjectSpec IM_PORT_SPEC = new ImagePortObjectSpec(PNGImageContent.TYPE);

    public Image image;
    public File nodeImageFile;
    private File rWorkspaceFile;
    private File nodeRWorkspaceFile;
    
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


    public AbstractRPlotNodeModel(PortType[] inPorts) {
        this(inPorts, new PortType[0]);
    }


    /**
     * This constructor just needs to be used if a plot node should have additional data table outputs.
     */
    public AbstractRPlotNodeModel(PortType[] inPorts, PortType[] outports) {
        super(inPorts, outports, new RColumnSupport());
        
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
     * Creates a figure using the R-variable in the current connection's workspace as input.
     * @throws KnimeScriptingException 
     */
    protected void createFigure(RConnection connection) throws RserveException, IOException, REXPMismatchException, REngineException, KnimeScriptingException {

        // workspace file saved as internal and used to recreate image 
        RUtils.saveWorkspaceToFile(getTempWSFile(), connection, RUtils.getHost());

        // create the image the script
        String script = prepareScript();
        image = RUtils.createImage(connection, script, getDefWidth(), getDefHeight(), getDevice());
        
        boolean enableFileOutput = ((SettingsModelBoolean) getModelSetting(CFG_WRITE)).getBooleanValue();
        // no need to save image to file ?
        if(!enableFileOutput) return;
        
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

            FileOutputStream fsOut = new FileOutputStream(new File(fileName));
            ImageIO.write(RPlotCanvas.toBufferedImage(image), "png", fsOut);
            fsOut.close();

        }
    }


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


    private File getTempWSFile() throws IOException {

        if (rWorkspaceFile == null) {
            // note: this 'R' is not a workspace variable name but a file suffix
            rWorkspaceFile = File.createTempFile("genericR", "R");
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

            RUtils.copyFile(rWorkspaceFile, f);
        }

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
            FileInputStream f_in = new FileInputStream(nodeImageFile);

            // Read object using ObjectInputStream
            ObjectInputStream obj_in = new ObjectInputStream(new BufferedInputStream(f_in));

            // Read an object
            image = ((ImageIcon) obj_in.readObject()).getImage();
        }
    }
    
}
