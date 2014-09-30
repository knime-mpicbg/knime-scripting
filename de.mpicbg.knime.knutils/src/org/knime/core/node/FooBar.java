package org.knime.core.node;

import de.mpicbg.knime.knutils.Attribute;
import de.mpicbg.knime.knutils.AttributeUtils;

import org.knime.base.node.io.arffreader.ARFFReaderNodeFactory;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTable;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.RowIterator;
import org.knime.core.data.def.DefaultRow;
import org.knime.core.data.def.DefaultRowIterator;
import org.knime.core.data.def.StringCell;

import java.util.Arrays;


/**
 * Document me!
 *
 * @author Holger Brandl
 */
public class FooBar {

    public static void main(String[] args) throws CanceledExecutionException {

        ExecutionContext executionContext = new ExecutionContext(null, new Node(new ARFFReaderNodeFactory()));

        DataTable table = new DataTable() {
            public DataTableSpec getDataTableSpec() {
                return AttributeUtils.compileTableSpecs(Arrays.asList(new Attribute("test", StringCell.TYPE)));
            }


            public RowIterator iterator() {
                return new DefaultRowIterator(new DataRow[]{new DefaultRow("test", new StringCell("test"))});
            }
        };

        BufferedDataTable bufTable = executionContext.createBufferedDataTable(table, new ExecutionMonitor());
        System.err.println("table is " + bufTable);

    }

}
