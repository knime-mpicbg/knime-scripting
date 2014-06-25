package de.mpicbg.knime.scripting.core.utils;

import de.mpicbg.knime.scripting.core.ScriptTemplateFile;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by IntelliJ IDEA.
 * User: Antje Niederlein
 * Date: 12/14/11
 * Time: 1:21 PM
 */
public class CreateTemplateFile {

    private JFileChooser parentDir;
    File selectedFile = null;
    File destFile = null;
    JFrame frame;
    List<File> templateFiles = new ArrayList<File>();
    List<File> pngFiles = new ArrayList<File>();

    public CreateTemplateFile(String openDir) {
        parentDir = new JFileChooser(openDir);

        frame = new JFrame("");
        JButton button = new JButton("Open File");

        button.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                templateFiles.clear();
                pngFiles.clear();
                selectedFile = null;
                destFile = null;

                showOpenDialog();
                if (selectedFile != null) {
                    readSubdirectories();
                    showSaveDialog();
                    if (destFile != null) {
                        saveTemplates();
                        savePngs();
                    }
                }
            }
        });

        frame.getContentPane().add(button);
        frame.pack();
        frame.setVisible(true);
    }

    private void savePngs() {
        if (templateFiles.size() > 0) {
            try {
                String destDir = destFile.getParent();
                for (File png : pngFiles) {
                    FileInputStream fIn = new FileInputStream(png.getAbsoluteFile());
                    FileOutputStream fOut = new FileOutputStream(destDir + "/" + png.getName());

                    byte[] buffer = new byte[0xFFFF];
                    int len;
                    while ((len = fIn.read(buffer)) != -1) {
                        fOut.write(buffer, 0, len);
                    }
                    fIn.close();
                    fOut.flush();
                    fOut.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void saveTemplates() {
        if (templateFiles.size() > 0) {
            try {
                BufferedOutputStream bufOut = new BufferedOutputStream(new FileOutputStream(destFile.getAbsoluteFile()));
                for (File template : templateFiles) {

                    BufferedInputStream bufRead = new BufferedInputStream(new FileInputStream(template.getAbsoluteFile()));

                    while (bufRead.available() > 0) {
                        bufOut.write(bufRead.read());
                    }
                    bufOut.write('\n');
                    bufRead.close();
                }
                bufOut.flush();
                bufOut.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void showSaveDialog() {
        parentDir.setFileSelectionMode(JFileChooser.FILES_ONLY);
        parentDir.showSaveDialog(frame);
        destFile = parentDir.getSelectedFile();
    }

    private void readSubdirectories() {
        System.out.println("find templates in " + selectedFile.getAbsoluteFile());

        fillTemplateList(selectedFile);
        System.out.println("Found " + templateFiles.size() + " files");
    }

    private void fillTemplateList(File selectedFile) {
        File[] fileList = selectedFile.listFiles();

        if (fileList == null) return;
        for (File curFile : fileList) {
            if (curFile.isDirectory()) fillTemplateList(curFile);
            else {
                if (curFile.getName().endsWith(".txt")) {
                    ScriptTemplateFile templateFile = new ScriptTemplateFile("file:" + curFile.getPath());
                    if (!templateFile.isEmpty()) templateFiles.add(curFile);
                }
                if (curFile.getName().endsWith(".png")) pngFiles.add(curFile);
            }
        }
    }

    private void showOpenDialog() {
        parentDir.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        parentDir.showOpenDialog(frame);
        selectedFile = parentDir.getSelectedFile();
    }


    public static void main(String[] args) {
        CreateTemplateFile cf = new CreateTemplateFile(args[0]);
    }
}
