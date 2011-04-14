package de.mpicbg.tds.knime.knutils;

import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.def.DoubleCell;
import org.knime.core.data.def.IntCell;
import org.knime.core.node.util.ColumnFilter;


/**
 * Document me!
 *
 * @author Holger Brandl
 */
public class NumericFilter implements ColumnFilter {

    public boolean includeColumn(DataColumnSpec dataColumnSpec) {
        return dataColumnSpec.getType().equals(DoubleCell.TYPE) || dataColumnSpec.getType().equals(IntCell.TYPE);
    }


    public String allFilteredMsg() {
        return "No matching (numerical) attributes";
    }
}
