package de.mpicbg.knime.scripting.core.prefs;

import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * class holds the specification of a jupyter kernel
 * 
 * @author Antje
 *
 */
public class JupyterKernelSpec {
	
	/** name of the kernel */
	private final String m_name;
	/** diplay name of the kernel */
	private final String m_displayName;
	/** language of the kernel */
	private final String m_language;
	
	/** kernel languages currently supported */
	public static final String PYTHON_LANG = "python";
	public static final String R_LANG = "R";
	
	private static final List<String> m_availableLanguages = Arrays.asList(PYTHON_LANG, R_LANG);
	
	/** pattern to parse the preferences for jupyter kernel specs */
	private static final Pattern PREF_PATTERN = Pattern.compile("^\\((.*),(.*),(.*)\\)$");
	
	/**
	 * constructor 
	 * 
	 * @param name	of the kernel
	 * @param displayName	of the kernel	
	 * @param language	of the kernel
	 */
	public JupyterKernelSpec(String name, String displayName, String language) {
		m_name = name;
		m_displayName = displayName;
		m_language = language;
	}
	
	/**
	 * @return kernel name
	 */
	public String getName() {
		return m_name;
	}
	
	/**
	 * @return kernel display name
	 */
	public String getDisplayName() {
		return m_displayName;
	}

	/**
	 * @return kernel language
	 */
	public String getLanguage() {
		return m_language;
	}
	
	/**
	 * check whether language is supported
	 * @param language to check
	 * @return true, if supported
	 */
	public static boolean isValidLanguage(String language) {
		return m_availableLanguages.contains(language);
	}

	/**
	 * ({@inheritDoc}
	 */
	@Override
	public String toString() {
		return m_name + ": (" + m_displayName + ", " + m_language.toString() + ")";
	}

	/**
	 * @return kernel spec as string to be stored in prefs
	 */
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

	/**
	 * deserialize kernel spec from pref string
	 * @param prefString
	 * @return	instance of {@link JupyterKernelSpec}
	 */
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
