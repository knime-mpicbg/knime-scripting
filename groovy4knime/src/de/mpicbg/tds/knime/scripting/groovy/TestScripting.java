package de.mpicbg.tds.knime.scripting.groovy;

import de.mpicbg.tds.knime.knutils.Attribute;
import de.mpicbg.tds.knime.knutils.AttributeUtils;
import de.mpicbg.tds.knime.knutils.TableUpdateCache;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.RowKey;
import org.knime.core.data.def.DefaultRow;
import org.knime.core.data.def.StringCell;
import org.knime.core.node.BufferedDataContainer;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;

import java.util.Arrays;
import java.util.List;


/**
 * Document me!
 *
 * @author Holger Brandl
 */
public class TestScripting {


    public static BufferedDataTable devScript0(ExecutionContext exec) {
        List<Attribute> attributes = Arrays.asList(new Attribute("test1", StringCell.TYPE));

        DataTableSpec dataTableSpec = AttributeUtils.compileTableSpecs(attributes);
        BufferedDataContainer container = exec.createDataContainer(dataTableSpec);


        for (int rowCounter = 1; rowCounter < 100; rowCounter++) {

            DataCell[] knimeRow = new DataCell[attributes.size()];
            knimeRow[0] = attributes.get(0).createCell("test" + (rowCounter * 2));

            DataRow tableRow = new DefaultRow(new RowKey(rowCounter + ""), knimeRow);
            container.addRowToTable(tableRow);
        }

        container.close();

        return exec.createWrappedTable(container.getTable());
    }


    public static BufferedDataTable devScript1(ExecutionContext exec, BufferedDataTable input) throws CanceledExecutionException {

        TableUpdateCache cache = new TableUpdateCache(input.getDataTableSpec());

// Get an existing input attribute by name
//  Attribute attribute = new Attribute("$$INPUT_COLUMN", input);

// create a new attribute with a name and a type
        Attribute attribute = new Attribute("new attribute", StringCell.TYPE);


        for (DataRow dataRow : input) {
            cache.add(dataRow, attribute, new StringCell("hello knime"));
        }

// convert the plate collection into a knime-table
        return exec.createColumnRearrangeTable(input, cache.createColRearranger(), exec);
    }


    public static void devScript2(ExecutionContext exec, BufferedDataTable in) {

        // add a column

    }

}
