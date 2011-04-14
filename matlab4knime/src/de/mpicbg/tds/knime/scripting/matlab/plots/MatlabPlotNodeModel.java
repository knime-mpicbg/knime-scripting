package de.mpicbg.tds.knime.scripting.matlab.plots;

import de.mpicbg.math.toolintegration.matlab.MatlabTempFile;
import de.mpicbg.tds.knime.knutils.scripting.FlowVarUtils;
import de.mpicbg.tds.knime.knutils.scripting.TemplateConfigurator;
import de.mpicbg.tds.knime.scripting.matlab.AbstractMatlabScriptingNodeModel;
import de.mpicbg.tds.knime.scripting.matlab.TableConverter;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.image.png.PNGImageContent;
import org.knime.core.node.*;
import org.knime.core.node.defaultnodesettings.SettingsModelBoolean;
import org.knime.core.node.defaultnodesettings.SettingsModelInteger;
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
 * Document me!
 *
 * @author Tom Haux
 */
public class MatlabPlotNodeModel extends AbstractMatlabScriptingNodeModel {

    private Image image;
    private SettingsModelInteger propWidth = MatlabPlotNodeFactory.createPropFigureWidth();
    private SettingsModelInteger propHeight = MatlabPlotNodeFactory.createPropFigureHeight();
    private SettingsModelString propOutputFile = MatlabPlotNodeFactory.createPropOutputFile();
    private SettingsModelBoolean propOverwriteFile = MatlabPlotNodeFactory.createOverwriteFile();
    private final String DEFAULT_MATLAB_PLOTCMD = "% The command 'figureHandle = figure(...)' will be run prior to these commands.\nplot(kIn);";
    private static String TODAY = new SimpleDateFormat("yyMMdd").format(new Date(System.currentTimeMillis()));
    protected static final ImagePortObjectSpec IM_PORT_SPEC = new ImagePortObjectSpec(PNGImageContent.TYPE);


    //
    // Constructiors
    //
    public MatlabPlotNodeModel() {
        this(createPorts(1), new PortType[]{ImagePortObject.TYPE});
    }

    public MatlabPlotNodeModel(PortType[] inPorts) {
        this(inPorts, new PortType[]{ImagePortObject.TYPE});
    }

    public MatlabPlotNodeModel(PortType[] inPorts, PortType[] outports) {
        super(inPorts, outports);

        addSetting(propWidth);
        addSetting(propHeight);
        addSetting(propOutputFile);
        addSetting(propOverwriteFile);
    }


    /**
     * {@inheritDoc}
     */
    @Override
    protected PortObjectSpec[] configure(final PortObjectSpec[] inSpecs) throws InvalidSettingsException {
        configure(new DataTableSpec[]{(DataTableSpec) inSpecs[0]});
        return new PortObjectSpec[]{IM_PORT_SPEC};
    }


    @Override
    protected PortObject[] execute(PortObject[] inData, ExecutionContext exec) throws Exception {

        logger.info("Render the Matlab Plot");
        logger.info("Converting inputs into matlab-format");

        establishConnection();

        // Lock the server and assess the initial workspace
        matlab.lockServer();
        String[] initialWorkspace = matlab.assessWorkspace();

        try {
            // Create the tempfiles on Server and Client
            transferFile = new MatlabTempFile(matlab, "matlabplot", ".png");
            logger.info("remote image file: " + transferFile.getServerPath());
            logger.info("local image file: " + transferFile.getClientPath());

            // Convert exampleSet into matlab-structures and put into the remote matlab-workspace
            TableConverter.pushData2Matlab(matlab, (BufferedDataTable) inData[0], "kIn");

            // Create the plot script
            String script = preparePlotScript(transferFile);

            // Execute the script
            executeMatlabScript(script);

            // Fetch the image file form the serever and load it
            transferFile.fetch();
            image = MatlabPlotCanvas.toBufferedImage(new ImageIcon(transferFile.getClientPath()).getImage());
            transferFile.delete();

            // Prepare the image file for KNIME to display
            String fileName = prepareOutputFileName();
            if (!fileName.isEmpty()) {
                if (!propOverwriteFile.getBooleanValue() && new File(fileName).exists()) {
                    throw new RuntimeException("Overwrite file is disabled but image file '" + fileName + "' already exists");
                }
                ImageIO.write((BufferedImage) image, "png", new File(fileName));
            }

            // Create the image port object
            PNGImageContent content;
            File m_imageFile = File.createTempFile("matlabportImage", ".png");
            ImageIO.write(MatlabPlotCanvas.toBufferedImage(image), "png", m_imageFile);
            FileInputStream in = new FileInputStream(m_imageFile);
            content = new PNGImageContent(in);
            in.close();
            PortObject[] outPorts = new PortObject[1];
            outPorts[0] = new ImagePortObject(content, IM_PORT_SPEC);
            return outPorts;

        } catch (Throwable e) {
            throw new Exception(e);
        } finally {
            matlab.recessWorkspace(initialWorkspace);
            matlab.unlockServer();
        }
    }


    public int getDefHeight() {
        return propHeight.getIntValue();
    }


    public int getDefWidth() {
        return propWidth.getIntValue();
    }


    private String preparePlotScript(MatlabTempFile file) {
        String script = "figureHandle = figure('visible', 'off', 'units', 'pixels', 'position', [0, 0, " + getDefWidth() + ", " + getDefHeight() + "]);";
        script += "\nset(gcf,'PaperPositionMode','auto');\n";
        // Stick in the user defined code
        script += prepareScript();
        // Create a matlab var called plotfile* to store the temporary file path in it.
        String tempvar = "plotfile" + new Date().getTime();
        script += "\n" + tempvar + "='" + file.getServerPath() + "';";
        script += "\nprint(figureHandle, '-dpng', " + tempvar + ");";
//        script += "saveas(figureHandle," + tempvar + ")";
        logger.info(script);
        return script;
    }


    public Image getImage() {
        return image;
    }


    private String prepareOutputFileName() {
        String fileName = propOutputFile.getStringValue();
        // process flow-variables
        fileName = FlowVarUtils.replaceFlowVars(fileName, this);
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


    @Override
    public String getDefaultScript() {
        if (getHardwiredTemplate() == null) {
            return DEFAULT_MATLAB_PLOTCMD;
        } else {
            return TemplateConfigurator.generateScript(getHardwiredTemplate());
        }
    }


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
        } catch (Throwable ignored) {
        }
    }

}
