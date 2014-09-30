package de.mpicbg.knime.knutils;

import org.knime.core.data.*;
import org.knime.core.data.container.CellFactory;
import org.knime.core.data.container.ColumnRearranger;
import org.knime.core.data.container.SingleCellFactory;

import java.util.*;


/**
 * Document me!
 *
 * @author Holger Brandl
 */
public class TableUpdateCache {

    private Map<Attribute, Map<RowKey, DataCell>> cache = new LinkedHashMap<Attribute, Map<RowKey, DataCell>>();
    private DataTableSpec dataTableSpec;


    private Collection<String> deleteColumns = new HashSet<String>();


    public TableUpdateCache(DataTableSpec dataTableSpec) {
        this.dataTableSpec = dataTableSpec;
    }


    public void add(DataRow dataRow, Attribute attribute, DataCell dataCell) {
        registerAttribute(attribute);

        cache.get(attribute).put(dataRow.getKey(), dataCell);
    }


    public void registerAttributes(List<Attribute> layoutAttributes) {
        for (Attribute layoutAttribute : layoutAttributes) {
            registerAttribute(layoutAttribute);
        }
    }


    public void registerAttribute(Attribute attribute) {
        if (!cache.keySet().contains(attribute)) {
            cache.put(attribute, new HashMap<RowKey, DataCell>());
        }
    }


    public ColumnRearranger createColRearranger() {

        ColumnRearranger c = new ColumnRearranger(dataTableSpec);
        // column spec of the appended column

        List<Attribute> existingAttributes = AttributeUtils.convert(dataTableSpec);

        for (final Attribute attribute : cache.keySet()) {

            // create a factory for each new attribute to be appended
            String newAttributeName = attribute.getName();

            DataColumnSpec attributeSpec = attribute.getColumnSpec();

            CellFactory factory = new SingleCellFactory(attributeSpec) {

                public DataCell getCell(DataRow row) {
                    Map<RowKey, DataCell> map = cache.get(attribute);

                    DataCell dataCell = map.get(row.getKey());

                    if (dataCell != null) {
                        return dataCell;
                    } else {
                        return DataType.getMissingCell();
                    }

                }
            };

            if (AttributeUtils.contains(existingAttributes, newAttributeName)) {
                c.replace(factory, newAttributeName);
            } else {
                c.append(factory);
            }
        }

        // delete all columns scheduled for removal
        c.remove(deleteColumns.toArray(new String[deleteColumns.size()]));

        return c;
    }


    public void registerDeleteColumn(String columnName) {
        deleteColumns.add(columnName);
    }

}
