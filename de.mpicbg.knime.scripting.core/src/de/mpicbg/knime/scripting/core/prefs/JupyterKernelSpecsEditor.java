package de.mpicbg.knime.scripting.core.prefs;

import java.util.LinkedList;
import java.util.List;

import org.eclipse.jface.preference.FieldEditor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;

/**
 * Eclipse preferences field editor for {@link JupyterKernelSpec}
 * 
 * @author Antje Janosch
 *
 */
public class JupyterKernelSpecsEditor extends FieldEditor {
	
	/* GUI-components */
    private Combo c_kernelCombo;
    
    /** language for which the editor should provide specs */
    private final String m_language;
    
    /** constant for de-selection */
    public static final String NO_SPEC = "< NO SELECTION >";
    /** constant for communicating failure in loading specs */
    private static final String NO_SPEC_AVAILABLE = "< FAILED TO LOAD SPECS >";

    
    /**
     * constructor
     * 
     * @param name of the preference
     * @param labelText of the field editor
     * @param parent composite
     * @param lang - language of the spec
     */
    public JupyterKernelSpecsEditor(String name, String labelText,Composite parent, String lang) {
		super(name, labelText, parent);
		
		m_language = lang;
	}
    
    /**
     * {@inheritDoc}
     */
	@Override
	protected void adjustForNumColumns(int numColumns) {
		((GridData) c_kernelCombo.getLayoutData()).horizontalSpan = numColumns-1;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void doFillIntoGrid(Composite parent, int numColumns) {
		
    	/* Layout comments:
    	 * 
    	 * component are sequentially filled into numColumns
    	 * by default each component will use 1 column
    	 * GridData can be set to use more that one columns
    	 */

        Label comboLabel = new Label(parent, SWT.NONE);
        comboLabel.setText(getLabelText());
        GridData gd = new GridData(SWT.LEFT, SWT.TOP, false, false);
        gd.horizontalSpan = 1;
        comboLabel.setLayoutData(gd);
        
        c_kernelCombo = new Combo(parent, SWT.READ_ONLY);
        gd = new GridData(SWT.FILL, SWT.TOP, true, false);
        gd.horizontalSpan = numColumns - 1;
        c_kernelCombo.setLayoutData(gd);     
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void doLoad() {
		String items = getPreferenceStore().getString(getPreferenceName());       
        deserialzieJupyterSpecs(items);
	}

	/**
	 * deserialize specs from preference string and update field editor
	 * 
	 * @param items - preference string
	 */
	private void deserialzieJupyterSpecs(String items) {
		
		String[] singleItems = items.split(";");
		
		int len = singleItems.length;
		
		if(len == 1) {
			// no specs available
			updateComboBoxes(null);
		} else {
			int selectedIndex = Integer.parseInt(singleItems[0]);
			List<JupyterKernelSpec> kernelSpecs = new LinkedList<JupyterKernelSpec>();
			
			for(int i = 1; i < len; i++) {
				JupyterKernelSpec spec = JupyterKernelSpec.fromPrefString(singleItems[i]);
				if(!spec.getName().equals(NO_SPEC))
					kernelSpecs.add(spec);
			}
			
			updateComboBoxes(kernelSpecs);
			c_kernelCombo.select(selectedIndex);
		}	
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void doLoadDefault() {
		c_kernelCombo.removeAll();		
		c_kernelCombo.add(NO_SPEC_AVAILABLE);
		c_kernelCombo.select(0);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void doStore() {
		// save everything to preference string
        String tString = serializeJupyterSpecs();
        if (tString != null)
            getPreferenceStore().setValue(getPreferenceName(), tString);
	}
	
	/**
	 * create preference string from field editor content
	 * @return preference string
	 */
	protected String serializeJupyterSpecs() {
		StringBuilder prefString = new StringBuilder();

		int idx = c_kernelCombo.getSelectionIndex();
		
		prefString.append(idx);
		
		for(int i = 0; i < c_kernelCombo.getItemCount(); i++) {
			String name = c_kernelCombo.getItem(i);
			if(!name.equals(NO_SPEC) && !name.equals(NO_SPEC_AVAILABLE)) {
				prefString.append(";");
				JupyterKernelSpec spec = (JupyterKernelSpec) c_kernelCombo.getData(name);
				prefString.append(spec.toPrefString());
			}
			if(name.equals(NO_SPEC)) {
				prefString.append(";");
				JupyterKernelSpec spec = new JupyterKernelSpec(NO_SPEC, "", "");
				prefString.append(spec.toPrefString());
			}
		}

        return prefString.toString(); 
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public int getNumberOfControls() {
		return 2;
	}

	/**
	 * update content based on a list of kernel specs
	 * @param kernelSpecs
	 */
	public void updateComboBoxes(List<JupyterKernelSpec> kernelSpecs) {
		
		c_kernelCombo.removeAll();	
		
		if(kernelSpecs == null) {
				
			c_kernelCombo.add(NO_SPEC_AVAILABLE);
			c_kernelCombo.select(0);
		} else {		
			int selectedIdx = c_kernelCombo.getSelectionIndex();
	
			String spec = null;
			if(selectedIdx >= 0) spec = c_kernelCombo.getItem(selectedIdx);
					
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

}
