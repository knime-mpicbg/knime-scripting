package de.mpicbg.knime.scripting.core.exceptions;

/**
 * Exception should be used in all Scripting Extensions to communicate issues to the user
 * 
 * @author Antje Janosch
 *
 */
public class KnimeScriptingException extends Exception {

	/** a generated serial id. */
	private static final long serialVersionUID = -872005723233752670L;

	/**
	 * create exception with given message
	 * @param message
	 */
	public KnimeScriptingException(final String message) {
		super(message);
	}
	
	/**
	 * create exception with combined message (might be constant + concrete part)
	 * @param preMessage
	 * @param message
	 */
	public KnimeScriptingException(final String preMessage, final String message) {
		super(preMessage + message);
	}
}
