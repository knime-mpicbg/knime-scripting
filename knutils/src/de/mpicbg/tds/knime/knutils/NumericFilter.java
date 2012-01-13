package de.mpicbg.tds.knime.knutils;

import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataType;
import org.knime.core.data.DoubleValue;
import org.knime.core.data.IntValue;
import org.knime.core.node.util.ColumnFilter;


/**
 * Document me!
 *
 * @author Holger Brandl
 */
public class NumericFilter implements ColumnFilter {

    public boolean includeColumn(DataColumnSpec dataColumnSpec) {
        //return dataColumnSpec.getType().equals(DoubleCell.TYPE) || dataColumnSpec.getType().equals(IntCell.TYPE);
        DataType dataType = dataColumnSpec.getType();
        return dataType.isCompatible(DoubleValue.class) || dataType.isCompatible(IntValue.class);
    }


    public String allFilteredMsg() {
        return "No matching (numerical) attributes";
    }
}
