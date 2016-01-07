package de.mpicbg.knime.scripting.r;

import de.mpicbg.knime.scripting.r.node.plot.RPlotCanvas;
import de.mpicbg.knime.scripting.r.node.plot.RPlotNodeModel;

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

import javax.imageio.ImageIO;
import java.io.File;
import java.io.FileInputStream;


/**
 * Document me!
 *
 * @author Holger Brandl
 */
public class RPlotWithImPortNodeModel extends RPlotNodeModel {

    protected static final ImagePortObjectSpec IM_PORT_SPEC = new ImagePortObjectSpec(PNGImageContent.TYPE);


    public RPlotWithImPortNodeModel() {
        super(createPorts(1), new PortType[]{ImagePortObject.TYPE});
    }


    /**
     * {@inheritDoc}
     */
    @Override
    protected PortObjectSpec[] configure(final PortObjectSpec[] inSpecs)
            throws InvalidSettingsException {

        configure(new DataTableSpec[]{(DataTableSpec) inSpecs[0]});

        return new PortObjectSpec[]{IM_PORT_SPEC};
    }


    @Override
    protected PortObject[] execute(PortObject[] inData, ExecutionContext exec) throws Exception {
        BufferedDataTable[] result = (BufferedDataTable[]) super.executeImpl(new PortObject[]{ inData[0]}, exec);

        // create the image port object
        PNGImageContent content;
        File m_imageFile = File.createTempFile("rportImage", ".png");
        ImageIO.write(RPlotCanvas.toBufferedImage(image), "png", m_imageFile);
        FileInputStream in = new FileInputStream(m_imageFile);
        content = new PNGImageContent(in);
        in.close();

        PortObject[] outPorts = new PortObject[1 + result.length];
        for (int i = 0; i < result.length; i++) {
            outPorts[i] = result[1];
        }

        outPorts[outPorts.length - 1] = new ImagePortObject(content, IM_PORT_SPEC);

        return outPorts;
    }
}
