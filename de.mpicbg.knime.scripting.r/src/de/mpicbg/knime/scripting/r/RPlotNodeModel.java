package de.mpicbg.knime.scripting.r;

import de.mpicbg.knime.scripting.core.ScriptProvider;
import de.mpicbg.knime.scripting.r.plots.AbstractRPlotNodeModel;
import de.mpicbg.knime.scripting.r.plots.RPlotCanvas;

import org.knime.core.data.DataTableSpec;
import org.knime.core.data.image.png.PNGImageContent;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.port.PortObject;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.core.node.port.PortType;
import org.knime.core.node.port.image.ImagePortObject;
import org.knime.core.node.port.image.ImagePortObjectSpec;
import org.rosuda.REngine.Rserve.RConnection;

import javax.imageio.ImageIO;
import java.io.File;
import java.io.FileInputStream;


/**
 * This is the model implementation of a plot panel that requires a data-table and a script as input.
 *
 * @author Holger Brandl (MPI-CBG)
 */
public class RPlotNodeModel extends AbstractRPlotNodeModel {

    protected static final ImagePortObjectSpec IM_PORT_SPEC = new ImagePortObjectSpec(PNGImageContent.TYPE);


    public RPlotNodeModel() {
        super(createPorts(1), new PortType[]{ImagePortObject.TYPE});
    }


    public RPlotNodeModel(PortType[] inPorts, PortType[] outports) {
        super(inPorts, outports);
    }


    @Override
    protected PortObjectSpec[] configure(final PortObjectSpec[] inSpecs) throws InvalidSettingsException {
        configure(new DataTableSpec[]{(DataTableSpec) inSpecs[0]});
        return new PortObjectSpec[]{IM_PORT_SPEC};
    }


    @Override
    protected PortObject[] execute(PortObject[] inData,
                                   final ExecutionContext exec) throws Exception {

        logger.info("Render the R Plot");

        RConnection connection = RUtils.createConnection();

        // 1) convert exampleSet ihnto data-frame and put into the r-workspace
        RUtils.pushToR(inData, connection, exec);

        adaptHardwiredTemplateToContext(ScriptProvider.unwrapPortSpecs(inData));
        createFigure(connection);

//        BufferedDataTable[] result = prepareOutput(exec, connection);

        // close the connection to R
        connection.close();


        // Retrun the image
        PNGImageContent content;
        File m_imageFile = File.createTempFile("RImage", ".png");
        ImageIO.write(RPlotCanvas.toBufferedImage(image), "png", m_imageFile);
        FileInputStream in = new FileInputStream(m_imageFile);
        content = new PNGImageContent(in);
        in.close();


        PortObject[] outPorts = new PortObject[1];
        outPorts[0] = new ImagePortObject(content, IM_PORT_SPEC);

        return outPorts;
    }


    /**
     * Prepares the ouput tables of this nodes. As most plot-nodes won't have any data output, this method won't be
     * overridden in most cases. Just in case a node should have both (an review and a data table output), you may
     * override it.
     */
    protected BufferedDataTable[] prepareOutput(ExecutionContext exec, RConnection connection) {
        return new BufferedDataTable[0];
    }
}