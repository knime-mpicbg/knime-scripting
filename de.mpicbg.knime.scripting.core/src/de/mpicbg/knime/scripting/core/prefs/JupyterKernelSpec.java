package de.mpicbg.knime.scripting.core.prefs;

import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class JupyterKernelSpec {
	
	private final String m_name;
	private final String m_displayName;
	private final String m_language;
	
	public static final String PYTHON_LANG = "python";
	public static final String R_LANG = "R";
	
	private static final List<String> m_availableLanguages = Arrays.asList(PYTHON_LANG, R_LANG);
	
	private static final Pattern PREF_PATTERN = Pattern.compile("^\\((.*),(.*),(.*)\\)$");
	
	
	public JupyterKernelSpec(String name, String displayName, String language) {
		m_name = name;
		m_displayName = displayName;
		m_language = language;
	}
	
	public String getName() {
		return m_name;
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

	public String toPrefString() {
		StringBuilder prefString = new StringBuilder();
		
		prefString.append("(");
        prefString.append(m_name);
        prefString.append(",");
        prefString.append(m_displayName);
        prefString.append(",");
        prefString.append(m_language);
        prefString.append(")");
        
        return prefString.toString();
	}

	public static JupyterKernelSpec fromPrefString(String prefString) {
		
		Matcher pMatch = PREF_PATTERN.matcher(prefString);
		
        if (pMatch.matches()) {
        	String name = pMatch.group(1);
        	String displayName = pMatch.group(2);
        	String language = pMatch.group(3);
        	
        	return new JupyterKernelSpec(name, displayName, language);
        }
		
		return null;
	}
	
	
}
