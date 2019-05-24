package de.mpicbg.knime.scripting.core.prefs;

public class JupyterKernelSpec {
	
	private final String m_name;
	private final String m_displayName;
	private final String m_language;
	
	public JupyterKernelSpec(String name, String displayName, String language) {
		m_name = name;
		m_displayName = displayName;
		m_language = language;
	}
	
	

}
