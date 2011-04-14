package de.mpicbg.tds.knime.scripting.r;

import de.mpicbg.tds.knime.knutils.scripting.AbstractTableScriptingNodeModel;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataType;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.ExecutionContext;
import org.rosuda.REngine.Rserve.RConnection;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;


/**
 * This is the model implementation of RSnippet. Improved R Integration for Knime
 *
 * @author Holger Brandl (MPI-CBG)
 */
public class RSnippetNodeModel extends AbstractTableScriptingNodeModel {

    public static final String R_INVAR_BASE_NAME = "kIn";
    public static final String R_OUTVAR_BASE_NAME = "rOut";


    public RSnippetNodeModel(int numInputs, int numOutputs) {
        super(numInputs, numOutputs);
    }


    @Override
    protected BufferedDataTable[] execute(final BufferedDataTable[] inData,
                                          final ExecutionContext exec) throws Exception {

        long starttime = System.currentTimeMillis();

        RConnection connection = RUtils.createConnection();

        // 1) convert exampleSet ihnto data-frame and put into the r-workspace
        RUtils.pushToR(inData, connection, exec);

        // 2) run the script  (remove all linebreaks and other no space whitespace-characters
//        connection.eval(RUtils.prepare4RExecution(script.getStringValue()));

        String rawScript = prepareScript();

        // LEGACY: we still support the old R workspace variable names ('R' for input and 'R' also for output)
        rawScript = RUtils.supportOldVarNames(rawScript);

        String fixedScript = RUtils.fixEncoding(rawScript);

        connection.voidEval(fixedScript);

        Map<String, DataType> typeMapping = getColumnTypeMapping(inData[0]);

        // 3) extract output data-frame from R
        BufferedDataTable dataTable = RUtils.convert2DataTable(exec, connection.eval(R_OUTVAR_BASE_NAME), typeMapping);

        connection.eval("rm(list = ls(all = TRUE));");
        connection.close();

        return new BufferedDataTable[]{dataTable};
    }


    private static Map<String, DataType> getColumnTypeMapping(BufferedDataTable bufferedDataTable) {
        Iterator<DataColumnSpec> dataColumnSpecIterator = bufferedDataTable.getSpec().iterator();
        Map<String, DataType> typeMapping = new HashMap<String, DataType>();
        while (dataColumnSpecIterator.hasNext()) {
            DataColumnSpec dataColumnSpec = dataColumnSpecIterator.next();
            typeMapping.put(dataColumnSpec.getName(), dataColumnSpec.getType());
        }

        return typeMapping;
    }


    public String getDefaultScript() {
        return RUtils.SCRIPT_PROPERTY_DEFAULT;
    }
}

