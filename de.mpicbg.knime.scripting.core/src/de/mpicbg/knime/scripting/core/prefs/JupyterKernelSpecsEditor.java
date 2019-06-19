package de.mpicbg.knime.scripting.core.prefs;

import java.util.List;

import org.eclipse.jface.preference.FieldEditor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;

public class JupyterKernelSpecsEditor extends FieldEditor {
	
	/* GUI-components */
    private Composite c_top;
    private Composite c_group;
    private Combo c_kernelCombo;
    
    private final String m_language;
    //private List<JupyterKernelSpec> m_kernelSpecs;
    
    //private static final String DEFAULT_SEPERATOR = ";";
    public static final String NO_SPEC = "< NO SELECTION >";

    
    public JupyterKernelSpecsEditor(String name, String labelText,Composite parent, String lang) {
		super(name, labelText, parent);
		
		m_language = lang;
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
        
        // kernel spec combo
    
        Label comboLabel = new Label(c_top, SWT.NONE);
        comboLabel.setText("Select kernel");
        gd = new GridData(SWT.LEFT, SWT.TOP, false, false);
        gd.horizontalSpan = numColumns - 1;
        comboLabel.setLayoutData(gd);
        
        c_kernelCombo = new Combo(c_top, SWT.READ_ONLY);
        c_kernelCombo.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, false));     
	}

	@Override
	protected void doLoad() {
		String items = getPreferenceStore().getString(getPreferenceName());       
        deserialzieJupyterSpecs(items);
	}

	private void deserialzieJupyterSpecs(String items) {
		// TODO Auto-generated method stub
		
	}

	@Override
	protected void doLoadDefault() {
		// TODO Auto-generated method stub

	}

	@Override
	protected void doStore() {
		// save everything to preference string
        String tString = serializeJupyterSpecs();
        if (tString != null)
            getPreferenceStore().setValue(getPreferenceName(), tString);
	}
	
	protected String serializeJupyterSpecs() {
		StringBuilder prefString = new StringBuilder();

		int idx = c_kernelCombo.getSelectionIndex();
		if(idx >= 0) {
			String name = c_kernelCombo.getItem(idx);
			if(!name.equals(NO_SPEC)) {			
				JupyterKernelSpec spec = (JupyterKernelSpec) c_kernelCombo.getData(name);
	            prefString.append("(");
	            prefString.append(spec.getName());
	            prefString.append(",");
	            prefString.append(spec.getDisplayName());
	            prefString.append(",");
	            prefString.append(spec.getLanguage());
	            prefString.append(")");
			}
		}

        return prefString.toString(); 
	}

	@Override
	public int getNumberOfControls() {
		return 2;
	}


		


	public void updateComboBoxes(List<JupyterKernelSpec> kernelSpecs) {
		
		//m_kernelSpecs = kernelSpecs;
		
		int selectedIdx = c_kernelCombo.getSelectionIndex();

		String spec = null;
		if(selectedIdx >= 0) spec = c_kernelCombo.getItem(selectedIdx);
				
		c_kernelCombo.removeAll();		
		c_kernelCombo.add(NO_SPEC);
			
		for(JupyterKernelSpec kSpec : kernelSpecs) {			
			if(kSpec.getLanguage().equals(m_language)) {
				String specString = kSpec.getDisplayName() + " (" + kSpec.getName() + ")";
				c_kernelCombo.add(specString);	
				c_kernelCombo.setData(specString, kSpec);
			}
		}
		
		if(selectedIdx >= 0) {
			int i = 0;
			for(String item : c_kernelCombo.getItems()) {
				if(item.equals(spec))
						c_kernelCombo.select(i);
				i++;
			}
		}
		if(c_kernelCombo.getSelectionIndex() == -1 && c_kernelCombo.getItemCount() > 0)
			c_kernelCombo.select(0);
		
	}

}
