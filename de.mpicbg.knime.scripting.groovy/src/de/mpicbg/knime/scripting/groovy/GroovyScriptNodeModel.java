/* @(#)$RCSfile$
 * $Revision$ $Date$ $Author$
 */
package de.mpicbg.knime.scripting.groovy;

import java.io.File;
import java.io.FilenameFilter;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.jface.preference.IPreferenceStore;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.port.PortObject;
import de.mpicbg.knime.scripting.core.AbstractScriptingNodeModel;
import de.mpicbg.knime.scripting.core.ScriptingModelConfig;
import de.mpicbg.knime.scripting.core.exceptions.KnimeScriptingException;
import de.mpicbg.knime.scripting.groovy.prefs.GroovyScriptingPreferenceInitializer;
import groovy.lang.Binding;
import groovy.lang.GroovyShell;


/**
 * This is the base implementation of the "JPython Script" node
 *
 * @author Tripos
 */
public class GroovyScriptNodeModel extends AbstractScriptingNodeModel {


	public static final String DEFAULT_SCRIPT = "//additional imports\n" +
			"import org.knime.core.data.DataColumnSpecCreator;\n" +
			"import org.knime.core.data.DataColumnSpec;\n\n" +
			"// create new output table spec\n" +
			"DataColumnSpecCreator c = new DataColumnSpecCreator(\"table name\", StringCell.TYPE);\n" +
			"DataColumnSpec cSpec1 = c.createSpec();\n" +
			"c = new DataColumnSpecCreator(\"number of columns\", IntCell.TYPE);\n" +
			"DataColumnSpec cSpec2 = c.createSpec();\n" +
			"c = new DataColumnSpecCreator(\"number of rows\", IntCell.TYPE);\n" +
			"DataColumnSpec cSpec3 = c.createSpec();\n\n" +
			"// open new output table container\n" +
			"DataTableSpec tSpec = new DataTableSpec(\"test\", cSpec1, cSpec2, cSpec3);\n" +
			"BufferedDataContainer con = exec.createDataContainer(tSpec);\n\n" +
			"// fill content from \"input\"\n" +
			"try {\n" +
			"  DataTableSpec iSpec = input.getDataTableSpec();\n" +
			"  String name = iSpec.getName();\n" +
			"  int rows = input.size();\n" +
			"  int cols = iSpec.getNumColumns();\n" +
			"  DataRow firstRow = new DefaultRow(new RowKey(\"first\"), new StringCell(name), new IntCell(cols), new IntCell(rows));\n" +
			"  con.addRowToTable(firstRow);\n\n" +
			"} catch(e) {\n" +
			"}\n" +
			"// fill content from \"input2\"\n" +
			"try {\n" +
			"  DataTableSpec iSpec = input2.getDataTableSpec();\n" +
			"  String name = iSpec.getName();\n" +
			"  int rows = input2.size();\n" +
			"  int cols = iSpec.getNumColumns();\n" +
			"  DataRow firstRow = new DefaultRow(new RowKey(\"second\"), new StringCell(name), new IntCell(cols), new IntCell(rows));\n" +
			"  con.addRowToTable(firstRow);\n\n" +
			"} catch(e) {\n" +
			"}\n\n" +
			"con.close()\n" +
			"return con.getTable();\n";


    private static final String defaultImports = "import de.mpicbg.knime.knutils.Attribute\n" +
            "import de.mpicbg.knime.knutils.AttributeUtils\n" +
            "import de.mpicbg.knime.knutils.InputTableAttribute\n" +
            "import de.mpicbg.knime.knutils.TableUpdateCache\n" +
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

	private static ScriptingModelConfig nodeModelConfig = new ScriptingModelConfig(
			createPorts(2, 1,2), 	// 2 inputs, both optional
			createPorts(1), 		// 1 output table
			new GroovyColumnSupport(), 	
			true, 					// use script
			false, 					// open in functionality
			false);					// use chunk settings

	/**
	 * {@inheritDoc}
	 */
    public GroovyScriptNodeModel() {
        super(nodeModelConfig);
    }
    
    @Override
    /**
     * {@inheritDoc}
     */
	public String getDefaultScript(String defaultScript) {
		return DEFAULT_SCRIPT;
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
                        urls.add(file.toURI().toURL());
                    }

                } else {
                    File file = new File(classPathEntry);
                    if (file.exists()) {
                        urls.add(file.toURI().toURL());
                    }
                }
            } catch (Throwable t) {
                logger.error("The url '" + classPathEntry + "' does not exist. Please correct the entry in Preferences > KNIME > Groovy Scripting");
            }
        }

        return new URLClassLoader(urls.toArray(new URL[0]), this.getClass().getClassLoader());
    }
    
	/**
	 * {@inheritDoc}
	 */
	@Override
	protected PortObject[] executeImpl(PortObject[] inData,
			ExecutionContext exec) throws Exception {
        BufferedDataTable in1;
        BufferedDataTable in2;

        // create the groovy enviroment and execute the script

        Binding binding = new Binding();

        switch (getNrInPorts()) {
            case 2:
                in2 = (BufferedDataTable) inData[1];
                binding.setVariable("input2", in2);
//                binding.setVariable("attributes2", Attribute.convert(in2.getDataTableSpec()));

            case 1:
                in1 = (BufferedDataTable) inData[0];
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
            throw new RuntimeException(t);
        }

        if (o == null) {
            throw new RuntimeException("No return value (of type BufferedDataTable");
        }

        if (!(o instanceof BufferedDataTable)) {
            throw new RuntimeException("return value is not of expected type BufferedDataTable");
        }

        BufferedDataTable outTable = (BufferedDataTable) o;

        return new BufferedDataTable[]{outTable};
	}


	@Override
	protected void openIn(PortObject[] inData, ExecutionContext exec)
			throws KnimeScriptingException {
		throw new KnimeScriptingException("The functionality to open data external is not yet implemented");
		
	}
}
