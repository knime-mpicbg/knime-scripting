package de.mpicbg.tds.knime.knutils;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.node.BufferedDataTable;


/**
 * Document me!
 *
 * @author Holger Brandl
 */
public class InputTableAttribute<AttributeType> extends Attribute<AttributeType> {

    private DataColumnSpec columnSpec;
    private Integer columnIndex;


    public InputTableAttribute(String attributeName, BufferedDataTable input) {
        this(attributeName, input.getSpec());
    }


    public InputTableAttribute(String attributeName, DataTableSpec dataTableSpec) {
        this(findColumnByName(attributeName, dataTableSpec), dataTableSpec.findColumnIndex(attributeName));
    }


    public InputTableAttribute(DataColumnSpec columnSpec, Integer columnIndex) {
        super(columnSpec.getName(), columnSpec.getType());

        if (columnSpec == null) {
            throw new RuntimeException("column specs are null when creating attribute instance in column no" + columnIndex);
        }

        this.columnSpec = columnSpec;
        this.columnIndex = columnIndex;
    }


    public static DataColumnSpec findColumnByName(String attributeName, DataTableSpec dataTableSpec) {
        DataColumnSpec colSpec = dataTableSpec.getColumnSpec(attributeName);
        if (colSpec == null) {
            throw new IllegalArgumentException("Could not find attribute-column with name '" + attributeName + "'");
        }

        return colSpec;
    }


    public Integer getColumnIndex() {
        return columnIndex;
    }


    public DataColumnSpec getColumnSpec() {
        return columnSpec;
    }


    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof InputTableAttribute)) return false;

        InputTableAttribute attribute = (InputTableAttribute) o;

        if (!columnSpec.getName().equals(attribute.columnSpec.getName())) return false;

        return true;
    }


    @Override
    public int hashCode() {
        return columnSpec.getName().hashCode();
    }


    public DataCell createCell(DataRow dataRow) {
        return createCell(getValue(dataRow));
    }


}
