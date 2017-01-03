package de.mpicbg.knime.scripting.core;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Platform;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataTableSpec;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.defaultnodesettings.SettingsModelBoolean;
import org.knime.core.node.defaultnodesettings.SettingsModelIntegerBounded;
import org.knime.core.node.defaultnodesettings.SettingsModelString;
import org.knime.core.node.port.PortObject;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.core.node.port.PortType;
import org.osgi.framework.Bundle;
import org.osgi.framework.FrameworkUtil;

import de.mpicbg.knime.knutils.AbstractNodeModel;
import de.mpicbg.knime.scripting.core.exceptions.KnimeScriptingException;
import de.mpicbg.knime.scripting.core.rgg.RGGDialogPanel;
import de.mpicbg.knime.scripting.core.rgg.TemplateUtils;
import de.mpicbg.knime.scripting.core.rgg.wizard.ScriptTemplate;


/**
 * Document me!
 *
 * @author Holger Brandl, Antje Janosch
 */
public abstract class AbstractScriptingNodeModel extends AbstractNodeModel {

    /**
     * The property for the string.
     */
    public static final String SCRIPT_PROPERTY = "node.script";
    public static final String SCRIPT_TEMPLATE = "node.template";

    public static final String SCRIPT_TEMPLATE_DEFAULT = "";

    /**
     * node setting: functionality to open the input data externally
     */
	public static final String OPEN_IN = "open.in";
	public static final boolean OPEN_IN_DFT = false;
	
	/**
	 * node setting: chunk size to push input table, columns per chunk (-1 => not split into chunks)
	 */
	public static final String CHUNK_IN = "chunk.in";
	public static final int CHUNK_IN_DFT = -1;
	
	/**
	 * node setting: chunk size to pull result table (-1 => not split into chunks)
	 */
	public static final String CHUNK_OUT = "chunk.out";
	public static final int CHUNK_OUT_DFT = -1;


    private ScriptTemplate hardwiredTemplate = null;
    private String contextAwareHWTemplateText;
    
    protected ColumnSupport m_colSupport = null;
    
    private ScriptingModelConfig m_nodeCfg = null;

    public AbstractScriptingNodeModel(PortType[] inPorts, PortType[] outPorts, ColumnSupport colSupport) {
    	this(inPorts, outPorts, colSupport, true, true, true);
    }
    
    /**
     * constructor able to add settings to node model by flags
     * @param inPorts
     * @param outPorts
     * @param useScriptSettings
     * @param useOpenIn
     * @param useChunkSettings
     */
    public AbstractScriptingNodeModel(PortType[] inPorts, PortType[] outPorts, ColumnSupport colSupport,
    		boolean useScriptSettings,
    		boolean useOpenIn,
    		boolean useChunkSettings) {
    	super(inPorts, outPorts, true);
    	
    	this.m_colSupport = colSupport;
    	
        if(useScriptSettings) {
        	this.addModelSetting(SCRIPT_PROPERTY, createSnippetProperty(getDefaultScript("")));
        	this.addModelSetting(SCRIPT_TEMPLATE, createTemplateProperty());
        }
        if(useOpenIn) {
        	this.addModelSetting(OPEN_IN, createOpenInProperty());
        }
        if(useChunkSettings) {
        	this.addModelSetting(CHUNK_IN, createChunkInProperty());
        	this.addModelSetting(CHUNK_OUT, createChunkOutProperty());
        }
        
        if(this.m_nodeCfg == null)
        	this.m_nodeCfg = new ScriptingModelConfig(inPorts, outPorts, colSupport, useScriptSettings, useOpenIn, useChunkSettings);
    }

    /**
     * constructor with node configuration object
     * @param cfg
     */
	public AbstractScriptingNodeModel(ScriptingModelConfig cfg) {
		this(
				cfg.getInPorts(), 
				cfg.getOutPorts(), 
				cfg.getColSupport(), 
				cfg.useScriptSettings(),
				cfg.useOpenIn(),
				cfg.useChunkSettings());
		this.m_nodeCfg = cfg;
	}
	
	/**
	 * @return node configuration
	 */
	protected ScriptingModelConfig getNodeCfg() {
		return this.m_nodeCfg;
	}


	public void setHardwiredTemplate(ScriptTemplate hardwiredTemplate) {
        // note we clone it here as it might be (and will be in most cases) an instance variable in the node factory.
        this.hardwiredTemplate = (ScriptTemplate) hardwiredTemplate.clone();
    }


    public ScriptTemplate getHardwiredTemplate() {
        return hardwiredTemplate;
    }


    public static SettingsModelString createSnippetProperty(String defaultScript) {
        return new SettingsModelString(SCRIPT_PROPERTY, defaultScript);
    }


    public static SettingsModelString createTemplateProperty() {
        return new SettingsModelString(SCRIPT_TEMPLATE, SCRIPT_TEMPLATE_DEFAULT);
    }
    
    public static SettingsModelBoolean createOpenInProperty() {
		return new SettingsModelBoolean(OPEN_IN, OPEN_IN_DFT);
	}
    
    public static SettingsModelIntegerBounded createChunkInProperty() {
		return new SettingsModelIntegerBounded(CHUNK_IN, CHUNK_IN_DFT, -1, Integer.MAX_VALUE);
	}
    
    public static SettingsModelIntegerBounded createChunkOutProperty() {
		return new SettingsModelIntegerBounded(CHUNK_OUT, CHUNK_OUT_DFT, -1, Integer.MAX_VALUE);
	}

    @Override
	protected PortObject[] execute(PortObject[] inObjects, ExecutionContext exec)
			throws Exception {
    	// check whether the data should be opened externally
    	SettingsModelBoolean openInSM = ((SettingsModelBoolean) this.getModelSetting(OPEN_IN));
    	if(openInSM != null) {
	    	if(openInSM.getBooleanValue()) {
	    		openIn(inObjects, exec);
	    		throw new KnimeScriptingException("Data has been opened externally. Uncheck that option to run the script within KNIME");
	    	} 
    	}
    	return executeImpl(inObjects,exec);
	}
    
    /**
     * method to run the code while node execution
     * @param inData
     * @param exec
     * @return
     * @throws Exception
     */
    protected abstract PortObject[] executeImpl(PortObject[] inData, ExecutionContext exec) throws Exception;
	
    /**
     * open data externally (in R, Python, ...)
     * @param inData
     * @param exec
     * @throws KnimeScriptingException
     * @throws CanceledExecutionException 
     */
	protected abstract void openIn(PortObject[] inData, ExecutionContext exec) 
			throws KnimeScriptingException, CanceledExecutionException;


    @Override
	protected PortObjectSpec[] configure(PortObjectSpec[] inSpecs)
			throws InvalidSettingsException {
    	// adapt hardwired templates to the input specs. Important: this just applies to nodes with outputs.
        // Plot-nodes need to be handled separately
        adaptHardwiredTemplateToContext(inSpecs);
        
        // add templates to template-cache-singleton
        Bundle bundle = FrameworkUtil.getBundle(getClass());
        IPath path = Platform.getStateLocation(bundle);
        String prefString = getTemplatePrefs();
        TemplateCache cache = TemplateCache.getInstance();
        try {
			cache.addTemplatesFromPref(prefString, path);
		} catch (IOException e) {
			e.printStackTrace();
		}
        
        SettingsModelBoolean openInSetting = ((SettingsModelBoolean) this.getModelSetting(OPEN_IN));
        if(openInSetting != null)
        	if(openInSetting.getBooleanValue())
        		this.setWarningMessage("The node is configured to open the input data externally\n.Execution will fail after that");

        return null;
	}

    /**
     * method should take care to cache templates files locally if available
     * @param path 
     */
    protected abstract String getTemplatePrefs();

	/**
     * @return default script
     */
	public String getDefaultScript(String defaultScript) {
		if (getHardwiredTemplate() != null) {
			return TemplateConfigurator.generateScript(getHardwiredTemplate());
		}
		return defaultScript;
    }
	

    @Override
    protected void validateSettings(final NodeSettingsRO settings)
            throws InvalidSettingsException {
        super.validateSettings(settings);
    }

    /**
     * does the node has output ports?
     * @return
     */
    protected boolean hasOutput() {  	
        return getNrOutPorts() > 0;
    }

    /**
     * if the node has an hardwired template, it's script needs to be adapted (RGG placeholders)
     * @param inData
     */
    protected void adaptHardwiredTemplateToContext(PortObjectSpec[] inData) {
        if (hardwiredTemplate != null && hardwiredTemplate.isLinkedToScript()) {
            Map<Integer, List<DataColumnSpec>> nodeInputModel = ScriptProvider.getPushableInputSpec(inData, m_colSupport);
            contextAwareHWTemplateText = TemplateUtils.replaceRGGPlaceholders(hardwiredTemplate.getTemplate(), nodeInputModel);
        }
    }


    protected void adaptHardwiredTemplateToContext(DataTableSpec[] inData) {
        if (hardwiredTemplate != null && hardwiredTemplate.isLinkedToScript()) {
            Map<Integer, List<DataColumnSpec>> nodeInputModel = ScriptProvider.getPushableInputSpec(inData, m_colSupport);
            contextAwareHWTemplateText = TemplateUtils.replaceRGGPlaceholders(hardwiredTemplate.getTemplate(), nodeInputModel);
        }
    }


    public Map<String, Object> getTemplateConfig(PortObjectSpec[] inSpecs) {
        adaptHardwiredTemplateToContext(inSpecs);

        return getTemplateConfigInternal();
    }


    public Map<String, Object> getTemplateConfig(DataTableSpec[] inSpecs) {
        adaptHardwiredTemplateToContext(inSpecs);

        return getTemplateConfigInternal();
    }


    private Map<String, Object> getTemplateConfigInternal() {
    	String template = ((SettingsModelString) getModelSetting(SCRIPT_TEMPLATE)).getStringValue();
        ScriptTemplate deserializedTemplate = ScriptingNodeDialog.deserializeTemplate(template);

        if (deserializedTemplate == null) {

            if (hardwiredTemplate.getTemplate() != null) {

                RGGDialogPanel panel = TemplateConfigurator.createRGGModel(contextAwareHWTemplateText, null);

                HashMap<String, Object> persistedUiState = new HashMap<String, Object>();
                panel.rgg.getRggModel().persistState(persistedUiState);
                return persistedUiState;
            }

            throw new RuntimeException("no template confugration found for current template.");
        }

        return deserializedTemplate.getPersistedConfig();
    }


    /**
     * <p>
     * deserializes the template from the node setting 'node.template'<br>
     * loads the script (either with RGG configuration settings or as it is)<br>
     * replace flowvariable placeholders
     * </p>
     * 
     * This method is usually just called from within the different execute implementations. Occassionally it is also
     * called in the view implemntations.
     */
    public String prepareScript() {

    	if(!m_nodeCfg.m_useScriptSettings) return "";

        String script;
        String serializedTemplate = ((SettingsModelString) getModelSetting(SCRIPT_TEMPLATE)).getStringValue();

        ScriptTemplate restoredTemplate = ScriptingNodeDialog.deserializeTemplate(serializedTemplate);


        // if the node is a hard-wired one, use the default template, otherwise use the script as saved in the template definition
        if (contextAwareHWTemplateText == null || hardwiredTemplate == null || (restoredTemplate != null && !restoredTemplate.isLinkedToScript())) {
            script = ((SettingsModelString) getModelSetting(SCRIPT_PROPERTY)).getStringValue();

        } else {
            ScriptTemplate contextAwareHWTemplate = new ScriptTemplate();
            contextAwareHWTemplate.setName("context-aware hardwired tempalte");
            contextAwareHWTemplate.setTemplate(contextAwareHWTemplateText);

            if (restoredTemplate != null) {
                Map<String, Object> uiConfig = restoredTemplate.getPersistedConfig();

                if (uiConfig != null) {
                    contextAwareHWTemplate.setPersistedConfig(uiConfig);
                }
            }

            script = TemplateConfigurator.generateScript(contextAwareHWTemplate);
        }
        
        script = fixEncoding(script);

        // replace flow-variables
        return FlowVarUtils.replaceFlowVars(script, this);
    }
    

    /**
     * cast an array of PortObjects into an array of BufferedDataTables
     * @param inData
     * @return array of BufferedDataTables
     */
	public static BufferedDataTable[] castToBDT(PortObject[] inData) {		
		
		List<BufferedDataTable> inTables = new ArrayList<BufferedDataTable>();
		
		for(PortObject in : inData) {
			if(in instanceof BufferedDataTable)
				inTables.add((BufferedDataTable) in);
		}	
		
		return inTables.toArray(new BufferedDataTable[inTables.size()]);
	}
	
    /**
     * cast a PortObjects into a BufferedDataTables
     * @param inData
     * @return BufferedDataTable or null
     */
	public static BufferedDataTable castToBDT(PortObject inData) {
		if(inData instanceof BufferedDataTable) return (BufferedDataTable) inData;
		return null;
	}

	/**
	 * ensure UTF-8 encoding
	 * @param stringValue
	 * @return
	 */
    public static String fixEncoding(String stringValue) {
        String encodedString = new String(stringValue.getBytes(StandardCharsets.UTF_8));
        return encodedString.replace("\r","");
    }

	/** load hardwired RGG template */
	public static ScriptTemplate loadTemplate(ScriptFileProvider scriptFileProvider) {
        String templateFileName = scriptFileProvider.getTemplateFileName();
        InputStream scriptStream = scriptFileProvider.getClass().getResourceAsStream(templateFileName);
        String unparsedTemplate = TemplateUtils.convertStreamToString(scriptStream);

        ScriptTemplate scriptTemplate = new ScriptTemplate();
        scriptTemplate.setTemplate(unparsedTemplate);
        return scriptTemplate;
    }
}
