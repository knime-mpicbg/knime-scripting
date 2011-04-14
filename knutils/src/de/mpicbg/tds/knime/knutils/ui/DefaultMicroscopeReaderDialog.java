package de.mpicbg.tds.knime.knutils.ui;

import de.mpicbg.tds.knime.knutils.AbstractConfigDialog;
import org.knime.core.data.DataTableSpec;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NotConfigurableException;
import org.knime.core.node.defaultnodesettings.SettingsModelString;

import javax.swing.filechooser.FileNameExtensionFilter;


/**
 * Document me!
 *
 * @author Holger Brandl
 */
public class DefaultMicroscopeReaderDialog extends AbstractConfigDialog {

    FileSelectPanel fileSelectionPanel;


    public SettingsModelString fileChooserProperty;
    public String fileNameFilterDesc;
    private String[] fileNameFilterSuffixes;


    public DefaultMicroscopeReaderDialog(String fileNameFilterDesc, String... fileNameFilterSuffixes) {
        super(null);

        this.fileNameFilterSuffixes = fileNameFilterSuffixes;
        this.fileNameFilterDesc = fileNameFilterDesc;

        createControls();
    }


    public void setFileNameFilterSuffixes(String[] fileNameFilterSuffix) {
        this.fileNameFilterSuffixes = fileNameFilterSuffix;
        fileSelectionPanel.setExtensionFilter(createExtensionFilter());
    }


    @Override
    public void createControls() {
        fileChooserProperty = createFileChooser();
//          fileChooserProperty.addChangeListener(new ChangeListener() {
//                    public void stateChanged(ChangeEvent changeEvent) {
//
//                        refetchColheader();
//                    }
//                });

        registerProperty(fileChooserProperty);
        fileSelectionPanel = new FileSelectPanel(fileChooserProperty, createExtensionFilter());
        removeOptionsTab();
        addTab("Input Files", fileSelectionPanel);
    }


    private FileNameExtensionFilter createExtensionFilter() {
        return new FileNameExtensionFilter(fileNameFilterDesc, fileNameFilterSuffixes);
    }


    protected void removeOptionsTab() {
        removeTab("Options");
    }


    private void refetchColheader() {

//                Sheet sheet = ExcelReader.openWorkSheet(new File(fileChooserProperty.getStringValue()), workSheetProperty.getIntValue());
//                DataColumnSpec[] columnSpecs = AttributeUtils.compileSpecs(ExcelReader.readHeader(sheet, null));
//
//                List<String> names = new ArrayList<String>();
//                for (int i = 0; i < columnSpecs.length; i++) {
//                    names.add(columnSpecs[i].getName());
//                }
//
//                importColumWidget.replaceListItems(names);
    }


    @Override
    public void loadAdditionalSettingsFrom(NodeSettingsRO settings, DataTableSpec[] specs) throws NotConfigurableException {
        super.loadAdditionalSettingsFrom(settings, specs);

        fileSelectionPanel.updateListView();
    }


    public static SettingsModelString createFileChooser() {
        return new SettingsModelString("input.files", "");
    }
}
