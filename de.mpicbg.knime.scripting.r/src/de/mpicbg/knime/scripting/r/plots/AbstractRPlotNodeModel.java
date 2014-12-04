package de.mpicbg.knime.scripting.r.plots;

import de.mpicbg.knime.scripting.core.AbstractScriptingNodeModel;
import de.mpicbg.knime.scripting.core.FlowVarUtils;
import de.mpicbg.knime.scripting.core.TemplateConfigurator;
import de.mpicbg.knime.scripting.core.exceptions.KnimeScriptingException;
import de.mpicbg.knime.scripting.r.RPlotNodeFactory;
import de.mpicbg.knime.scripting.r.RSnippetNodeModel;
import de.mpicbg.knime.scripting.r.RUtils;

import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.defaultnodesettings.SettingsModelBoolean;
import org.knime.core.node.defaultnodesettings.SettingsModelInteger;
import org.knime.core.node.defaultnodesettings.SettingsModelString;
import org.knime.core.node.port.PortType;
import org.rosuda.REngine.REXPMismatchException;
import org.rosuda.REngine.REngineException;
import org.rosuda.REngine.Rserve.RConnection;
import org.rosuda.REngine.Rserve.RserveException;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.io.*;
import java.text.SimpleDateFormat;
import java.util.Date;


/**
 * Document me!
 *
 * @author Holger Brandl
 */
public class AbstractRPlotNodeModel extends AbstractScriptingNodeModel {

    public Image image;
    public File nodeImageFile;
    private File rWorkspaceFile;
    private File nodeRWorkspaceFile;

    protected SettingsModelInteger propWidth = RPlotNodeFactory.createPropFigureWidth();
    protected SettingsModelInteger propHeight = RPlotNodeFactory.createPropFigureHeight();
    protected SettingsModelString propOutputFile = RPlotNodeFactory.createPropOutputFile();
    protected SettingsModelBoolean propEnableFile = RPlotNodeFactory.createEnableFile();
    protected SettingsModelBoolean propOverwriteFile = RPlotNodeFactory.createOverwriteFile();
    private SettingsModelString propOutputType = RPlotNodeFactory.createPropOutputType();

    public static final String DEFAULT_R_PLOTCMD = "plot(1:10)";

    public static String TODAY = new SimpleDateFormat("yyMMdd").format(new Date(System.currentTimeMillis()));


    public AbstractRPlotNodeModel(PortType[] inPorts) {
        this(inPorts, new PortType[0]);
    }


    /**
     * This constructor justs needs to be used dirif a plot node should have additional data table outputs.
     */
    public AbstractRPlotNodeModel(PortType[] inPorts, PortType[] outports) {
        super(inPorts, outports);

        addSetting(propWidth);
        addSetting(propHeight);

        addSetting(propOutputFile);
        addSetting(propEnableFile);
        addSetting(propOverwriteFile);
        addSetting(propOutputType);
    }


    /**
     * Creates a figure using the R-variable in the current connection's workspace as input.
     * @throws KnimeScriptingException 
     */
    protected void createFigure(RConnection connection) throws RserveException, IOException, REXPMismatchException, REngineException, KnimeScriptingException {

        RUtils.saveToLocalFile(getTempWSFile(), connection, RUtils.getHost(), RSnippetNodeModel.R_INVAR_BASE_NAME);


        // 2) create the image the script
        String script = prepareScript();
        image = RUtils.createImage(connection, script, getDefWidth(), getDefHeight(), getDevice());

        // no need to save image to file
        if(!propEnableFile.getBooleanValue()) return;

        String fileName = prepareOutputFileName();
        
        // the plot should be written to file
        if (!fileName.isEmpty()) {
        	File imageFile = new File(fileName);
        	// check if the file already exists but should not be overwritten
        	if(imageFile.exists()) {
        		if(!propOverwriteFile.getBooleanValue())
        			throw new KnimeScriptingException("Overwrite file is disabled but image file '" + fileName + "' already exsists.");
        	} else {
        		try {
        			imageFile.createNewFile();
        		} catch(IOException e) {
        			throw new KnimeScriptingException("Output file '" + fileName + "' cannot be created. Please check the output path! (" + e.getMessage() + ")");
        		}
        	}       
            ImageIO.write(RPlotCanvas.toBufferedImage(image), "png", imageFile);
        }
    }


    private String prepareOutputFileName() {
        String fileName = propOutputFile.getStringValue();

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
        //return "jpeg";
        return propOutputType.getStringValue();
    }


    public int getDefHeight() {
        return propHeight.getIntValue();
    }


    public int getDefWidth() {
        return propWidth.getIntValue();
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
