package de.mpicbg.knime.scripting.core.prefs;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;

import org.eclipse.jface.preference.FieldEditor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.swt.widgets.Text;

import de.mpicbg.knime.scripting.core.TemplateCache;
import de.mpicbg.knime.scripting.core.utils.ScriptingUtils;


/**
 * Created by IntelliJ IDEA.
 * User: niederle
 * Date: 6/28/11
 * Time: 8:34 AM
 * <p/>
 * This class implements a table with checkboxes as a preference page element
 */
public class TemplateTableEditor extends FieldEditor {

    private Composite top;
    private Composite group;
    private Table templateTable;
    private Text templateField;
    private Button browseUrl;
    private Button addUrl;
    private Button removeURL;

    private List<String> nonValidChars;

    private List<TemplatePref> m_templateList;
    private List<TemplatePref> m_initialTemplateList = new ArrayList<TemplatePref>();

    private static Color gray;
    private static Color black;
    
    private Path cacheFolder;
    private Path indexFile;

    /**
     * constructor of preference field holding template files
     * 
     * @param name
     * @param labelText
     * @param bundlePath
     * @param parent
     */
    public TemplateTableEditor(String name, String labelText, String bundlePath, Composite parent) {
        super(name, labelText, parent);

        nonValidChars = new ArrayList<String>();
        nonValidChars.add(new String(","));
        nonValidChars.add(new String(";"));

        gray = Display.getCurrent().getSystemColor(SWT.COLOR_GRAY);
        black = Display.getCurrent().getSystemColor(SWT.COLOR_BLACK);
        
        this.cacheFolder = Paths.get(bundlePath, ScriptingUtils.LOCAL_CACHE_FOLDER);
        this.indexFile = Paths.get(bundlePath, ScriptingUtils.LOCAL_CACHE_FOLDER, ScriptingUtils.LOCAL_CACHE_INDEX);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void adjustForNumColumns(int numColumns) {
        ((GridData) top.getLayoutData()).horizontalSpan = numColumns;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void doFillIntoGrid(Composite parent, int numColumns) {
        top = parent;
        top.setLayoutData(getMainGridData(numColumns));

        group = new Composite(top, SWT.BORDER);

        GridLayout newgd = new GridLayout(3, false);
        group.setLayout(newgd);
        group.setLayoutData(getMainGridData(numColumns));

        // set label
        Label label = getLabelControl(group);
        GridData labelData = new GridData();
        labelData.horizontalSpan = numColumns;
        label.setLayoutData(labelData);

        // url table
        templateTable = new Table(group, SWT.BORDER | SWT.CHECK | SWT.MULTI | SWT.FULL_SELECTION | SWT.H_SCROLL | SWT.V_SCROLL);
        templateTable.setHeaderVisible(true);
        //int desiredHeight = templateTable.getItemHeight() * 4 + templateTable.getHeaderHeight();
        GridData gd = new GridData(SWT.FILL, SWT.FILL, true, true);
        gd.horizontalSpan = 2;
        templateTable.setLayoutData(gd);
        templateTable.addListener(SWT.Selection, new Listener() {
            public void handleEvent(Event event) {
                if (event.detail != SWT.CHECK) return;
                checkBoxClicked(event);
            }
        });

        // set column for template url
        TableColumn templateUri = new TableColumn(templateTable, SWT.LEFT);
        templateUri.setText("Template URL (only enabled urls will be loaded)");
        templateUri.setWidth(600);

        removeURL = new Button(group, SWT.PUSH);
        removeURL.setLayoutData(new GridData(SWT.LEFT, SWT.TOP, false, false));
        removeURL.setText("remove");
        removeURL.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                removeURI();
            }
        });

        templateField = new Text(group, SWT.BORDER);
        templateField.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));

        browseUrl = new Button(group, SWT.PUSH);
        browseUrl.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, false, false));
        browseUrl.setText("browse...");
        browseUrl.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                browseForFile();
            }
        });

        addUrl = new Button(group, SWT.PUSH);
        addUrl.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false));
        addUrl.setText("add");
        addUrl.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                addURI(templateField.getText());
            }
        });

        Label emptyLabel = new Label(top, SWT.NONE);
        emptyLabel.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, false, 3, 1));
    }

    /**
     * Event handling method to make template active or inactive
     *
     * @param event
     */
    private void checkBoxClicked(Event event) {
        TableItem checkedItem = (TableItem) event.item;
        boolean isChecked = checkedItem.getChecked();

        // set text color and deselect row
        if (isChecked) checkedItem.setForeground(black);
        else checkedItem.setForeground(gray);

        int tIdx = templateTable.indexOf(checkedItem);
        templateTable.deselect(tIdx);

        // update template list
        for (Iterator<TemplatePref> iterator = m_templateList.iterator(); iterator.hasNext(); ) {
            TemplatePref tPref = iterator.next();
            if (tPref.getUri().equals(templateTable.getItem(tIdx).getText(0))) {
                tPref.setActive(isChecked);
            }
        }
        
        fillTable();
    }

    /**
     * action event if "browse..." button has been clicked <br/>
     * file dialog shows up and allows to select a file
     */
    private void browseForFile() {
        FileDialog dialog = new FileDialog(group.getShell(), SWT.OPEN);
        String newURI = dialog.open();

        if (newURI != null) {
            File f = new File(newURI);
            String urlString;
            try {
                urlString = f.toURI().toURL().toString();
            } catch (MalformedURLException e) {
                return;
            }
            addURI(urlString);
        }
    }

    /**
     * remove uri(s) from template list
     */
    private void removeURI() {
        int[] tIdx = templateTable.getSelectionIndices();

        if (tIdx.length == 0) return;

        HashSet<TemplatePref> toRemove = new HashSet<TemplatePref>();

        for (int i = 0; i < tIdx.length; i++) {
        	for(TemplatePref tPref : m_templateList) {
            //for (Iterator<TemplatePref> iterator = templateList.iterator(); iterator.hasNext(); ) {
                //TemplatePref tPref = iterator.next();
                if (tPref.getUri().equals(templateTable.getItem(tIdx[i]).getText(0))) {
                    toRemove.add(tPref);
                    //removeFileFromCache(tPref.getUri());
                }
            }
        }

        m_templateList.removeAll(toRemove);
        fillTable();
    }

    /**
     * after "add" or "browse" the new file needs to be added to the template list <br/>
     * some checks performed before <br/>
     * file table will be updated
     * 
     * @param newURI
     */
    private void addURI(String newURI) {
    	
        try {
            newURI = validateURI(newURI);

        } catch (IOException e) {
            MessageBox messageDialog = new MessageBox(group.getShell(), SWT.ERROR);
            messageDialog.setText("Exception");
            messageDialog.setMessage("This is not a valid file location\n\n" + e.getMessage());
            messageDialog.open();
            return;
        }

        TemplatePref newTemplate = new TemplatePref();

        newTemplate.setUri(newURI);
        newTemplate.setActive(true);

        m_templateList.add(newTemplate);

        fillTable();
    }

    /**
     * checks if the given string is a valid URL <br/>
     * correct the format for local files<br/>
     * checks for duplicates<br/>
     * checks for nonvalid characters
     *
     * @param newURI		uri string to be checked
     * @return				uri string (adapted to file:/ for local files)
     * @throws IOException	if checks fail
     */
    private String validateURI(String newURI) throws IOException {
    	
    	// validate for URL format
        try {
        	new URL(newURI);
        } catch(MalformedURLException mue) {
        	// might be a local file path; if yes convert to URL format file://
        	File f = new File(newURI);
        	if(f.canRead())
        		newURI = f.toURI().toURL().toExternalForm();
        }

        // check if uri is already listed
        for (TemplatePref tPref : m_templateList) {
            if (tPref.getUri().equals(newURI)) {
                throw new IOException("Template source is already listed");
            }
        }

        // check if the new uri contains nonvalid characters
        for (String nvChar : nonValidChars) {
            if (newURI.contains(nvChar)) {
                throw new IOException("nonvalid characters");
            }
        }
        
        return newURI;
    }

    private GridData getMainGridData(int numColumns) {
        GridData gd = new GridData(SWT.FILL, SWT.TOP, true, false);
        gd.horizontalSpan = numColumns;

        return gd;
    }

    /**
     * update graphical representation
     */
    private void fillTable() {
        templateTable.removeAll();

        if (m_templateList.isEmpty()) return;

        for (TemplatePref tPref : m_templateList) {
            TableItem tItem = new TableItem(templateTable, SWT.NONE);
            tItem.setText(tPref.getUri());
            tItem.setChecked(tPref.isActive());

            if (tPref.isActive()) tItem.setForeground(black);
            else tItem.setForeground(gray);
        }
    }


    
    private void loadPreferencesFromString(String prefString) {
    	TemplatePrefString tString = new TemplatePrefString(prefString);
        m_templateList = tString.parsePrefString();
        fillTable();       
    }
    
    /*
     * (non-Javadoc)
     * @see org.eclipse.jface.preference.FieldEditor#doLoad()
     */
    @Override
    protected void doLoad() {
        String items = getPreferenceStore().getString(getPreferenceName());       
        loadPreferencesFromString(items);   
        // create a copy of settings after load
        m_initialTemplateList.addAll(m_templateList);
    }

    @Override
    protected void doLoadDefault() {
        String items = getPreferenceStore().getDefaultString(getPreferenceName());
        loadPreferencesFromString(items);
        // needs to be set to false, otherwise doStore is not called ?
        setPresentsDefaultValue(false);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void doStore() {
    	
    	/*
    	 * what could have happened:
    	 * 1) file has been added => store to cache
    	 * 2) file has been deactivated => remove from cache (but not local version)
    	 * 3) file has been activated => add to cache (save local if not yet there)
    	 * 4) file has been removed => remove from cache and local version
    	 */
    	
    	TemplateCache templateCache = TemplateCache.getInstance();
    	
    	// check for new templates in template list (compare with initial prefs)
    	for(TemplatePref tPref : m_templateList) {
    		if(!m_initialTemplateList.contains(tPref)) {
    			// only add to cache if active
    			if(tPref.isActive()) {
    				try {
    					templateCache.addTemplateFile(tPref.getUri(), this.cacheFolder, this.indexFile);
    				} catch (IOException e) {
    					// if given file failed to be added to the cache remove from prefs and 
    					// notify user
    					m_templateList.remove(tPref);

    					MessageBox messageDialog = new MessageBox(group.getShell(), SWT.ERROR);
    					messageDialog.setText("Exception");
    					messageDialog.setMessage("Failed to add file to template cache\n\n" + e.getMessage());
    					messageDialog.open();
    				}
    			}
    		} else {
    			// if files has been already in cache but is now deactived, remove from cache but do not delete
    			// local version
    			if(tPref.isActive()) {
    				try {
						templateCache.addTemplateFile(tPref.getUri(), this.cacheFolder, this.indexFile);
					} catch (IOException e) {
						// if given file failed to be added to the cache deactivate again 
						// in prefs and notify user
    					int i = m_templateList.indexOf(tPref);
    					tPref.setActive(false);
    					m_templateList.set(i, tPref);

    					MessageBox messageDialog = new MessageBox(group.getShell(), SWT.ERROR);
    					messageDialog.setText("Exception");
    					messageDialog.setMessage("Failed to add activated file to template cache\n\n" + e.getMessage());
    					messageDialog.open();
					}
    			} else 
    				templateCache.removeTemplateFile(tPref.getUri());
    		}
    	}
    	
    	// check for removed templates in template list (compare initial prefs with current)
    	for(TemplatePref tPref : m_initialTemplateList) {
    		if(!m_templateList.contains(tPref)) {
    			templateCache.removeTemplateFile(tPref.getUri(), this.cacheFolder, this.indexFile);
    		}
    	}

    	// save everything to preference string
        TemplatePrefString tString = new TemplatePrefString(m_templateList);
        String s = tString.getPrefString();
        if (s != null)
            getPreferenceStore().setValue(getPreferenceName(), s);
    }


    @Override
    public int getNumberOfControls() {
        return 2;
    }
}
