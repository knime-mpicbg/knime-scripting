package de.mpicbg.knime.scripting.r.utils;

import de.mpicbg.knime.knutils.AbstractNodeModel;
import de.mpicbg.knime.knutils.Attribute;
import de.mpicbg.knime.knutils.AttributeUtils;
import de.mpicbg.knime.scripting.core.exceptions.KnimeScriptingException;
import de.mpicbg.knime.scripting.r.RUtils;

import org.apache.commons.lang.StringEscapeUtils;
import org.knime.core.data.*;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.defaultnodesettings.SettingsModelBoolean;
import org.rosuda.REngine.REXPMismatchException;
import org.rosuda.REngine.Rserve.RConnection;
import org.rosuda.REngine.Rserve.RserveException;

import java.util.ArrayList;
import java.util.List;
/**
 * This is the model implementation of RSnippet. Improved R Integration for Knime
 *
 * @author Holger Brandl (MPI-CBG)
 */
public class FixColumnsNamesNodeModel extends AbstractNodeModel {

    SettingsModelBoolean useMakeNames = FixColumnsNamesNodeFactory.createPropStrictRNames();


    /**
     * Constructor for the node model.
     */
    protected FixColumnsNamesNodeModel() {
        super(1, 1);

        addSetting(useMakeNames);
    }


    /**
     * {@inheritDoc}
     */
    @Override
    protected BufferedDataTable[] execute(final BufferedDataTable[] inData,
                                          final ExecutionContext exec) throws Exception {


        BufferedDataTable inputTable = inData[0];
        DataTableSpec inputSpecs = inputTable.getDataTableSpec();

        DataTableSpec dts = fixSpec(inputSpecs);
        BufferedDataTable replacerTable = exec.createSpecReplacerTable(inputTable, dts);

        return new BufferedDataTable[]{replacerTable};
    }


    // the better approach might be
    // http://stat.ethz.ch/R-manual/R-devel/library/base/html/make.names.html


    private DataTableSpec fixSpec(DataTableSpec inputSpecs) throws KnimeScriptingException, RserveException, REXPMismatchException {
        List<Attribute> inputAttributes = AttributeUtils.convert(inputSpecs);
        List<DataColumnSpec> outputSpec = new ArrayList<DataColumnSpec>();
        
        //establish connection if names should be fixed by R
        RConnection connection = null;
        if(useMakeNames.getBooleanValue())
        	connection = RUtils.createConnection();

        for (Attribute attribute : inputAttributes) {

            String originalName = attribute.getName();
            String fixedName = null;

            if (useMakeNames.getBooleanValue()) {
					fixedName = fixNameWithR(originalName, connection);
            } else {
                fixedName = fixName(originalName);
            }

            if (!originalName.equals(fixedName)) {
                DataColumnSpecCreator columnSpecCreator = new DataColumnSpecCreator(fixedName, attribute.getType());

                if (attribute.getColumnSpec().getDomain() != null) {
                    DataColumnDomain domain = attribute.getColumnSpec().getDomain();
                    DataColumnDomain dataColumnDomain = new DataColumnDomainCreator(domain).createDomain();
                    columnSpecCreator.setDomain(dataColumnDomain);
                }

                outputSpec.add(columnSpecCreator.createSpec());
            } else {
                outputSpec.add(attribute.getColumnSpec());
            }
        }
        
        // close connection to R
        if(useMakeNames.getBooleanValue()) {
        	assert(connection != null);
        	connection.close();
        }
        	

        return new DataTableSpec(outputSpec.toArray(new DataColumnSpec[outputSpec.size()]));
    }


    private String fixNameWithR(String originalName, RConnection connection) throws RserveException, REXPMismatchException {
    	
    	// mask " in column names to avoid syntax errors with R
    	originalName = StringEscapeUtils.escapeJava(originalName); //create an additional plugin dependency 
    	//originalName = originalName.replace('"', '.');
    	//originalName = originalName.replace('\', '.');
    			
    	// connection should not be null, as it throws exception when creation fails
    	assert(connection != null);
    	return connection.eval("make.unique(make.names(\"" + originalName + "\"))").asStrings()[0];

    }


    private String fixName(String originalName) {
        while (originalName.startsWith("+")) {
            originalName = originalName.replaceAll("[+]", "Plus");
        }

        while (originalName.startsWith("-")) {
            originalName = originalName.replaceAll("[-]", "Minus");
        }

        while (originalName.startsWith("*")) {
            originalName = originalName.replaceAll("[*]", "Times");
        }

        while (originalName.startsWith("%")) {
            originalName = originalName.replaceAll("[%]", "Percent");
        }

        while (originalName.startsWith(":")) {
            originalName = originalName.replaceAll("[:]", "DivBy");
        }

        while (originalName.startsWith("/")) {
            originalName = originalName.replaceFirst("[/]", "DivBy");
        }

        return originalName;

    }


    public static void main(String[] args) {
        System.err.println(new FixColumnsNamesNodeModel().fixName("++serse%1324"));
        ;
    }


    @Override
    protected DataTableSpec[] configure(DataTableSpec[] inSpecs) throws InvalidSettingsException {
        try {
			return new DataTableSpec[]{fixSpec(inSpecs[0])};
			
		} catch (RserveException e) {
			e.printStackTrace();
		} catch (REXPMismatchException e) {
			e.printStackTrace();
		} catch (KnimeScriptingException e) {
			e.printStackTrace();
		}
        logger.warn("generating output table spec failed");
        return null;
    }
}