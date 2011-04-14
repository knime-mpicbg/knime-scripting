/*
 * Created by JFormDesigner on Wed Mar 17 09:42:52 CET 2010
 */

package de.mpicbg.tds.knime.knutils.ui;

import de.mpicbg.tds.knime.knutils.FileUtils;
import org.knime.core.node.defaultnodesettings.SettingsModelString;

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;


/**
 * @author Holger Brandl
 */
public class FileSelectPanel extends JPanel {

    private SettingsModelString fileChooserProperty;
    public FileNameExtensionFilter extensionFilter;


    public FileSelectPanel(SettingsModelString fileChooserProperty, FileNameExtensionFilter fileFilter) {
        this.fileChooserProperty = fileChooserProperty;
        setExtensionFilter(fileFilter);

        initComponents();
    }


    public void setExtensionFilter(FileNameExtensionFilter extensionFilter) {
        this.extensionFilter = extensionFilter;
    }


    private void selectFilesButtonActionPerformed() {

        // go back to the old directory
//        File oldFile = new File(fileChooserProperty.getStringValue());

        JFileChooser fileChooser;

//        if (oldFile.exists()) {
//            fileChooser = new JFileChooser(oldFile.getParentFile());
//        } else {
        fileChooser = new JFileChooser();
//        }

        // take the first and use it as start-directoruy
        List<File> inputFiles = FileSelectPanel.getInputFilesInternal(fileChooserProperty.getStringValue(), extensionFilter.getExtensions());
        if (inputFiles.size() > 0) {
            File firstFile = inputFiles.get(0);
            if (firstFile.exists()) {
                fileChooser.setCurrentDirectory(firstFile.getParentFile());
            }
        }

        fileChooser.setFileFilter(extensionFilter);
        fileChooser.setMultiSelectionEnabled(true);
        fileChooser.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);

        int status = fileChooser.showOpenDialog(this);

        if (status == JFileChooser.CANCEL_OPTION)
            return;

        List<File> selectedFiles = Arrays.asList(fileChooser.getSelectedFiles());

        String selectionString = selectedFiles.toString();
        selectionString = selectionString.replace("[", "").replace("]", "").replace(",", ";");

        fileChooserProperty.setStringValue(selectionString);

        updateListView();
    }


    public void updateListView() {
        String fileChooserValue = fileChooserProperty.getStringValue();

        if (!fileChooserValue.contains(";") && new File(fileChooserValue).isDirectory()) {
            selectionMode.setText("Directory ('" + fileChooserValue + "')");
        } else {
            selectionMode.setText("Multiple files");
        }

        List<File> inputFiles = FileSelectPanel.getInputFiles(fileChooserProperty.getStringValue(), extensionFilter.getExtensions());
        StringBuffer sb = new StringBuffer();
        for (File inputFile : inputFiles) {
            sb.append(inputFile.getAbsolutePath() + "\n");
        }

        fileListArea.setText(sb.toString());
        invalidate();
    }


    public static List<File> parseFileSelection(String serializedFileCollection) {
        List<File> files = new ArrayList<File>();

        if (serializedFileCollection == null || serializedFileCollection.trim().isEmpty())
            return files;

        for (String fileName : serializedFileCollection.split(";")) {
            files.add(new File(fileName.trim()));
        }

        return files;
    }


    private void initComponents() {
        // JFormDesigner - Component initialization - DO NOT MODIFY  //GEN-BEGIN:initComponents
        // Generated using JFormDesigner Open Source Project license - Sphinx-4 (cmusphinx.sourceforge.net/sphinx4/)
        selectFilesButton = new JButton();
        scrollPane1 = new JScrollPane();
        fileListArea = new JTextArea();
        panel1 = new JPanel();
        label1 = new JLabel();
        selectionMode = new JLabel();

        //======== this ========
        setLayout(new BorderLayout());

        //---- selectFilesButton ----
        selectFilesButton.setText("Select Files or Directory");
        selectFilesButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                selectFilesButtonActionPerformed();
            }
        });
        add(selectFilesButton, BorderLayout.NORTH);

        //======== scrollPane1 ========
        {

            //---- fileListArea ----
            fileListArea.setEditable(false);
            fileListArea.setEnabled(false);
            fileListArea.setPreferredSize(new Dimension(200, 200));
            scrollPane1.setViewportView(fileListArea);
        }
        add(scrollPane1, BorderLayout.CENTER);

        //======== panel1 ========
        {
            panel1.setLayout(new BorderLayout());

            //---- label1 ----
            label1.setText("Mode:  ");
            panel1.add(label1, BorderLayout.WEST);

            //---- selectionMode ----
            selectionMode.setText("text");
            panel1.add(selectionMode, BorderLayout.CENTER);
        }
        add(panel1, BorderLayout.SOUTH);
        // JFormDesigner - End of component initialization  //GEN-END:initComponents
    }

    // JFormDesigner - Variables declaration - DO NOT MODIFY  //GEN-BEGIN:variables
    // Generated using JFormDesigner Open Source Project license - Sphinx-4 (cmusphinx.sourceforge.net/sphinx4/)
    private JButton selectFilesButton;
    private JScrollPane scrollPane1;
    private JTextArea fileListArea;
    private JPanel panel1;
    private JLabel label1;
    private JLabel selectionMode;
    // JFormDesigner - End of variables declaration  //GEN-END:variables


    public static void main(String[] args) {
        JFrame jFrame = new JFrame();
        jFrame.add(new FileSelectPanel(new SettingsModelString("test", ""), new FileNameExtensionFilter("text files", "xls")));

        jFrame.setBounds(200, 200, 500, 500);
        jFrame.setVisible(true);
    }


    public static List<File> getInputFiles(String serializedFileSelection, String... extensions) {
        List<File> filteredFiles = getInputFilesInternal(serializedFileSelection, extensions);

        if (filteredFiles.isEmpty()) {
            throw new RuntimeException("no " + Arrays.toString(extensions) + "-files found in input selection");
        }

        return filteredFiles;
    }


    private static List<File> getInputFilesInternal(String serializedFileSelection, String[] extensions) {
        List<File> inputSelection = parseFileSelection(serializedFileSelection);

        List<File> foundFiles = new ArrayList<File>();

        for (File inputArg : inputSelection) {

            if (inputArg.isDirectory()) {
                foundFiles.addAll(FileUtils.findFiles(inputArg));
            } else {
                foundFiles.add(inputArg);
            }
        }


        //filter the list
        List<File> filteredFiles = new ArrayList<File>();
        for (File foundFile : foundFiles) {
            for (String extension : extensions) {
                if (foundFile.getName().endsWith(extension)) {
                    filteredFiles.add(foundFile);
                    break;
                }
            }
        }

        return filteredFiles;
    }


}
