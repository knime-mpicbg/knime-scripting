package de.mpicbg.knime.scripting.core.panels;

import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.geom.AffineTransform;
import java.awt.image.AffineTransformOp;
import java.awt.image.BufferedImage;

import javax.swing.JPanel;

/**
 * Abstract class for panel which holds the plot image
 * 
 * @author Antje Janosch
 *
 * @param <NodeModel>
 */
@SuppressWarnings("serial")
public abstract class ScriptingPlotCanvas<NodeModel> extends JPanel {
	
	// it is necessary to keep both images,
	// as during resizing the rescaling should happen of the base image
	/** image how it has been created by the node */
	protected BufferedImage m_baseImage;
	/** image after being recreated due to resized view window */
    protected BufferedImage m_scaledImage;
    /** node model */
    protected NodeModel m_plotModel;
	
    /**
     * affine transform of the image with a given new dimensions
     * 
     * @param width
     * @param height
     */
	protected void rescaleImage(int width, int height) {

        BufferedImage bufImage = new BufferedImage(getWidth(), getHeight(), BufferedImage.TYPE_INT_RGB);
        bufImage.createGraphics();
        AffineTransform at = AffineTransform.getScaleInstance((double) getWidth() / m_baseImage.getWidth(null),
                (double) getHeight() / m_baseImage.getHeight(null));

        AffineTransformOp op = new AffineTransformOp(at, AffineTransformOp.TYPE_BILINEAR);
        m_scaledImage = op.filter(m_baseImage, null);
	}
	
	/**
	 * recreate image with given dimensions
	 * 
	 * @param width
	 * @param height
	 */
	protected void recreateImage(int width, int height) {
		m_baseImage = recreateImageImpl(width, height);
		m_scaledImage = null;
	}
	
	/**
	 * draws either the base image of the scaled image
	 */
	public void paint(Graphics g) {
        g.drawImage(m_scaledImage != null ? m_scaledImage : m_baseImage, 0, 0, null);
    }
	
	/**
	 * @return underlying {@link org.knime.core.node.NodeModel}
	 */
	protected NodeModel getNodeModel() {
		return m_plotModel;
	}
	
	/**
	 * set base image
	 * @param img
	 */
	protected void setBaseImage(BufferedImage img) {
		m_baseImage = img;
	}
	
	/** 
	 * set rescaled image
	 * @param img
	 */
	protected void setRescaledImage(BufferedImage img) {
		m_scaledImage = img;
	}
	
	/**
	 * following methods needs to be implemented for the various scripting extensions
	 */
	
	/**
	 * @param width
	 * @param height
	 * @return recreated image
	 */
	protected abstract BufferedImage recreateImageImpl(int width, int height);
	
	/**
	 * @return dimensions of image created by the node
	 */
	protected abstract Dimension getPlotDimensionsFromModel();
	
	/**
	 * @return image created by the node
	 */
	protected abstract BufferedImage getBaseImageFromModel();
	
	
	
}
