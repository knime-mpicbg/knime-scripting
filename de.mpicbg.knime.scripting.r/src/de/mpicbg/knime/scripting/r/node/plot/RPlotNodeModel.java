package de.mpicbg.knime.scripting.r.node.plot;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;

import javax.imageio.ImageIO;

import org.knime.core.data.DataTableSpec;
import org.knime.core.data.image.png.PNGImageContent;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.defaultnodesettings.SettingsModelIntegerBounded;
import org.knime.core.node.port.PortObject;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.core.node.port.PortType;
import org.knime.core.node.port.image.ImagePortObject;
import org.rosuda.REngine.REXPMismatchException;
import org.rosuda.REngine.REngineException;
import org.rosuda.REngine.Rserve.RConnection;
import org.rosuda.REngine.Rserve.RserveException;

import de.mpicbg.knime.scripting.core.AbstractScriptingNodeModel;
import de.mpicbg.knime.scripting.core.ScriptProvider;
import de.mpicbg.knime.scripting.core.exceptions.KnimeScriptingException;
import de.mpicbg.knime.scripting.r.R4KnimeBundleActivator;
import de.mpicbg.knime.scripting.r.RUtils;
import de.mpicbg.knime.scripting.r.plots.AbstractRPlotNodeModel;
import de.mpicbg.knime.scripting.r.prefs.RPreferenceInitializer;


/**
 * This is the model implementation of a plot panel that requires a data-table and a script as input.
 *
 * @author Holger Brandl (MPI-CBG)
 */
public class RPlotNodeModel extends AbstractRPlotNodeModel {

    public RPlotNodeModel() {
        super(createPorts(1), new PortType[]{ImagePortObject.TYPE});
    }


    public RPlotNodeModel(PortType[] inPorts, PortType[] outports) {
        super(inPorts, outports);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected PortObjectSpec[] configure(final PortObjectSpec[] inSpecs) throws InvalidSettingsException {
        super.configure(inSpecs);
        return new PortObjectSpec[]{IM_PORT_SPEC};
    }

    /**
     * {@inheritDoc}
     */
	/*@Override
	protected PortObject[] executeImpl(PortObject[] inData,
			ExecutionContext exec) throws Exception {
		logger.info("Render the R Plot");
		
		// create connection
        RConnection connection = RUtils.createConnection();
        
        // try to push data, close connection if anything fails and pass through the error
        try {
        	transferAndParse(castToBDT(inData), exec, connection);
        	
        	adaptHardwiredTemplateToContext(ScriptProvider.unwrapPortSpecs(inData));
        	createFigure(connection);
        	
        	ArrayList<String> warningMessages = RUtils.checkForWarnings(connection);
        	if(warningMessages.size() > 0) 
        		setWarningMessage("R-script produced " + warningMessages.size() + 
        				" warnings. See R-console view for further details");
        	
        } catch (Exception e) {
			if(connection.isConnected()) {
				connection.close();
				throw e;
			}
		}


        // close the connection to R
        connection.close();

        // Rerun the image
        PNGImageContent content;
        File m_imageFile = File.createTempFile("RImage", ".png");
        ImageIO.write(RPlotCanvas.toBufferedImage(image), "png", m_imageFile);
        FileInputStream in = new FileInputStream(m_imageFile);
        content = new PNGImageContent(in);
        in.close();


        PortObject[] outPorts = new PortObject[1];
        outPorts[0] = new ImagePortObject(content, IM_PORT_SPEC);

        return outPorts;
	}*/

 /*   private void transferAndParse(BufferedDataTable[] inData, ExecutionContext exec,
			RConnection connection) throws KnimeScriptingException, RserveException, REXPMismatchException {
        DataTableSpec inSpec = inData[0].getDataTableSpec();
    	
    	// retrieve chunk settings
        int chunkInSize = getChunkIn(((SettingsModelIntegerBounded) this.getModelSetting(CHUNK_IN)).getIntValue(), inData);
    	
    	// push color/size/shape model to R
		pushColorModelToR(inSpec, connection, exec);
		pushShapeModelToR(inSpec, connection, exec);
		pushSizeModelToR(inSpec, connection, exec);
		
		// push flow variables to R
		pushFlowVariablesToR(getAvailableInputFlowVariables(), connection, exec);

        // CONVERT input table into data-frame and put into the r-workspace
        //pushToR(inData, connection, exec, chunkInSize);
        
        exec.setMessage("Evaluate R-script (cannot be cancelled)");

        // PREPARE and parse script
        String script = prepareScript();
        // LEGACY: we still support the old R workspace variable names ('R' for input and 'R' also for output)
        // stop support !
        //rawScript = RUtils.supportOldVarNames(rawScript);   
        parseScript(connection, script);
	}*/


	/**
     * {@inheritDoc}
     */
	@Override
	protected void openIn(PortObject[] inData, ExecutionContext exec)
			throws KnimeScriptingException {
		/*try {
			String rawScript = prepareScript();
			RUtils.openInR(inData, exec, rawScript, logger);   
			setWarningMessage("To push the node's input to R again, you need to reset and re-execute it.");
		} catch (REXPMismatchException | IOException | REngineException e) {
			throw new KnimeScriptingException("Failed to open in R\n" + e);
		}	*/
	}
}