package de.mpicbg.knime.knutils;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.def.StringCell;
import org.knime.core.data.property.ColorHandler;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NotConfigurableException;
import org.knime.core.node.defaultnodesettings.SettingsModelFilterString;

import java.util.*;


/**
 * Document me!
 *
 * @author Holger Brandl
 */
public class AttributeUtils {


    public static List<String> toStringList(Set<DataCell> values) {
        ArrayList<String> list = new ArrayList<String>();
        for (DataCell value : values) {
            if (!(value instanceof StringCell)) {
                throw new RuntimeException("expected string in cell but found" + value);
            }
            list.add(((StringCell) value).getStringValue());
        }

        return list;
    }

    /**
    * Input table will be split according to unique values of the factor column (keyset = String)
    */
    public static Map<String, List<DataRow>> splitRows(BufferedDataTable table, Attribute factor) {
        Map<Object, List<DataRow>> groupedRows = splitRowsGeneric(table, factor);

        Map<String, List<DataRow>> groupedRowsStringKeys = new LinkedHashMap<String, List<DataRow>>();
        for (Object o : groupedRows.keySet()) {
            groupedRowsStringKeys.put(o == null ? null : o.toString(), groupedRows.get(o));
        }

        return groupedRowsStringKeys;
    }

    /**
    * Input table will be split according to unique values of the factor column (keyset = Object)
    */
    public static Map<Object, List<DataRow>> splitRowsGeneric(BufferedDataTable table, Attribute factor) {
        Map<Object, List<DataRow>> splitScreen = new LinkedHashMap<Object, List<DataRow>>();

        for (DataRow dataRow : table) {
            Object groupFactor = factor.getValue(dataRow);

            if (!splitScreen.containsKey(groupFactor)) {
                splitScreen.put(groupFactor, new ArrayList<DataRow>());
            }

            splitScreen.get(groupFactor).add(dataRow);
        }

        return splitScreen;
    }


    public static List<DataRow> filterByAttributeValue(List<DataRow> plate, Attribute attribute, String value) {
        List<DataRow> filteredList = new ArrayList<DataRow>();

        for (DataRow dataRow : plate) {
            if (value.equals(attribute.getNominalAttribute(dataRow))) {
                filteredList.add(dataRow);
            }
        }

        return filteredList;
    }


    public static DataTableSpec compileTableSpecs(List<Attribute> attributes) {
        return new DataTableSpec(compileSpecs(attributes));
    }


    public static DataColumnSpec[] compileSpecs(List<Attribute> attributes) {
        DataColumnSpec[] compiledSpecs = new DataColumnSpec[attributes.size()];
        for (int i = 0, attributesSize = attributes.size(); i < attributesSize; i++) {
            Attribute attribute = attributes.get(i);
            compiledSpecs[i] = attribute.getColumnSpec();
        }

        return compiledSpecs;

    }


    public static Attribute find(List<Attribute> attributes, String attributeName) {
        for (Attribute attribute : attributes) {
            if (attribute.getName().equals(attributeName))
                return attribute;
        }

        return null;
    }


    public static List<Attribute> compileSpecs(List<String> includeList, BufferedDataTable input) {
        List<Attribute> attributes = new ArrayList<Attribute>();
        for (String attributeName : includeList) {
            attributes.add(new InputTableAttribute(attributeName, input));

        }

        return attributes;
    }


    /**
     * Method takes table spec in KNIME format and returns it as attributes list
     */
    public static List<Attribute> convert(DataTableSpec dataTableSpec) {
        List<Attribute> attributes = new ArrayList<Attribute>();
        for (DataColumnSpec columnSpec : dataTableSpec) {
            attributes.add(new InputTableAttribute<Object>(columnSpec, dataTableSpec.findColumnIndex(columnSpec.getName())));
        }

        return attributes;
    }


    public static boolean contains(List<Attribute> colAttributes, String attrName) {
        for (Attribute colAttribute : colAttributes) {
            if (colAttribute.getName().equals(attrName))
                return true;
        }

        return false;
    }


    public static boolean validate(List<String> includeReadouts, DataTableSpec spec) {
        for (String includeReadout : includeReadouts) {
            if (spec.getColumnSpec(includeReadout) == null) {
                return false;
            }
        }

        return true;
    }


    public static List<String> toStringList(List<Attribute> attrs) {
        List<String> names = new ArrayList<String>();
        for (Attribute attr : attrs) {
            names.add(attr.getName());
        }

        return names;
    }


    public static void updateExcludeToNonSelected(DataTableSpec spec, SettingsModelFilterString filterString) throws NotConfigurableException {

        List<String> excludeList = toStringList(convert(spec));
        excludeList.removeAll(filterString.getIncludeList());

        filterString.setExcludeList(excludeList);
    }


    /**
     * Method that returns the Attribute belonging to the column that holds the {@link ColorHandler}.
     * Only one column can hold the column handler (multiple consecutive {@link org.knime.base.node.viz.property.color.ColorAppender2NodeModel}
     * overwrite the previous {@link ColorHandler} (tested with org.knime.base 2.6.2))
     *
     * @param spec of the table of which you want to find the color handler attribute
     * @return attribute belonging to the column holding the {@link ColorHandler}
     */
    public static Attribute<Object> getKnimeColorAttribute(DataTableSpec spec) {
        for (int index = 0; index < spec.getNumColumns(); index++) {
            DataColumnSpec cspec = spec.getColumnSpec(index);
            if ( cspec.getColorHandler() != null ) {
                return new Attribute<Object>(cspec.getName(), cspec.getType());
            }
        }
        return null;
    }

}
