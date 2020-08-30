package de.mpicbg.knime.scripting.core.panels;

import java.awt.BorderLayout;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.OverlayLayout;

import org.knime.core.node.NodeModel;

/**
 * Panel to provide the plot view of scripting nodes
 * Provides Dimension information a procedures to reproduce the image after rescaling
 * 
 * @author Antje Janosch
 *
 */
public class ScriptingPlotPanel extends JPanel {
	
	private static final long serialVersionUID = 1L;
	
	/** flag to mark whether image is in recreation process */
	private boolean isReCreatingImage = false;
	/** panel which contains the image */
	private ScriptingPlotCanvas<NodeModel> m_plotPanel;
	/** label which contains the dimensions */
	private JLabel label;

	/**
	 * constructor 
	 * 
	 * @param plotPanel
	 */
	public ScriptingPlotPanel(ScriptingPlotCanvas<NodeModel> plotPanel) {
		
		this.m_plotPanel = plotPanel;
		
		m_plotPanel.setFocusable(true);
        m_plotPanel.setPreferredSize(m_plotPanel.getPlotDimensionsFromModel());
        m_plotPanel.setBaseImage(m_plotPanel.getBaseImageFromModel());
		
		this.setLayout(new OverlayLayout(this));
        
        JPanel showText = new JPanel();
        showText.setLayout(new BorderLayout());
        
        label = new JLabel("");
        label.setOpaque(false);      
        showText.add(label, BorderLayout.SOUTH);
        showText.setOpaque(false);
        
        // order is important! added last = background layer, added first = foreground layer
        this.add(showText);
        this.add(plotPanel);
            
        // rescale image when component is resized
        // display height, width
        plotPanel.addComponentListener(new ComponentAdapter() {
			
			@SuppressWarnings("unchecked")
			@Override
			public void componentResized(ComponentEvent e) {
				if (!isVisible()) {
                    return;
                }
				((ScriptingPlotCanvas<NodeModel>)e.getComponent()).rescaleImage(getWidth(), getHeight());
				setDimensionLabel();
				
				invalidate();
    			repaint();
			}
			
		});
        
        this.addComponentListener(new ComponentAdapter() {

        	@Override
        	public void componentShown(ComponentEvent e) {
        		super.componentShown(e);			
        		setDimensionLabel();
        	}

        });

        this.addMouseListener(new MouseAdapter() {
        	@Override
        	public void mouseClicked(MouseEvent mouseEvent) {
        		if (!isReCreatingImage) {
        			isReCreatingImage = true;
        			m_plotPanel.recreateImage(getWidth(),getHeight());
        			isReCreatingImage = false;

        			invalidate();
        			repaint();
        		}
        	}
        });

	}
	
	/**
	 * update dimension label
	 */
    private void setDimensionLabel() {
    	label.setText(getWidth() + " x " + getHeight());
    }
}
