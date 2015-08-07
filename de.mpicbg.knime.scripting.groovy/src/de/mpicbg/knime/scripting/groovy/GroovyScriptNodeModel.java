/* @(#)$RCSfile$
 * $Revision$ $Date$ $Author$
 */
package de.mpicbg.knime.scripting.groovy;

import de.mpicbg.knime.scripting.core.AbstractTableScriptingNodeModel;
import de.mpicbg.knime.scripting.core.exceptions.KnimeScriptingException;
import de.mpicbg.knime.scripting.groovy.prefs.GroovyScriptingPreferenceInitializer;
import groovy.lang.Binding;
import groovy.lang.GroovyShell;
import org.eclipse.jface.preference.IPreferenceStore;
import org.knime.core.data.DataTableSpec;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.InvalidSettingsException;

import java.io.File;
import java.io.FilenameFilter;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.List;


/**
 * This is the base implementation of the "JPython Script" node
 *
 * @author Tripos
 */
public class GroovyScriptNodeModel extends AbstractTableScriptingNodeModel {


    public static final String DEFAULT_SCRIPT_OLD = "TableUpdateCache cache = new TableUpdateCache(input.getDataTableSpec());\n" +
            "\n" +
            "return exec.createColumnRearrangeTable(input, cache.createColRearranger(), exec);";


    public static final String DEFAULT_SCRIPT = "//initialize output table:\n" +
            "TableUpdateCache cache = new TableUpdateCache(input.getDataTableSpec());\n" +
            "\n" +
            "// get an existing input attribute by name:\n" +
            "//  Attribute attribute = new InputTableAttribute(\"$$INPUT_COLUMN\", input);\n" +
            "\n" +
            "// create a new attribute with a name and a type:\n" +
            "Attribute attribute = new Attribute(\"new attribute\", StringCell.TYPE);\n" +
            "\n" +
            "\n" +
            "for (DataRow dataRow : input) {\n" +
            "    // get an attribute value:\n" +
            "    // Double value = (Double) attribute.getValue(dataRow);\n" +
            "\n" +
            "    // Put stuff into table\n" +
            "    cache.add(dataRow, attribute, new StringCell(\"hello knime\"));\n" +
            "}\n" +
            "\n" +
            "\n" +
            "return exec.createColumnRearrangeTable(input, cache.createColRearranger(), exec);";


    private static final String defaultImports = "import de.mpicbg.knime.knutilsAttribute\n" +
            "import de.mpicbg.knime.knutilsAttributeUtils\n" +
            "import de.mpicbg.knime.knutilsInputTableAttribute\n" +
            "import de.mpicbg.knime.knutilsTableUpdateCache\n" +
            "import org.knime.core.data.DataCell\n" +
            "import org.knime.core.data.def.StringCell\n" +
            "import org.knime.core.data.def.DoubleCell\n" +
            "import org.knime.core.data.def.IntCell\n" +
            "import org.knime.core.data.DataRow\n" +
            "import org.knime.core.data.DataType\n" +
            "import org.knime.core.data.DataTableSpec\n" +
            "import org.knime.core.data.RowKey\n" +
            "import org.knime.core.data.def.DefaultRow\n" +
            "import org.knime.core.data.def.StringCell\n" +
            "import org.knime.core.node.BufferedDataContainer\n" +
            "import org.knime.core.node.BufferedDataTable\n" +
            "import org.knime.core.node.ExecutionContext\n\n" +
            "import org.knime.core.node.CanceledExecutionException\n\n";


    protected GroovyScriptNodeModel() {
        super(2, 1, 1, 2);
    }


    protected GroovyScriptNodeModel(int nrInDataPorts, int nrOutDataPorts) {
        super(nrInDataPorts, nrOutDataPorts);
    }


    @Override
    public String getDefaultScript() {
        return DEFAULT_SCRIPT;
    }


    @Override
    protected DataTableSpec[] configure(DataTableSpec[] inSpecs) throws InvalidSettingsException {
        return new DataTableSpec[]{null};
    }


    protected BufferedDataTable[] execute(final BufferedDataTable[] inData, final ExecutionContext exec) throws Exception {

        BufferedDataTable in1;
        BufferedDataTable in2;

        // create the groovy enviroment and execute the script

        Binding binding = new Binding();


        switch (numInputs) {
            case 2:
                in2 = inData[1];
                binding.setVariable("input2", in2);
//                binding.setVariable("attributes2", Attribute.convert(in2.getDataTableSpec()));

            case 1:
                in1 = inData[0];
                binding.setVariable("input", in1);
//                binding.setVariable("attributes", Attribute.convert(in1.getDataTableSpec()));

            case 0:
                break;
        }

        // register the exec (in case the script needs to create a new table
        binding.setVariable("exec", exec);

        ClassLoader loader = createClassLoader();

        GroovyShell shell = new GroovyShell(loader, binding);
        Object o;

        try {
            String script = prepareScript();
            o = shell.evaluate(defaultImports + script);
        } catch (Throwable t) {
//            logger.error(t.toString()); // does not include any stacktrace
            throw new RuntimeException(t);
        }

        if (o == null) {
            throw new RuntimeException("No return value (of type BufferedDataTable");
        }

        if (!(o instanceof BufferedDataTable)) {
            throw new RuntimeException("return value is not of expected type BufferedDataTable");
        }

        BufferedDataTable outTable = (BufferedDataTable) o;

//        if (outContainer.isOpen()) {
//            outContainer.close();
//        }
//
//        return new BufferedDataTable[]{exec.createBufferedDataTable(outContainer.getTable(), exec)};
        return new BufferedDataTable[]{outTable};
    }


    private ClassLoader createClassLoader() throws MalformedURLException {
        IPreferenceStore prefStore = GroovyScriptingBundleActivator.getDefault().getPreferenceStore();
        String classpathAddons = prefStore.getString(GroovyScriptingPreferenceInitializer.GROOVY_CLASSPATH_ADDONS);

        classpathAddons.replace("\n", ";");

        List<URL> urls = new ArrayList<URL>();
        for (String classPathEntry : classpathAddons.split(";")) {
            classPathEntry = classPathEntry.trim();

            if (classPathEntry.trim().startsWith("#"))
                continue;

            // replace patterns
            classPathEntry = classPathEntry.replace("{KNIME.HOME}", System.getProperty("osgi.syspath"));

            try {
                if (classPathEntry.endsWith("*")) {
                    FilenameFilter jarFilter = new FilenameFilter() {
                        public boolean accept(File file, String s) {
                            return s.endsWith(".jar");
                        }
                    };
                    for (File file : new File(classPathEntry).getParentFile().listFiles(jarFilter)) {
                        urls.add(file.toURL());
                    }

                } else {
                    File file = new File(classPathEntry);
                    if (file.exists()) {
                        urls.add(file.toURL());
                    }
                }
            } catch (Throwable t) {
                logger.error("The url '" + classPathEntry + "' does not exist. Please correct the entry in Preferences > KNIME > Groovy Scripting");
            }
        }

//        ClassLoader loader = new PluginClassLoader(urls, this.getClass().getClassLoader());
        return new URLClassLoader(urls.toArray(new URL[0]), this.getClass().getClassLoader());
    }
    
	@Override
	protected void openIn(BufferedDataTable[] inData, ExecutionContext exec) throws KnimeScriptingException {
		throw new KnimeScriptingException("not yet implemented");
	}


	@Override
	protected BufferedDataTable[] executeImpl(BufferedDataTable[] inData,
			ExecutionContext exec) throws Exception {
		// TODO Auto-generated method stub
		return null;
	}
}
