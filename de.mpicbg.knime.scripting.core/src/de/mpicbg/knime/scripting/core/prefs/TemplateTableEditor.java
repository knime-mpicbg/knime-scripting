package de.mpicbg.knime.scripting.core.prefs;

import de.mpicbg.knime.scripting.core.TemplateCache;

import org.eclipse.jface.preference.FieldEditor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.*;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;


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

    private List<TemplatePref> templateList;

    private static Color gray;
    private static Color black;

    public TemplateTableEditor(String name, String labelText, Composite parent) {
        super(name, labelText, parent);

        nonValidChars = new ArrayList<String>();
        nonValidChars.add(new String(","));
        nonValidChars.add(new String(";"));
        /*nonValidChars.add(new String("("));
        nonValidChars.add(new String(")"));*/

        gray = Display.getCurrent().getSystemColor(SWT.COLOR_GRAY);
        black = Display.getCurrent().getSystemColor(SWT.COLOR_BLACK);
    }

    @Override
    protected void adjustForNumColumns(int numColumns) {
        ((GridData) top.getLayoutData()).horizontalSpan = numColumns;
    }

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
        for (Iterator<TemplatePref> iterator = templateList.iterator(); iterator.hasNext(); ) {
            TemplatePref tPref = iterator.next();
            if (tPref.getUri().equals(templateTable.getItem(tIdx).getText(0))) {
                tPref.setActive(isChecked);
                if (isChecked) 
                	if(!addFileToCache(tPref.getUri())) tPref.setActive(false);
                else removeFileFromCache(tPref.getUri());
            }
        }
        
        fillTable();
    }

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

    private void removeURI() {
        int[] tIdx = templateTable.getSelectionIndices();

        if (tIdx.length == 0) return;

        HashSet<TemplatePref> toRemove = new HashSet<TemplatePref>();

        for (int i = 0; i < tIdx.length; i++) {
            for (Iterator<TemplatePref> iterator = templateList.iterator(); iterator.hasNext(); ) {
                TemplatePref tPref = iterator.next();
                if (tPref.getUri().equals(templateTable.getItem(tIdx[i]).getText(0))) {
                    toRemove.add(tPref);

                    removeFileFromCache(tPref.getUri());
                }
            }
        }

        templateList.removeAll(toRemove);

        fillTable();
    }

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

        if(addFileToCache(newURI)) {
        	TemplatePref newTemplate = new TemplatePref();

            newTemplate.setUri(newURI);
            newTemplate.setActive(true);

            templateList.add(newTemplate);
        }

        fillTable();
    }

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
        for (TemplatePref tPref : templateList) {
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

    @Override
    protected void doLoad() {
        String items = getPreferenceStore().getString(getPreferenceName());
        TemplatePrefString tString = new TemplatePrefString(items);
        templateList = tString.parsePrefString();
        fillTable();
    }

    /**
     * update graphical representation
     */
    private void fillTable() {
        templateTable.removeAll();

        if (templateList.isEmpty()) return;

        for (TemplatePref tPref : templateList) {
            TableItem tItem = new TableItem(templateTable, SWT.NONE);
            tItem.setText(tPref.getUri());
            tItem.setChecked(tPref.isActive());

            if (tPref.isActive()) tItem.setForeground(black);
            else tItem.setForeground(gray);
        }
    }

    /**
     * adds the given file to the template cache
     *
     * @param uri
     * @return true, if file was added successfully; otherwise false
     */
    private boolean addFileToCache(String uri) {
        // add script to cache
        try {
            TemplateCache templateCache = TemplateCache.getInstance();
            templateCache.getTemplateCache(uri);
        } catch (IOException e) {
            MessageBox messageDialog = new MessageBox(group.getShell(), SWT.ERROR);
            messageDialog.setText("Exception");
            messageDialog.setMessage("Failed to add file to template cache\n\n" + e.getMessage());
            messageDialog.open();
            return false;
        }
        return true;
    }

    /**
     * removes a given file from the template cache
     *
     * @param uri
     */
    private void removeFileFromCache(String uri) {
        // remove script from cache
        TemplateCache templateCache = TemplateCache.getInstance();
        if (templateCache.contains(uri)) templateCache.remove(uri);
    }

    @Override
    protected void doLoadDefault() {
        String items = getPreferenceStore().getDefaultString(getPreferenceName());
        TemplatePrefString tString = new TemplatePrefString(items);
        templateList = tString.parsePrefString();
        fillTable();
    }

    @Override
    protected void doStore() {

        TemplatePrefString tString = new TemplatePrefString(templateList);
        String s = tString.getPrefString();
        if (s != null)
            getPreferenceStore().setValue(getPreferenceName(), s);
    }

    @Override
    public int getNumberOfControls() {
        return 2;
    }
}
