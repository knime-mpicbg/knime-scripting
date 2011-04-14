package de.mpicbg.tds.knime.knutils;

import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.def.StringCell;
import org.knime.core.node.util.ColumnFilter;


/**
 * Document me!
 *
 * @author Holger Brandl
 */
public class StringFilter implements ColumnFilter {

    public boolean includeColumn(DataColumnSpec dataColumnSpec) {
        return dataColumnSpec.getType().equals(StringCell.TYPE);
    }


    public String allFilteredMsg() {
        return null;
    }
}