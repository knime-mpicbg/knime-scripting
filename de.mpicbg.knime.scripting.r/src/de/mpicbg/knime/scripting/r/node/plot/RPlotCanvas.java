package de.mpicbg.knime.scripting.r.node.plot;

import de.mpicbg.knime.scripting.core.ImageClipper;
import de.mpicbg.knime.scripting.r.R4KnimeBundleActivator;
import de.mpicbg.knime.scripting.r.RUtils;
import de.mpicbg.knime.scripting.r.plots.AbstractRPlotNodeModel;
import de.mpicbg.knime.scripting.r.prefs.RPreferenceInitializer;

import org.rosuda.REngine.Rserve.RConnection;

import javax.swing.*;

import static de.mpicbg.knime.scripting.r.node.snippet.RSnippetNodeModel.R_INVAR_BASE_NAME;

import java.awt.*;
import java.awt.event.*;
import java.awt.geom.AffineTransform;
import java.awt.image.AffineTransformOp;
import java.awt.image.BufferedImage;
import java.awt.image.PixelGrabber;
import java.util.Collections;


/**
 * A Rm-renderer which allows to display r-plots. It automatically adapts to the panel size by replotting the figures on
 * resize.
 *
 * @author Holger Brandl
 */
public class RPlotCanvas extends JPanel {


    private BufferedImage baseImage;
    private BufferedImage scaledImage;
    private AbstractRPlotNodeModel plotModel;

    private boolean isReCreatingImage = false;


    public RPlotCanvas(AbstractRPlotNodeModel plotModel) {
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

                boolean repaintOnResize = R4KnimeBundleActivator.getDefault().getPreferenceStore().getBoolean(RPreferenceInitializer.REPAINT_ON_RESIZE);
                if (repaintOnResize) {
                    recreateImage();

                } else {
                    BufferedImage bufImage = new BufferedImage(getWidth(), getHeight(), BufferedImage.TYPE_INT_RGB);
                    Graphics2D g = bufImage.createGraphics();
                    AffineTransform at = AffineTransform.getScaleInstance((double) getWidth() / baseImage.getWidth(null),
                            (double) getHeight() / baseImage.getHeight(null));

                    AffineTransformOp op = new AffineTransformOp(at, AffineTransformOp.TYPE_BILINEAR);
                    scaledImage = op.filter(baseImage, null);
                }
            }
        });

        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent mouseEvent) {
                if (mouseEvent.getClickCount() == 2 && !isReCreatingImage) {
                    isReCreatingImage = true;
                    recreateImage();
                    isReCreatingImage = false;

                    invalidate();
                    repaint();
                }
            }
        });

        // add clipboard copy paste
        addKeyListener(new KeyAdapter() {

            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_C && e.isMetaDown())
                    new ImageClipper().copyToClipboard(RPlotCanvas.this.baseImage);

//                    if (e.getKeyCode() == KeyEvent.VK_S && e.isControlDown())
//                        showSaveImageDialog();
            }
        });

    }


    public void recreateImage() {
        RConnection connection = null;
        try {
            connection = RUtils.createConnection();

            RUtils.loadGenericInputs(Collections.singletonMap(R_INVAR_BASE_NAME, plotModel.getWSFile()), connection);

            String script = plotModel.prepareScript();

            Image image = RUtils.createImage(connection, script, getWidth(), getHeight(), plotModel.getDevice());

            connection.close();

            baseImage = toBufferedImage(image);
            scaledImage = null;

        } catch (Exception e1) {
            if (connection != null) {
                connection.close();
            }
            throw new RuntimeException(e1);
        }
    }


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
