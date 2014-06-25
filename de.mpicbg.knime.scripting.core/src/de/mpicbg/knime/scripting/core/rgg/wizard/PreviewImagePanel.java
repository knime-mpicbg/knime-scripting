package de.mpicbg.knime.scripting.core.rgg.wizard;

import de.mpicbg.knime.scripting.core.TemplateConfigurator;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.AffineTransform;
import java.awt.image.AffineTransformOp;
import java.awt.image.BufferedImage;


/**
 * Document me!
 *
 * @author Holger Brandl
 */
public class PreviewImagePanel extends JPanel {

    BufferedImage image;
    BufferedImage scaledImage;

    String title;


    public PreviewImagePanel() {
        addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent componentEvent) {
                if (isDisplayable())
                    rescaleImage();
            }
        });

        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseReleased(MouseEvent mouseEvent) {
                if (mouseEvent.getClickCount() == 2) {
                    JDialog jDialog = new JDialog(TemplateConfigurator.getOwnerDialog(PreviewImagePanel.this));
                    jDialog.setSize(new Dimension(image.getWidth() + 20, image.getHeight() + 20));
                    jDialog.setLayout(new BorderLayout());
                    PreviewImagePanel imagePanel = new PreviewImagePanel();
                    imagePanel.setImage(image);
                    jDialog.setTitle("Preview" + getTitle());
                    jDialog.getContentPane().add(imagePanel);

                    jDialog.setModal(true);
                    jDialog.setVisible(true);
                }
            }


        });
    }


    private void rescaleImage() {
        if (image != null && isVisible()) {
            AffineTransform at = AffineTransform.getScaleInstance((double) getWidth() / image.getWidth(null),
                    (double) getHeight() / image.getHeight(null));

            AffineTransformOp op = new AffineTransformOp(at, AffineTransformOp.TYPE_BILINEAR);
            scaledImage = op.filter(image, null);
        }
    }


    public void setImage(BufferedImage image) {
        this.image = image;
        scaledImage = null;
    }


    public void paint(Graphics g) {
        if (scaledImage == null && image != null) {
            rescaleImage();
        }

        if (scaledImage != null) {
            g.drawImage(scaledImage, 0, 0, null);
        }
    }


    private String getTitle() {
        return title != null ? ": " + title : "";
    }


    public void setTitle(String title) {
        this.title = title;
    }
}
