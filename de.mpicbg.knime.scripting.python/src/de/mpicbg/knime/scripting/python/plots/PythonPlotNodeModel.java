package de.mpicbg.knime.scripting.python.plots;

import java.awt.Image;
import java.awt.image.BufferedImage;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;

import javax.imageio.ImageIO;
import javax.swing.ImageIcon;

import org.eclipse.jface.preference.IPreferenceStore;
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

import de.mpicbg.knime.scripting.core.exceptions.KnimeScriptingException;
import de.mpicbg.knime.scripting.python.AbstractPythonScriptingNodeModel;
import de.mpicbg.knime.scripting.python.PythonScriptingBundleActivator;
import de.mpicbg.knime.scripting.python.PythonTableConverter;
import de.mpicbg.knime.scripting.python.prefs.PythonPreferenceInitializer;
import de.mpicbg.knime.scripting.python.srv.CommandOutput;
import de.mpicbg.knime.scripting.python.srv.LocalPythonClient;
import de.mpicbg.knime.scripting.python.srv.PythonClient;
import de.mpicbg.knime.scripting.python.srv.PythonTempFile;


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

    private static final String FIGURE_WIDTH_SETTING_NAME = "figure.width";
	private static final String FIGURE_HEIGHT_SETTING_NAME = "figure.height";
	private static final String OUTPUT_FILE_SETTING_NAME = "figure.output.file";
	private static final String OVERWRITE_SETTING_NAME = "overwrite.ok";

    private final String DEFAULT_PYTHON_PLOTCMD = "" +     
		"# the following import is not required, as the node take care of it \n" + 
		"#import matplotlib.pyplot as plt\n" + 
		"\n" + 
		"X = range(10)\n" + 
		"plt.plot(X, [x*x for x in X])\n" + 
		"plt.show()";

    protected static final ImagePortObjectSpec IM_PORT_SPEC = new ImagePortObjectSpec(PNGImageContent.TYPE);

    /**
     * {@inheritDoc}
     */
    @Override
    protected PortObjectSpec[] configure(final PortObjectSpec[] inSpecs) throws InvalidSettingsException {
    	super.configure(inSpecs);
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

        addModelSetting(FIGURE_WIDTH_SETTING_NAME, propWidth);
        addModelSetting(FIGURE_HEIGHT_SETTING_NAME, propHeight);

        addModelSetting(OUTPUT_FILE_SETTING_NAME, propOutputFile);
        addModelSetting(OVERWRITE_SETTING_NAME, propOverwriteFile);
    }

    protected void prepareScript(Writer writer) throws IOException {
        writer.write("import matplotlib\nmatplotlib.use('Agg')\nfrom pylab import *\n");

        super.prepareScript(writer, true);

        float dpi = 100;
        float width = getDefWidth() / dpi;
        float height = getDefHeight() / dpi;

        writer.write("F = gcf()\n");
        writer.write("F.set_dpi(" + dpi + ")\n");
        writer.write("F.set_size_inches(" + width + "," + height + ")");
    }


    public int getDefHeight() {
        return propHeight.getIntValue();
    }

    public int getDefWidth() {
        return propWidth.getIntValue();
    }

    @Override
    public String getDefaultScript(String defaultScript) {
    	return super.getDefaultScript(DEFAULT_PYTHON_PLOTCMD);
    }

    public Image getImage() {
        return image;
    }
    

    /**
     * {@inheritDoc}
     */
	@Override
	protected PortObject[] executeImpl(PortObject[] inData,
			ExecutionContext exec) throws Exception {
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
        writer.write("\nsavefig(r'" + imageFile.getServerPath() + "')\n");
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
        String fileName = prepareOutputFileName(propOutputFile.getStringValue());
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

    /**
     * {@inheritDoc}
     */
	@Override
	protected void openIn(PortObject[] inData, ExecutionContext exec)
			throws KnimeScriptingException {
		openInPython(inData, exec, logger);   
		setWarningMessage("To push the node's input to R again, you need to reset and re-execute it.");
	}
}
