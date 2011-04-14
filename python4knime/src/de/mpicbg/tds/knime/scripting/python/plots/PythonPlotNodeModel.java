package de.mpicbg.tds.knime.scripting.python.plots;

import de.mpicbg.sweng.pythonserver.CommandOutput;
import de.mpicbg.sweng.pythonserver.LocalPythonClient;
import de.mpicbg.sweng.pythonserver.PythonClient;
import de.mpicbg.sweng.pythonserver.PythonTempFile;
import de.mpicbg.tds.knime.knutils.scripting.FlowVarUtils;
import de.mpicbg.tds.knime.knutils.scripting.TemplateConfigurator;
import de.mpicbg.tds.knime.scripting.python.AbstractPythonScriptingNodeModel;
import de.mpicbg.tds.knime.scripting.python.PythonScriptingBundleActivator;
import de.mpicbg.tds.knime.scripting.python.PythonTableConverter;
import de.mpicbg.tds.knime.scripting.python.prefs.PythonPreferenceInitializer;
import org.eclipse.jface.preference.IPreferenceStore;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.image.png.PNGImageContent;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.InvalidSettingsException;
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
public class PythonPlotNodeModel extends AbstractPythonScriptingNodeModel {
    private Image image;

    private SettingsModelInteger propWidth = PythonPlotNodeFactory.createPropFigureWidth();
    private SettingsModelInteger propHeight = PythonPlotNodeFactory.createPropFigureHeight();
    private SettingsModelString propOutputFile = PythonPlotNodeFactory.createPropOutputFile();
    private SettingsModelBoolean propOverwriteFile = PythonPlotNodeFactory.createOverwriteFile();

    private static String TODAY = new SimpleDateFormat("yyMMdd").format(new Date(System.currentTimeMillis()));

    private final String DEFAULT_PYTHON_PLOTCMD = "plot(kIn)";

    protected static final ImagePortObjectSpec IM_PORT_SPEC = new ImagePortObjectSpec(PNGImageContent.TYPE);

    /**
     * {@inheritDoc}
     */
    @Override
    protected PortObjectSpec[] configure(final PortObjectSpec[] inSpecs) throws InvalidSettingsException {
        configure(new DataTableSpec[]{(DataTableSpec) inSpecs[0]});

        return new PortObjectSpec[]{IM_PORT_SPEC};
    }

    public PythonPlotNodeModel() {
        this(createPorts(1), new PortType[]{ImagePortObject.TYPE});
    }

    public PythonPlotNodeModel(PortType[] inPorts) {
        this(inPorts, new PortType[]{ImagePortObject.TYPE});
    }

    public PythonPlotNodeModel(PortType[] inPorts, PortType[] outports) {
        super(inPorts, outports);

        addSetting(propWidth);
        addSetting(propHeight);

        addSetting(propOutputFile);
        addSetting(propOverwriteFile);
    }

    protected void prepareScript(Writer writer) throws IOException {
        writer.write("import matplotlib\nmatplotlib.use('Agg')\nfrom pylab import *\n");

        super.prepareScript(writer);

        float dpi = 100;
        float width = getDefWidth() / dpi;
        float height = getDefHeight() / dpi;

        writer.write("F = gcf()\n");
        writer.write("F.set_dpi(" + dpi + ")\n");
        writer.write("F.set_size_inches(" + width + "," + height + ")");
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

    @Override
    protected PortObject[] execute(PortObject[] inData, ExecutionContext exec) throws Exception {

        // Determine what kind of connection to instantiate (local or remote)
        IPreferenceStore preferences = PythonScriptingBundleActivator.getDefault().getPreferenceStore();
        boolean local = preferences.getBoolean(PythonPreferenceInitializer.PYTHON_LOCAL);
        String host = preferences.getString(PythonPreferenceInitializer.PYTHON_HOST);
        int port = preferences.getInt(PythonPreferenceInitializer.PYTHON_PORT);

        // If the host is empty use a local client, otherwise use the server values
        python = local ? new LocalPythonClient() : new PythonClient(host, port);

        // Create the temp files that are needed throughout the node
        createTempFiles();

        // Don't need the output table
        pyOutFile.delete();
        pyOutFile = null;

        // Prepare the script
        Writer writer = new BufferedWriter(new FileWriter(scriptFile.getClientFile()));
        prepareScript(writer);

        // Add plot-specific commands
        PythonTempFile imageFile = new PythonTempFile(python, "pyplot", ".png");
        writer.write("\nsavefig('" + imageFile.getServerPath() + "')\n");
        writer.close();

        // Copy the script file to the server
        scriptFile.upload();

        // Write the input table and upload it
        PythonTableConverter.convertTableToCSV(exec, (BufferedDataTable) inData[0], kInFile.getClientFile(), logger);
        kInFile.upload();

        // Run the script
        String pythonExecPath = local ? preferences.getString(PythonPreferenceInitializer.PYTHON_EXECUTABLE) : "python";
        CommandOutput output = python.executeCommand(new String[]{pythonExecPath, scriptFile.getServerPath()});

        // Log any output
        for (String o : output.getStandardOutput()) {
            logger.info(o);
        }

        for (String o : output.getErrorOutput()) {
            logger.error(o);
        }

        // Copy back the remote image
        imageFile.fetch();

        // If the file wasn't found throw an exception
        if (!imageFile.getClientFile().exists() || imageFile.getClientFile().length() == 0)
            throw new RuntimeException("No output image found");

        // Prepare it for the node view
        image = PythonPlotCanvas.toBufferedImage(new ImageIcon(imageFile.getClientPath()).getImage());

        // Write the image to a file if desired
        String fileName = prepareOutputFileName();
        if (!fileName.isEmpty()) {
            if (!propOverwriteFile.getBooleanValue() && new File(fileName).exists()) {
                throw new RuntimeException("Image file '" + fileName + "' already exists, enable overwrite to replace it");
            }


            try {
                ImageIO.write((BufferedImage) image, "png", new File(fileName));
            } catch (Throwable t) {
                throw new RuntimeException("Error writing image file '" + fileName);
            }
        }

        // Clean up temp files
        deleteTempFiles();
        imageFile.delete();

        // Create the image port object
        PNGImageContent content;
        File m_imageFile = File.createTempFile("pythonImage", ".png");
        ImageIO.write(PythonPlotCanvas.toBufferedImage(image), "png", m_imageFile);
        FileInputStream in = new FileInputStream(m_imageFile);
        content = new PNGImageContent(in);
        in.close();

        PortObject[] outPorts = new PortObject[1];
        outPorts[0] = new ImagePortObject(content, IM_PORT_SPEC);

        return outPorts;
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
            return DEFAULT_PYTHON_PLOTCMD;
        } else {
            return TemplateConfigurator.generateScript(getHardwiredTemplate());
        }
    }

    public Image getImage() {
        return image;
    }
}
