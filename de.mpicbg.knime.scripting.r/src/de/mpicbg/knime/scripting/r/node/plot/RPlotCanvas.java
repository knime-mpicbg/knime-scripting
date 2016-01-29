package de.mpicbg.knime.scripting.r.node.plot;

import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.AffineTransform;
import java.awt.image.AffineTransformOp;
import java.awt.image.BufferedImage;

import javax.swing.JPanel;

import org.rosuda.REngine.Rserve.RConnection;

import de.mpicbg.knime.scripting.core.ImageClipper;
import de.mpicbg.knime.scripting.r.RUtils;
import de.mpicbg.knime.scripting.r.plots.AbstractRPlotNodeModel;


/**
 * A renderer which allows to display r-plots. It automatically adapts to the panel size by rescaling the figures on
 * resize. Figure will be recreated by mouse click
 *
 * @author Holger Brandl, Antje Janosch
 */
@SuppressWarnings("serial")
public class RPlotCanvas extends JPanel {

    private BufferedImage m_baseImage;
    private BufferedImage m_scaledImage;
    private AbstractRPlotNodeModel m_plotModel;

    // flag to mark the process of recreating the image
    private boolean isReCreatingImage = false;
    
    // NOTE: Failed to trigger repainting as soon as the mouse is released after resizing
    // instead: rescale while resizing and use single mouse click to recreate the plot

    /**
     * constructor
     * @param plotModel
     */
    public RPlotCanvas(AbstractRPlotNodeModel plotModel) {
        setFocusable(true);
        setPreferredSize(new Dimension(plotModel.getDefWidth(), plotModel.getDefHeight()));

        this.m_plotModel = plotModel;
        m_baseImage = plotModel.getImage();
        
        // if component resized
        addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
            	           	
                if (!isVisible()) {
                    return;
                }
                
                // scale image
                AffineTransform at = AffineTransform.getScaleInstance((double) getWidth() / m_baseImage.getWidth(null),
                		(double) getHeight() / m_baseImage.getHeight(null));

                AffineTransformOp op = new AffineTransformOp(at, AffineTransformOp.TYPE_BILINEAR);
                m_scaledImage = op.filter(m_baseImage, null);

            }
        });

        // single click
        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent mouseEvent) {
                if (!isReCreatingImage) {
                    isReCreatingImage = true;
                    recreateImage();
                    isReCreatingImage = false;

                    invalidate();
                    repaint();
                }
            }
        });

        // keys: copy => copy to clipboard
        addKeyListener(new KeyAdapter() {
        	@Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_C && e.isMetaDown())
                    new ImageClipper().copyToClipboard(RPlotCanvas.this.m_baseImage);

                //nice idea: save image from view
                //if (e.getKeyCode() == KeyEvent.VK_S && e.isControlDown())
                //showSaveImageDialog();
            }
        });

    }

    /**
     * runs R code again to recreate the image with the panel dimensions
     */
    public void recreateImage() {
        RConnection connection = null;
        try {
            connection = RUtils.createConnection();

            RUtils.loadWorkspace(m_plotModel.getWSFile(), connection);
            String script = m_plotModel.prepareScript();
            BufferedImage image = AbstractRPlotNodeModel.createImage(connection, script, getWidth(), getHeight(), m_plotModel.getDevice());

            connection.close();

            m_baseImage = image;
            m_scaledImage = null;

        } catch (Exception e1) {
            if (connection != null) {
                connection.close();
            }
            throw new RuntimeException(e1);
        }
    }

    /**
     * draw image
     */
    public void paint(Graphics g) {
        g.drawImage(m_scaledImage != null ? m_scaledImage : m_baseImage, 0, 0, null);
    }
}
