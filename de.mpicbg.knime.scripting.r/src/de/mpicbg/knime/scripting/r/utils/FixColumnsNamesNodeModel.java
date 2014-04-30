package de.mpicbg.knime.scripting.r.utils;

import de.mpicbg.knime.knutils.AbstractNodeModel;
import de.mpicbg.knime.knutils.Attribute;
import de.mpicbg.knime.knutils.AttributeUtils;
import de.mpicbg.knime.scripting.r.RUtils;

import org.knime.core.data.*;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.defaultnodesettings.SettingsModelBoolean;
import org.rosuda.REngine.Rserve.RConnection;

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


    private DataTableSpec fixSpec(DataTableSpec inputSpecs) {
        List<Attribute> inputAttributes = AttributeUtils.convert(inputSpecs);
        List<DataColumnSpec> outputSpec = new ArrayList<DataColumnSpec>();

        RConnection connection = null;
        if (useMakeNames.getBooleanValue()) {
            connection = RUtils.createConnection();
        }

        for (Attribute attribute : inputAttributes) {

            String originalName = attribute.getName();

            String fixedName;
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

        if (useMakeNames.getBooleanValue()) {
            connection.close();
        }

        return new DataTableSpec(outputSpec.toArray(new DataColumnSpec[outputSpec.size()]));
    }


    private String fixNameWithR(String originalName, RConnection connection) {
        try {
            return connection.eval("make.names('" + originalName + "');").asStrings()[0];

        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
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
        return new DataTableSpec[]{fixSpec(inSpecs[0])};
    }
}