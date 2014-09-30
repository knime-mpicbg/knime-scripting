package de.mpicbg.knime.scripting.python;

import de.mpicbg.knime.scripting.core.ColNameReformater;

import org.knime.core.data.DataType;


/**
 * Document me!
 *
 * @author Holger Brandl
 */
public class PythonColReformatter implements ColNameReformater {
    public String reformat(String name, DataType type, boolean altDown) {
        return altDown ? "kIn['" + name + "']" : name;
    }
}