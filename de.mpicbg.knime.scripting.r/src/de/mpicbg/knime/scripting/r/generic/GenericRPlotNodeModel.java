package de.mpicbg.knime.scripting.r.generic;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.nio.file.Files;
import java.util.Collections;

import javax.swing.ImageIcon;

import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.port.PortObject;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.core.node.port.PortType;
import org.rosuda.REngine.Rserve.RConnection;

import de.mpicbg.knime.scripting.core.ScriptProvider;
import de.mpicbg.knime.scripting.core.ScriptingModelConfig;
import de.mpicbg.knime.scripting.core.exceptions.KnimeScriptingException;
import de.mpicbg.knime.scripting.r.RColumnSupport;
import de.mpicbg.knime.scripting.r.RUtils;
import de.mpicbg.knime.scripting.r.node.snippet.RSnippetNodeModel;
import de.mpicbg.knime.scripting.r.plots.AbstractRPlotNodeModel;


/**
 * A generic R node which creates a figure from a genericR input.
 *
 * @author Holger Brandl
 */
public class GenericRPlotNodeModel extends AbstractRPlotNodeModel {
	
	private static ScriptingModelConfig nodeModelConfig = new ScriptingModelConfig(
			createPorts(1, RPortObject.TYPE, RPortObject.class), 	// 1 generic input
			new PortType[0], 		// no output
			new RColumnSupport(), 	
			true, 					// no script
			false, 					// open in functionality
			false);					// use chunk settings


    public GenericRPlotNodeModel() {
        super(nodeModelConfig);
    }


    /**
     * This constructor is just necessary to allow superclasses to create plot nodes with ouputs
     */
    public GenericRPlotNodeModel(ScriptingModelConfig cfg) {
        super(cfg);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected PortObjectSpec[] configure(final PortObjectSpec[] inSpecs) throws InvalidSettingsException {
    	super.configure(inSpecs);
    	return new PortObjectSpec[0];
    }


    /**
     * Prepares the ouput tables of this nodes. As most plot-nodes won't have any data output, this method won't be
     * overridden in most cases. Just in case a node should have both (an review and a data table output), you may
     * override it.
     * @throws KnimeScriptingException 
     */
    protected PortObject[] prepareOutput(ExecutionContext exec, RConnection connection) 
    		throws KnimeScriptingException {
    	/*PortObject[] outPorts = new PortObject[1];
    	ByteArrayOutputStream baos = new ByteArrayOutputStream();
		try {
			ImageIO.write(m_image, "png", baos);
		} catch (IOException e) {
			throw new KnimeScriptingException(e.getMessage());
		}
		PNGImageContent content = new PNGImageContent(baos.toByteArray());
        
        outPorts[0] = new ImagePortObject(content, IM_PORT_SPEC);*/
        return new PortObject[0];
    }

    /**
     * {@inheritDoc}
     */
	@Override
	protected PortObject[] executeImpl(PortObject[] inData,
			ExecutionContext exec) throws Exception {
		m_con = RUtils.createConnection();
        PortObject[] nodeOutput = null;
        try {
	        // 1) restore the workspace in a different server session
        	RUtils.loadGenericInputs(Collections.singletonMap(RSnippetNodeModel.R_INVAR_BASE_NAME, ((RPortObject)inData[0]).getFile()), m_con);
	
	        // 2) create the figure
	        adaptHardwiredTemplateToContext(ScriptProvider.unwrapPortSpecs(inData));
	        
			try {
				createInternals(m_con);
				saveFigureAsFile();
			} catch (KnimeScriptingException e) {
				closeRConnection();
				throw e;
			}

	        // 3) prepare the output tables (which will do nothing in most cases, as most plot nodes don't have output)
	        nodeOutput = prepareOutput(exec, m_con);
	
	        // 3) close the connection to R (which will also delete the temporary workspace on the server)
	        closeRConnection();
        } catch(Exception e) {
        	closeRConnection();
        	throw e;
        }

        return nodeOutput;
	}

	/**
	 * overwrite this method to support old internal image format (ImageIcon)
	 */
    @Override
	protected void saveInternals(File nodeDir, ExecutionMonitor executionMonitor)
			throws IOException, CanceledExecutionException {
    	if (m_rWorkspaceFile != null) {
            File f = new File(nodeDir, "pushtable.R");

            Files.copy(m_rWorkspaceFile.toPath(), f.toPath());
        }

        if (m_image != null) {
        	File imageFile = new File(nodeDir, "image.bin");

            FileOutputStream f_out = new FileOutputStream(imageFile);

            // Write object with ObjectOutputStream
            ObjectOutputStream obj_out = new ObjectOutputStream(new BufferedOutputStream(f_out));

            // Write object out to disk

            obj_out.writeObject(new ImageIcon(m_image));
            obj_out.close();
        }
	}

	/**
     * {@inheritDoc}
     */
	@Override
	protected void openIn(PortObject[] inData, ExecutionContext exec)
			throws KnimeScriptingException {
	}
}
