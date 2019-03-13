package de.mpicbg.knime.scripting.core;

import org.knime.core.node.port.PortType;

/**
 * configuration class which stores information about the architecture of a scripting node
 * 
 * @author Antje Janosch
 *
 */
public class ScriptingModelConfig {
	
	/** array of input port types */
	private PortType[] m_inPorts;
	/** array of output port types */
	private PortType[] m_outPorts;
	/** scripting language depending how to handle column access / types */
	private ColumnSupport m_colSupport = null;
	/** display tab with script / template stuff? */
	boolean m_useScriptSettings = true;
	/** allow 'open in' option? */
	boolean m_useOpenIn = true;
	/** display tab for chunk settings? */
	boolean m_useChunkSettings = true;
	
	/**
	 * constructor
	 * 
	 * @param m_inPorts			array of input port types
	 * @param m_outPorts		array of output port types
	 * @param m_colSupport		scripting language depending how to handle column access / types
	 * @param m_useScriptSettings	display tab with script / template stuff?
	 * @param m_useOpenIn		allow 'open in' option?
	 * @param m_useChunkSettings	display tab for chunk settings?
	 */
	public ScriptingModelConfig(PortType[] m_inPorts, PortType[] m_outPorts, ColumnSupport m_colSupport,
			boolean m_useScriptSettings, boolean m_useOpenIn, boolean m_useChunkSettings) {
		super();
		this.m_inPorts = m_inPorts;
		this.m_outPorts = m_outPorts;
		this.m_colSupport = m_colSupport;
		this.m_useScriptSettings = m_useScriptSettings;
		this.m_useOpenIn = m_useOpenIn;
		this.m_useChunkSettings = m_useChunkSettings;
	}

	/** 
	 * @return array of input port types
	 */
	public PortType[] getInPorts() {
		return m_inPorts;
	}

	/**
	 * @param inPorts	array of input port types
	 */
	public void setInPorts(PortType[] inPorts) {
		this.m_inPorts = inPorts;
	}

	/**
	 * @return array of output port types
	 */
	public PortType[] getOutPorts() {
		return m_outPorts;
	}

	/** 
	 * @param outPorts	array of outnput port types
	 */
	public void setOutPorts(PortType[] outPorts) {
		this.m_outPorts = outPorts;
	}

	/**
	 * 
	 * @return {@link ColumnSupport}
	 */
	public ColumnSupport getColSupport() {
		return m_colSupport;
	}

	/**
	 * set column support
	 * @param colSupport
	 */
	public void setColSupport(ColumnSupport colSupport) {
		this.m_colSupport = colSupport;
	}

	/**
	 * 
	 * @return true, if script editing/selection should be allowed
	 */
	public boolean useScriptSettings() {
		return m_useScriptSettings;
	}

	/**
	 * script editing/selection should be allowed ?
	 * @param useScriptSettings
	 */
	public void setUseScriptSettings(boolean useScriptSettings) {
		this.m_useScriptSettings = useScriptSettings;
	}

	/**
	 * 
	 * @return true, if 'open in' option shall be made available
	 */
	public boolean useOpenIn() {
		return m_useOpenIn;
	}

	/**
	 * 'open in' option shall be made available ?
	 * @param useOpenIn
	 */
	public void setUseOpenIn(boolean useOpenIn) {
		this.m_useOpenIn = useOpenIn;
	}

	/**
	 * 
	 * @return true, if chunk settings tab shall be available
	 */
	public boolean useChunkSettings() {
		return m_useChunkSettings;
	}

	/**
	 * chunk settings tab shall be available ?
	 * @param useChunkSettings
	 */
	public void setUseChunkSettings(boolean useChunkSettings) {
		this.m_useChunkSettings = useChunkSettings;
	}
	
	
}
