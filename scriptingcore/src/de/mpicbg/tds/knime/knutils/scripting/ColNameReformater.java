package de.mpicbg.tds.knime.knutils.scripting;

import org.knime.core.data.DataType;


/**
 * Defines the way dataitems are rendered when the user selects them from the variable list while writing a template.
 *
 * @author Holger Brandl
 */
public interface ColNameReformater {

    public String reformat(String name, DataType type, boolean altDown);
}
