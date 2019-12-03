package de.mpicbg.knime.scripting.core.exceptions;

public class KnimeScriptingException extends Exception {

	/** a generated serial id. */
	private static final long serialVersionUID = -872005723233752670L;

	public KnimeScriptingException(final String message) {
		super(message);
	}
	
	public KnimeScriptingException(final String preMessage, final String message) {
		super(preMessage + message);
	}
}
