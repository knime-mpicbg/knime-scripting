package de.mpicbg.tds.knime.scripting.python.plots;

import de.mpicbg.tds.knime.knutils.scripting.ImageClipper;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.geom.AffineTransform;
import java.awt.image.AffineTransformOp;
import java.awt.image.BufferedImage;
import java.awt.image.PixelGrabber;


/**
 * A renderer which allows to display matlab plots. It automatically adapts to the panel size by replotting the figures on
 * resize.
 *
 * @author Holger Brandl
 */
public class PythonPlotCanvas extends JPanel {
    private BufferedImage baseImage;
    private BufferedImage scaledImage;
    private PythonPlotNodeModel plotModel;

    private boolean isReCreatingImage = false;

    public PythonPlotCanvas(PythonPlotNodeModel plotModel) {
        setFocusable(true);
        setPreferredSize(new Dimension(plotModel.getDefWidth(), plotModel.getDefHeight()));

        this.plotModel = plotModel;

        baseImage = toBufferedImage(plotModel.getImage());

        addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                if (!isVisible()) {
                    return;
                }

                BufferedImage bufImage = new BufferedImage(getWidth(), getHeight(), BufferedImage.TYPE_INT_RGB);
                Graphics2D g = bufImage.createGraphics();
                AffineTransform at = AffineTransform.getScaleInstance((double) getWidth() / baseImage.getWidth(null),
                        (double) getHeight() / baseImage.getHeight(null));

                AffineTransformOp op = new AffineTransformOp(at, AffineTransformOp.TYPE_BILINEAR);
                scaledImage = op.filter(baseImage, null);
            }
        });

        // add clipboard copy paste
        addKeyListener(new KeyAdapter() {

            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_C && e.isMetaDown())
                    new ImageClipper().copyToClipboard(PythonPlotCanvas.this.baseImage);
            }
        });


    }


//    public void recreateImage() {
//        RConnection connection = null;
//        try {
//            connection = RUtils.createConnection();
//
//            RUtils.loadGenericInputs(Collections.singletonMap(R_INVAR_BASE_NAME, plotModel.getWSFile()), connection);
//
//            String script = plotModel.prepareScript();
//
//            Image image = RImageFactory.createImage(connection, script, getWidth(), getHeight(), plotModel.getDevice());
//
//            connection.close();
//
//            baseImage = toBufferedImage(image);
//            scaledImage = null;
//
//        } catch (Throwable e1) {
//            if (connection != null) {
//                connection.close();
//            }
//            throw new RuntimeException(e1);
//        }
//    }


    public void paint(Graphics g) {
        g.drawImage(scaledImage != null ? scaledImage : baseImage, 0, 0, null);
    }


    public static BufferedImage toBufferedImage(Image image) {

        if (image instanceof BufferedImage) {
            return (BufferedImage) image;
        }

        // This code ensures that all the pixels in the image are loaded
        image = new ImageIcon(image).getImage();

        // Determine if the image has transparent pixels
        boolean hasAlpha = hasAlpha(image);

        // Create a buffered image with a format that's compatible with the
        // screen
        BufferedImage bimage = null;
        GraphicsEnvironment ge = GraphicsEnvironment
                .getLocalGraphicsEnvironment();
        try {
            // Determine the type of transparency of the new buffered image
            int transparency = Transparency.OPAQUE;
            if (hasAlpha == true) {
                transparency = Transparency.BITMASK;
            }

            // Create the buffered image
            GraphicsDevice gs = ge.getDefaultScreenDevice();
            GraphicsConfiguration gc = gs.getDefaultConfiguration();
            bimage = gc.createCompatibleImage(image.getWidth(null), image
                    .getHeight(null), transparency);
        } catch (HeadlessException e) {
        } // No screen

        if (bimage == null) {
            // Create a buffered image using the default color model
            int type = BufferedImage.TYPE_INT_RGB;
            if (hasAlpha == true) {
                type = BufferedImage.TYPE_INT_ARGB;
            }
            bimage = new BufferedImage(image.getWidth(null), image
                    .getHeight(null), type);
        }

        // Copy image to buffered image
        Graphics g = bimage.createGraphics();

        // Paint the image onto the buffered image
        g.drawImage(image, 0, 0, null);
        g.dispose();

        return bimage;
    }


    public static boolean hasAlpha(Image image) {
        // If buffered image, the color model is readily available
        if (image instanceof BufferedImage) {
            return ((BufferedImage) image).getColorModel().hasAlpha();
        }

        // Use a pixel grabber to retrieve the image's color model;
        // grabbing a single pixel is usually sufficient
        PixelGrabber pg = new PixelGrabber(image, 0, 0, 1, 1, false);
        try {
            pg.grabPixels();
        } catch (InterruptedException e) {
        }

        // Get the image's color model
        return pg.getColorModel().hasAlpha();
    }
}
