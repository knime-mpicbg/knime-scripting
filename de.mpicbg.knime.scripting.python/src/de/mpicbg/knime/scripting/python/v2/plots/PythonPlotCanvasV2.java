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
