package de.mpicbg.tds.knime.scripting.groovy;

import de.mpicbg.tds.knime.knutils.scripting.ColNameReformater;
import org.knime.core.data.DataType;


/**
 * @author Holger Brandl
 */
public class GroovyColReformatter implements ColNameReformater {

    public String reformat(String name, DataType type, boolean altDown) {
        // todo replace the double with something type-specific
        return "new InputTableAttribute(\"" + name + "\", input)";
    }
}
