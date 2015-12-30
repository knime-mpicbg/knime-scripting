package de.mpicbg.knime.scripting.r.port;

import java.awt.BorderLayout;
import java.awt.Font;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.zip.ZipEntry;

import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.text.DefaultCaret;

import org.apache.commons.lang.StringUtils;
import org.knime.core.node.port.PortObject;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.core.node.port.PortObjectZipOutputStream;
import org.rosuda.REngine.REXPMismatchException;
import org.rosuda.REngine.REXPString;
import org.rosuda.REngine.Rserve.RConnection;
import org.rosuda.REngine.Rserve.RserveException;

import de.mpicbg.knime.scripting.core.exceptions.KnimeScriptingException;
import de.mpicbg.knime.scripting.r.RUtils;

public class RPortObject implements PortObject {
	
	private final File m_WorkspaceFile;
	private HashMap<String, String> m_rObjects;

	public RPortObject(File workspaceFile) {
		this.m_WorkspaceFile = workspaceFile;
		this.m_rObjects = getRObjects();
	}

	@Override
	public PortObjectSpec getSpec() {
		final RPortObjectSpec spec = new RPortObjectSpec(m_rObjects);
		return spec;
	}

	private HashMap<String, String> getRObjects() {
		
		HashMap<String, String> rObjects = new HashMap<String, String>();
		RConnection connection = null;
		try {
			connection = RUtils.createConnection();
			RUtils.loadWorkspace(m_WorkspaceFile, connection);
			REXPString objTypes = (REXPString) connection.eval("sapply(mget(ls(), .GlobalEnv), class)"); 
			REXPString objNames = (REXPString) objTypes.getAttribute("names");
			
			String[] types = objTypes.asStrings();
			String[] names = objNames.asStrings();
			
			for(int i = 0; i < objTypes.length(); i++) {
				rObjects.put(names[i], types[i]);
			}
			
		} catch (KnimeScriptingException | RserveException e) {
			if(connection != null) connection.close();
			e.printStackTrace();
			return rObjects;
		}
		
		connection.close();
		return rObjects;
	}

	@Override
	public String getSummary() {
		return "R Objects";
	}

	/**
	 * view provided after execution?
	 * {@inheritDoc}
	 */
	@Override
	public JComponent[] getViews() {
		
		JPanel panel = new JPanel(new BorderLayout());
		panel.setName("R Port");
		
        JTextArea jep = new JTextArea();
        // prevent autoscrolling
        DefaultCaret caret = (DefaultCaret)jep.getCaret();
        caret.setUpdatePolicy(DefaultCaret.NEVER_UPDATE);
        jep.setEditable(false);

        RConnection connection ;
		try {
			connection = RUtils.createConnection();
		} catch (KnimeScriptingException e) {
			jep.setText(e.getMessage());
			panel.add(new JScrollPane(jep));	        
			e.printStackTrace();
			return new JComponent[]{panel};
		}
        jep.setFont(new Font("Monospaced", Font.PLAIN, 14));

        try {
            RUtils.loadWorkspace(m_WorkspaceFile, connection);
                        
            // to get structure information about the output object, one needs to use capture the output
            // str(...) does not provide any return value
            connection.voidEval("tempfile <- tempfile(pattern = 'structure_', fileext = '.txt')");
            List<String> objects = Arrays.asList(connection.eval("ls()").asStrings());
            for(String obj : objects) {
            	// exclude files which where created temporary TODO: it is too dependent on how temporary file variables are named... other solution?
            	if(!(obj.equals("tempfile") || obj.equals("tmpwfile")))
            		connection.voidEval("capture.output(print(\"" + obj + "\"), str(" + obj + "), file = tempfile, append = TRUE)");
            }         
            
            String[] structure = connection.eval("readLines(tempfile)").asStrings();
            connection.voidEval("unlink(tempfile)");
              
            String summary = StringUtils.join(structure, '\n');
            
            jep.setText(summary);

        } catch (Exception e) {
            jep.setText("Failed to retrieve the structure of R objects from port file " + getFilePath());
        }
        
        connection.close();

        panel.add(new JScrollPane(jep));
        return new JComponent[]{panel};
	}

	private String getFilePath() {
		assert m_WorkspaceFile != null;
		return m_WorkspaceFile.getAbsolutePath();
	}

	public File getFile() {
		return m_WorkspaceFile;
	}



}
