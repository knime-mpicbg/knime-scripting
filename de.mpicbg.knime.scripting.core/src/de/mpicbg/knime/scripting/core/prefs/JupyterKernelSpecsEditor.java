package de.mpicbg.knime.scripting.core.prefs;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.lang.ProcessBuilder.Redirect;
import java.nio.file.Files;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonReader;

import org.eclipse.jface.preference.FieldEditor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;

import de.mpicbg.knime.scripting.core.exceptions.KnimeScriptingException;

public class JupyterKernelSpecsEditor extends FieldEditor {
	
	/* GUI-components */
    private Composite c_top;
    private Composite c_group;
    private Combo c_py2KernelCombo;
    private Combo c_py3KernelCombo;
    private Combo c_rKernelCombo;
    
    private List<JupyterKernelSpec> m_kernelSpecs;

    
    public JupyterKernelSpecsEditor(String name, String labelText,Composite parent) {
		super(name, labelText, parent);
		
		m_kernelSpecs = new LinkedList<JupyterKernelSpec>();
	}
    
	@Override
	protected void adjustForNumColumns(int numColumns) {
		((GridData) c_top.getLayoutData()).horizontalSpan = numColumns;
	}

	@Override
	protected void doFillIntoGrid(Composite parent, int numColumns) {
		
    	/* Layout comments:
    	 * 
    	 * component are sequentially filled into numColumns
    	 * by default each component will use 1 column
    	 * GridData can be set to use more that one columns
    	 */
		
		GridData gd = new GridData(SWT.FILL, SWT.TOP, true, false);
        gd.horizontalSpan = numColumns;
    	
        c_top = parent;
        c_top.setLayoutData(gd);
        
        c_group = new Composite(c_top, SWT.BORDER);

        GridLayout newgd = new GridLayout(2, false);
        c_group.setLayout(newgd);
        c_group.setLayoutData(gd);

        // set label
        Label label = getLabelControl(c_group);
        GridData labelData = new GridData();
        labelData.horizontalSpan = numColumns;
        label.setLayoutData(labelData);
        
        // python 2
    
        Label comboLabel = new Label(c_group, SWT.NONE);
        comboLabel.setText("Python 2");
        comboLabel.setLayoutData(new GridData(SWT.LEFT, SWT.TOP, false, false));
        
        c_py2KernelCombo = new Combo(c_group, SWT.READ_ONLY);
        c_py2KernelCombo.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, false));
        
        // python 3
        
        comboLabel = new Label(c_group, SWT.NONE);
        comboLabel.setText("Python 3");
        comboLabel.setLayoutData(new GridData(SWT.LEFT, SWT.TOP, false, false));
        
        c_py3KernelCombo = new Combo(c_group, SWT.READ_ONLY);
        c_py3KernelCombo.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, false));
        
        // R
        
        comboLabel = new Label(c_group, SWT.NONE);
        comboLabel.setText("R");
        comboLabel.setLayoutData(new GridData(SWT.LEFT, SWT.TOP, false, false));
        
        c_rKernelCombo = new Combo(c_group, SWT.READ_ONLY);
        c_rKernelCombo.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, false));
        
        
	}

	@Override
	protected void doLoad() {
		// TODO Auto-generated method stub

	}

	@Override
	protected void doLoadDefault() {
		// TODO Auto-generated method stub

	}

	@Override
	protected void doStore() {
		// TODO Auto-generated method stub

	}

	@Override
	public int getNumberOfControls() {
		return 2;
	}

	public void updateKernelSpecs(String jupyterLocation) throws IOException, KnimeScriptingException, InterruptedException {
		
		// use temporary list to fill with new content
		List<JupyterKernelSpec> kernelSpecs = new LinkedList<JupyterKernelSpec>();
		
		ProcessBuilder pb = new ProcessBuilder(jupyterLocation, "kernelspec", "list", "--json");
		File outFile = Files.createTempFile("kernelspecs_", ".txt").toFile();
		//File outFile = outPath.toFile();
		
		pb.redirectErrorStream(true);
		pb.redirectOutput(Redirect.appendTo(outFile));
		Process p = pb.start();
		p.waitFor(5, TimeUnit.SECONDS);
				
		try(JsonReader reader = Json.createReader(new FileReader(outFile))) {
			JsonObject jsonObject = reader.readObject();
			JsonObject kernelspecs = jsonObject.getJsonObject("kernelspecs");
			
			if(kernelspecs == null)
				throw new KnimeScriptingException("failed to read JSON. Expected key \"kernelspecs\"");
			
			for(String key : kernelspecs.keySet()) {
				String name = key;
				JsonObject spec = kernelspecs.getJsonObject(name).getJsonObject("spec");
				if(spec == null)
					throw new KnimeScriptingException("failed to read JSON. Expected key \"spec\"");
				String displayName = spec.getString("display_name");
				String language = spec.getString("language");
				
				if(JupyterKernelSpec.isValidLanguage(language))
					kernelSpecs.add(new JupyterKernelSpec(name, displayName, language));
			}
			
		}
		
		if(outFile.exists())
			outFile.delete();
		
		m_kernelSpecs.clear();
		m_kernelSpecs.addAll(kernelSpecs);
		
		updateComboBoxes();	
	}

	private void updateComboBoxes() {
		
		int py2SelectedIdx = c_py2KernelCombo.getSelectionIndex();
		int py3SelectedIdx = c_py3KernelCombo.getSelectionIndex();
		int rSelectedIdx = c_rKernelCombo.getSelectionIndex();
		String py2Spec = null;
		if(py2SelectedIdx >= 0) py2Spec = c_py2KernelCombo.getItem(py2SelectedIdx);
		String py3Spec = null;
		if(py3SelectedIdx >= 0) py3Spec = c_py3KernelCombo.getItem(py3SelectedIdx);
		String rSpec = null;
		if(rSelectedIdx >= 0) rSpec = c_rKernelCombo.getItem(rSelectedIdx);
		
		c_py2KernelCombo.removeAll();
		c_py3KernelCombo.removeAll();
		c_rKernelCombo.removeAll();
		
		for(JupyterKernelSpec spec : m_kernelSpecs) {
			
			if(spec.getLanguage().equals(JupyterKernelSpec.PYTHON_LANG)) {
				c_py2KernelCombo.add(spec.getDisplayName());
				c_py3KernelCombo.add(spec.getDisplayName());
			}
			if(spec.getLanguage().equals(JupyterKernelSpec.R_LANG)) {
				c_rKernelCombo.add(spec.getDisplayName());
			}
			
		}
		
		if(py2SelectedIdx >= 0) {
			int i = 0;
			for(String item : c_py2KernelCombo.getItems()) {
				if(item.equals(py2Spec))
						c_py2KernelCombo.select(i);
				i++;
			}
		}
		if(c_py2KernelCombo.getSelectionIndex() == -1 && c_py2KernelCombo.getItemCount() > 0)
			c_py2KernelCombo.select(0);
		
		if(py3SelectedIdx >= 0) {
			int i = 0;
			for(String item : c_py3KernelCombo.getItems()) {
				if(item.equals(py3Spec))
						c_py3KernelCombo.select(i);
				i++;
			}
		}
		if(c_py3KernelCombo.getSelectionIndex() == -1 && c_py3KernelCombo.getItemCount() > 0)
			c_py3KernelCombo.select(0);
		
		if(rSelectedIdx >= 0) {
			int i = 0;
			for(String item : c_rKernelCombo.getItems()) {
				if(item.equals(rSpec))
						c_rKernelCombo.select(i);
				i++;
			}
		}
		if(c_rKernelCombo.getSelectionIndex() == -1 && c_rKernelCombo.getItemCount() > 0)
			c_rKernelCombo.select(0);
	}

}
