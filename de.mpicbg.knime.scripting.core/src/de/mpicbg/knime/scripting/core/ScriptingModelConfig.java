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

	public PortType[] getM_inPorts() {
		return m_inPorts;
	}

	public void setM_inPorts(PortType[] m_inPorts) {
		this.m_inPorts = m_inPorts;
	}

	public PortType[] getM_outPorts() {
		return m_outPorts;
	}

	public void setM_outPorts(PortType[] m_outPorts) {
		this.m_outPorts = m_outPorts;
	}

	public ColumnSupport getM_colSupport() {
		return m_colSupport;
	}

	public void setM_colSupport(ColumnSupport m_colSupport) {
		this.m_colSupport = m_colSupport;
	}

	public boolean isM_useScriptSettings() {
		return m_useScriptSettings;
	}

	public void setM_useScriptSettings(boolean m_useScriptSettings) {
		this.m_useScriptSettings = m_useScriptSettings;
	}

	public boolean isM_useOpenIn() {
		return m_useOpenIn;
	}

	public void setM_useOpenIn(boolean m_useOpenIn) {
		this.m_useOpenIn = m_useOpenIn;
	}

	public boolean isM_useChunkSettings() {
		return m_useChunkSettings;
	}

	public void setM_useChunkSettings(boolean m_useChunkSettings) {
		this.m_useChunkSettings = m_useChunkSettings;
	}
	
	
}
