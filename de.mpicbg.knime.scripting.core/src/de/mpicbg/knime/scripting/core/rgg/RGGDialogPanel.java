package de.mpicbg.knime.scripting.core.rgg;

import at.ac.arcs.rgg.RGG;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import javax.swing.*;
import java.awt.*;
import java.io.InputStream;


/**
 * A panel which contains a dialog created from an rgg-template.
 *
 * @author Holger Brandl
 */
public class RGGDialogPanel extends JPanel {

    public RGG rgg;
    private Log log = LogFactory.getLog(RGGDialogPanel.class);


    /**
     * Creates new form RGGDialog
     */
    public RGGDialogPanel(InputStream xmlStream) {

        JScrollPane rggScrollPane = new JScrollPane();
        setLayout(new BorderLayout());

        JPanel panel = initRGGPanel(xmlStream);
        rggScrollPane.setViewportView(panel);

        add(rggScrollPane, BorderLayout.CENTER);
//        rggScrollPane.setSize(panel.getWidth(), panel.getHeight() + 100);
    }


    private JPanel initRGGPanel(InputStream rggxml) {
        try {
            rgg = RGG.createInstance(rggxml);
            return rgg.buildPanel(true, false);
        } catch (Exception ex) {
            log.fatal(ex);
            ex.printStackTrace();
        }

        JPanel errorPanel = new JPanel(new BorderLayout());
        errorPanel.add(new JLabel("<html><h1>ERROR!</h1><br>The given template is not a valid rgg-xml document.</html>"), BorderLayout.CENTER);
        return errorPanel;
    }


    public String generateRScriptFromTemplate() {
        return rgg.generateRScript();
    }
}