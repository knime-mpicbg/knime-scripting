package de.mpicbg.tds.knime.scripting;

import de.mpicbg.tds.knime.knutils.Attribute;
import de.mpicbg.tds.knime.knutils.AttributeUtils;
import de.mpicbg.tds.knime.knutils.TableUpdateCache;
import org.knime.core.data.*;
import org.knime.core.data.def.DefaultRow;
import org.knime.core.data.def.StringCell;
import org.knime.core.node.*;

import java.io.File;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;


/**
 * Document me!
 *
 * @author Holger Brandl
 */
public class ScriptExamples {

    public static BufferedDataTable execute(ExecutionContext exec) throws CanceledExecutionException, SQLException {


        String dirName = "/Users/brandl/Desktop/waldmann";


        DataColumnSpec[] allColSpecs = new DataColumnSpec[1];

        allColSpecs[0] = new DataColumnSpecCreator("File", StringCell.TYPE).createSpec();
        DataTableSpec outputSpec = new DataTableSpec(allColSpecs);

        BufferedDataContainer container = exec.createDataContainer(outputSpec);


        List<File> dirContents = new ArrayList<File>(Arrays.asList(new File(dirName).listFiles()));

        for (int i = 0; i < dirContents.size(); i++) {
            File file = dirContents.get(i);

            DataCell[] cells = new DataCell[1];
            cells[0] = new StringCell(file.getAbsolutePath());
            DataRow row = new DefaultRow(new RowKey("Row " + i), cells);
            container.addRowToTable(row);
        }

        container.close();

        return container.getTable();


    }


    public static BufferedDataTable replaceEmptyWithMissingCells(ExecutionContext exec, BufferedDataTable input) throws CanceledExecutionException, SQLException {


        String[] replaceCols = new String[]{};
        List<String> replaceColumns = Arrays.asList(replaceCols);

        List<Attribute> attributes = AttributeUtils.compileSpecs(replaceColumns, input);

        TableUpdateCache cache = new TableUpdateCache(input.getDataTableSpec());

        //iterate over all rows
        for (DataRow dataRow : input) {
            for (Attribute attribute : attributes) {

                String value = attribute.getRawValue(dataRow);


                if (value == null || value.isEmpty()) {
                    cache.add(dataRow, attribute, DataType.getMissingCell());

                } else {
                    cache.add(dataRow, attribute, attribute.createCell(dataRow));
                }
            }
        }

        // convert the plate collection into a knime-table
        return exec.createColumnRearrangeTable(input, cache.createColRearranger(), exec);

    }


    public static BufferedDataTable addMissingPCRColumnsForEsibase(ExecutionContext exec, BufferedDataTable input) throws CanceledExecutionException, SQLException {

        TableUpdateCache cache = new TableUpdateCache(input.getDataTableSpec());

// create a new attribute with a name and a type
        Attribute barcode = new Attribute("RepeatOf Barcode", StringCell.TYPE);
        Attribute row = new Attribute("RepeatOf Row", StringCell.TYPE);
        Attribute column = new Attribute("RepeatOf Column", StringCell.TYPE);

        cache.registerAttribute(barcode);
        cache.registerAttribute(row);
        cache.registerAttribute(column);
//for (DataRow dataRow : input) {
//    cache.add(dataRow, attribute, new StringCell"hello knime"));
//}

        return exec.createColumnRearrangeTable(input, cache.createColRearranger(), exec);

    }


    public static BufferedDataTable logColumnNames(ExecutionContext exec, BufferedDataTable input) throws CanceledExecutionException, SQLException {

        List<Attribute> attrs = AttributeUtils.convert(input.getDataTableSpec());
        List<String> attrNames = AttributeUtils.toStringList(attrs);
        NodeLogger.getLogger("colnames").warn(attrNames.toString());

        return input;
    }

}
