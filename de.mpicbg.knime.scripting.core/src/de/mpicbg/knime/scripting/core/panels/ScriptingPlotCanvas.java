package de.mpicbg.knime.scripting.core.panels;

import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.geom.AffineTransform;
import java.awt.image.AffineTransformOp;
import java.awt.image.BufferedImage;

import javax.swing.JPanel;


@SuppressWarnings("serial")
public abstract class ScriptingPlotCanvas<NodeModel> extends JPanel {
	
	// it is necessary to keep both images,
	// as during resizing the rescaling should happen of the base image
	protected BufferedImage m_baseImage;
    protected BufferedImage m_scaledImage;
    
    protected NodeModel m_plotModel;
	
	protected void rescaleImage(int width, int height) {

        BufferedImage bufImage = new BufferedImage(getWidth(), getHeight(), BufferedImage.TYPE_INT_RGB);
        bufImage.createGraphics();
        AffineTransform at = AffineTransform.getScaleInstance((double) getWidth() / m_baseImage.getWidth(null),
                (double) getHeight() / m_baseImage.getHeight(null));

        AffineTransformOp op = new AffineTransformOp(at, AffineTransformOp.TYPE_BILINEAR);
        m_scaledImage = op.filter(m_baseImage, null);
	}
	
	protected void recreateImage(int width, int height) {
		m_baseImage = recreateImageImpl(width, height);
		m_scaledImage = null;
	}
	
	protected abstract BufferedImage recreateImageImpl(int width, int height);
	
	protected NodeModel getNodeModel() {
		return m_plotModel;
	}
	
	protected void setBaseImage(BufferedImage img) {
		m_baseImage = img;
	}
	
	protected void setRescaledImage(BufferedImage img) {
		m_scaledImage = img;
	}
	
	protected abstract Dimension getPlotDimensionsFromModel();
	
	protected abstract BufferedImage getBaseImageFromModel();
	
	public void paint(Graphics g) {
        g.drawImage(m_scaledImage != null ? m_scaledImage : m_baseImage, 0, 0, null);
    }
	
}
