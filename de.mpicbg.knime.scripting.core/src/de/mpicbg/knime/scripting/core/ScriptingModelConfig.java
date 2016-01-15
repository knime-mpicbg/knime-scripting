package de.mpicbg.knime.scripting.core;

import org.knime.core.node.port.PortType;

public class ScriptingModelConfig {
	
	private PortType[] m_inPorts;
	private PortType[] m_outPorts;
	private ColumnSupport m_colSupport = null;
	boolean m_useScriptSettings = true;
	boolean m_useOpenIn = true;
	boolean m_useChunkSettings = true;
	
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

	public ScriptingModelConfig() {
		// TODO Auto-generated constructor stub
	}

	public PortType[] getInPorts() {
		return m_inPorts;
	}

	public void setInPorts(PortType[] inPorts) {
		this.m_inPorts = inPorts;
	}

	public PortType[] getOutPorts() {
		return m_outPorts;
	}

	public void setOutPorts(PortType[] outPorts) {
		this.m_outPorts = outPorts;
	}

	public ColumnSupport getColSupport() {
		return m_colSupport;
	}

	public void setColSupport(ColumnSupport colSupport) {
		this.m_colSupport = colSupport;
	}

	public boolean useScriptSettings() {
		return m_useScriptSettings;
	}

	public void setUseScriptSettings(boolean useScriptSettings) {
		this.m_useScriptSettings = useScriptSettings;
	}

	public boolean useOpenIn() {
		return m_useOpenIn;
	}

	public void setUseOpenIn(boolean useOpenIn) {
		this.m_useOpenIn = useOpenIn;
	}

	public boolean useChunkSettings() {
		return m_useChunkSettings;
	}

	public void setUseChunkSettings(boolean useChunkSettings) {
		this.m_useChunkSettings = useChunkSettings;
	}
	
	
}
