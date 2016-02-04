package de.mpicbg.knime.scripting.core;

import org.knime.core.data.DataType;


/**
 * Defines the way data items are rendered when the user selects them from the variable list while writing a template.
 * Defines which column types are supported
 *
 * @author Holger Brandl, Antje Janosch
 */
public abstract interface ColumnSupport {

	/** formatting for script editor */
    public abstract String reformat(String name, DataType type, boolean altDown);
    /** does the integration support this column type? */
    public abstract boolean isSupported(DataType dtype);
}
