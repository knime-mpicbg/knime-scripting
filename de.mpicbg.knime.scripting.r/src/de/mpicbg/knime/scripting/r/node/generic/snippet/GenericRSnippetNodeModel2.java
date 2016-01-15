package de.mpicbg.knime.scripting.r.node.generic.snippet;

import org.knime.core.node.ExecutionContext;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.port.PortObject;
import org.knime.core.node.port.PortObjectSpec;

import de.mpicbg.knime.scripting.core.ScriptingModelConfig;
import de.mpicbg.knime.scripting.r.AbstractRScriptingNodeModel;
import de.mpicbg.knime.scripting.r.RColumnSupport;
import de.mpicbg.knime.scripting.r.port.RPortObject;
import de.mpicbg.knime.scripting.r.port.RPortObjectSpec;


/**
 * Model: A generic R node which modifies an R workspace
 *
 * @author Holger Brandl, Antje Janosch
 */
public class GenericRSnippetNodeModel2 extends AbstractRScriptingNodeModel {
	
	/** default R script */
	public static final String GENERIC_SNIPPET_DFT = 
			"# put your R code here\n"
			+ "# availableObjects <- ls()";
	
	public static final ScriptingModelConfig nodeModelConfig = new ScriptingModelConfig(
			createPorts(1, RPortObject.TYPE, RPortObject.class), 	// 1 generic input
			createPorts(1, RPortObject.TYPE, RPortObject.class)	, 	// 1 generic output
			new RColumnSupport(), 		
			true, 	// script
			true, 	// open in R functionality
			false);	// no chunks
			
	/**
	 * constructor for 1 RPort input and 1 RPort output
	 */
    public GenericRSnippetNodeModel2(ScriptingModelConfig cfg) {
        super(cfg);
    }

    /**
     * constructor
     * @param inPortTypes
     * @param outPortTypes
     * @param useChunkSettings 
     * @param useOpenIn 
     */
    public GenericRSnippetNodeModel2() {
        this(nodeModelConfig);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    // note: This is not the usual configure but a more generic one with PortObjectSpec instead of DataTableSpec
    protected PortObjectSpec[] configure(final PortObjectSpec[] inSpecs) throws InvalidSettingsException {
    	super.configure(inSpecs);
        return hasOutput() ? new PortObjectSpec[]{RPortObjectSpec.INSTANCE} : new PortObjectSpec[0];
    }

    
    public String getDefaultScript() {
    	return GENERIC_SNIPPET_DFT;
    }
    
    /**
     * {@inheritDoc}
     */
	@Override
	protected PortObject[] executeImpl(PortObject[] inData,
			ExecutionContext exec) throws Exception {
		
		super.executeImpl(inData, exec);
		super.runScript(exec);
		PortObject[] outData = super.pullOutputFromR(exec);
   
        return outData;
    }
    
}