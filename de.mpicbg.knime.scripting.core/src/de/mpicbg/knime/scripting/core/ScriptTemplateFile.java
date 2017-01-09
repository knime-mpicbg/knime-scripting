package de.mpicbg.knime.scripting.core;

import de.mpicbg.knime.scripting.core.rgg.wizard.ScriptTemplate;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

/**
 * This class represents a template file consisting of a location (URL) and a list of templates
 * <p/>
 * Created by IntelliJ IDEA.
 * User: Antje Niederlein
 * Date: 10/26/11
 * Time: 8:42 AM
 */
public class ScriptTemplateFile {

	private String filePath;
    private List<ScriptTemplate> templates;

    /**
     * @param filePath location of template file
     */
    public ScriptTemplateFile(String filePath) {
        this.filePath = filePath;
        parseTemplateFile();
    }

    /**
     * parse file and fill template list with templates
     */
    private void parseTemplateFile() {
        templates = new ArrayList<ScriptTemplate>();


        try {
        	BufferedReader reader = null;
        	try {
        		URL fileUrl = new URL(filePath);
        		reader = new BufferedReader(new InputStreamReader(fileUrl.openStream()));
        	} catch(MalformedURLException mue) {
        		reader = Files.newBufferedReader(Paths.get(this.filePath), StandardCharsets.UTF_8);
        	}

            StringBuffer templateBuffer = new StringBuffer();

            String line = reader.readLine();
            while (line != null) {
            	// at least 10 times '#' mark the beginning of a new template
                if (line.startsWith("##########")) {
                    if (!templateBuffer.toString().trim().isEmpty()) {
                    	ScriptTemplate newTemplate = ScriptTemplate.parse(templateBuffer.toString(), filePath);
                    	// template text might not contain a valid template structure
                        if(newTemplate != null) templates.add(newTemplate);
                        templateBuffer = new StringBuffer();
                    }
                } else templateBuffer.append(line + "\n");

                line = reader.readLine();
            }
            // don't forget the last template
            if (templateBuffer.length() > 0) {
            	ScriptTemplate newTemplate = ScriptTemplate.parse(templateBuffer.toString(), filePath);
            	// template text might not contain a valid template structure
                if(newTemplate != null) templates.add(newTemplate);
            }
        } catch (IOException e) {
        }
    }

    /**
     * check whether the template list contains any template
     *
     * @return true if there is at least one template
     */
    public boolean isEmpty() {
        return templates.isEmpty();
    }

    /**
     * applies a scripting language to every single template (could have been a parameter for this class instead of every single template
     *
     * @param language
     */
    public void setScriptingLanguage(String language) {
        for (ScriptTemplate template : templates) template.setScriptingLanguage(language);
    }
    
    /**
     * @return string representation of URL
     */
    public String getFilePath() {
		return filePath;
	}

	public List<ScriptTemplate> getTemplates() {
		return templates;
	}
}
