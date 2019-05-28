package de.mpicbg.knime.scripting.core.prefs;

import java.util.Arrays;
import java.util.List;

public class JupyterKernelSpec {
	
	private final String m_name;
	private final String m_displayName;
	private final String m_language;
	
	public static final String PYTHON_LANG = "python";
	public static final String R_LANG = "R";
	
	private static final List<String> m_availableLanguages = Arrays.asList(PYTHON_LANG, R_LANG);
	
	
	public JupyterKernelSpec(String name, String displayName, String language) {
		m_name = name;
		m_displayName = displayName;
		m_language = language;
	}
	
	public String getDisplayName() {
		return m_displayName;
	}

	public String getLanguage() {
		return m_language;
	}
	
	public static boolean isValidLanguage(String language) {
		return m_availableLanguages.contains(language);
	}

	@Override
	public String toString() {
		return m_name + ": (" + m_displayName + ", " + m_language.toString() + ")";
	}
	
}
