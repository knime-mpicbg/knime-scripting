package de.mpicbg.knime.scripting.python.v2.plots;

import java.awt.Dimension;
import java.awt.image.BufferedImage;
import java.io.IOException;

import org.knime.core.node.NodeModel;

import de.mpicbg.knime.scripting.core.exceptions.KnimeScriptingException;
import de.mpicbg.knime.scripting.core.panels.ScriptingPlotCanvas;


/**
 * A renderer which allows to display Python plots. 
 *
 * @author Antje Janosch
 */
@SuppressWarnings("serial")
public class PythonPlotCanvasV2 extends ScriptingPlotCanvas<NodeModel> {
	
    private AbstractPythonPlotV2NodeModel m_plotModel;
    
    public PythonPlotCanvasV2(AbstractPythonPlotV2NodeModel plotModel) {
    	
    	this.m_plotModel = plotModel;
    }



    /*public static BufferedImage toBufferedImage(Image image) {

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
    }*/

	@Override
	protected BufferedImage recreateImageImpl(int width, int height) {
		try {
			return m_plotModel.getRecreatedImage(width, height);
		} catch (IOException | KnimeScriptingException e) {
			e.printStackTrace();
		}
		return null;
	}

	@Override
	protected Dimension getPlotDimensionsFromModel() {
		return new Dimension(m_plotModel.getConfigWidth(), m_plotModel.getConfigHeight());
	}

	@Override
	protected BufferedImage getBaseImageFromModel() {
		try {
			return m_plotModel.getImage();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		return null;
	}


}
