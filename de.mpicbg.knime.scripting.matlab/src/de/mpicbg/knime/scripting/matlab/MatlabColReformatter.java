package de.mpicbg.knime.scripting.matlab;

import de.mpicbg.knime.scripting.core.ColNameReformater;

import org.knime.core.data.DataType;


/**
 * Document me!
 *
 * @author Holger Brandl
 */
public class MatlabColReformatter implements ColNameReformater {

    public String reformat(String name, DataType type, boolean altDown) {
        if (altDown) return "kIn." + name.replaceAll("[^0-9a-zA-Z_]", "");
        else return name;
    }
}
