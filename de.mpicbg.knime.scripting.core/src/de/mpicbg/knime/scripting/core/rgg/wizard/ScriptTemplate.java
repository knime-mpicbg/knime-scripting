package de.mpicbg.knime.scripting.core.rgg.wizard;

import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;


/**
 * As the class is used for serialization/deserialization template configurations to node.xml:
 * DO NOT REFACTOR (at least do not rename the class)!
 *
 * @author Holger Brandl
 */
public class ScriptTemplate implements Cloneable {

	/** scripting language */
    private String scriptingLanguage = "";

    /** template author */
    private String author;

    /** list of categories */
    private List<String> categories = new ArrayList<String>();

    /** description of the template */
    private String description;

    /** the full template */
    private String template;

    private Map<String, Object> persistedConfig;

    /** name of the template */
    private String name;
    
    /** false, if the template is selected but has been unlinked for modification */
    private boolean linkedToScript = true;

    /** preview image URL */
    private String previewURL;

    /** URL-source */
    private String templateURL;
    
    

    /** RGG placeholders */
    public static final String DESCRIPTION_PATTERN = "$$$TEMPLATE_DESC$$$";
    public static final String NAME_PATTERN = "$$$TEMPLATE_NAME$$$";


    public String getTemplateURL() {
        return templateURL;
    }

    public void setTemplateURL(String templateURL) {
        this.templateURL = templateURL;
    }

    public void setName(String name) {
        this.name = name;
    }


    public String getName() {
        return name;
    }


    public String getAuthor() {
        return author;
    }


    public void setAuthor(String author) {
        this.author = author;
    }


    public List<String> getCategories() {
        return categories;
    }


    private void setCategories(List<String> categories) {
        this.categories = categories;
    }


    public String getPreviewURL() {
        return previewURL;
    }


    public void setPreviewURL(String previewURL) {
        this.previewURL = previewURL;
    }


    public String getDescription() {
        return description;
    }


    public void setDescription(String description) {
        this.description = description;
    }


    public String getTemplate() {
        return template;
    }


    public void setTemplate(String template) {
        this.template = template;
    }


    @Override
    public String toString() {
        return name;
    }

    public String getScriptingLanguage() {
        return scriptingLanguage;
    }

    public void setScriptingLanguage(String scriptingLanguage) {
        this.scriptingLanguage = scriptingLanguage;
    }

    public static ScriptTemplate parse(String rawTemplateText, String templateFile) {

        String[] lines = rawTemplateText.split("\n");

        if (lines.length < 3)
            return null;


        ScriptTemplate template = new ScriptTemplate();

        template.setTemplateURL(templateFile);
        
        // parse meta information, template name is required
        int rowCounter = 0;
        while(lines[rowCounter].startsWith("#")) {
        	String currentLine = lines[rowCounter];
        	if(currentLine.matches("^# *name *: *.*$")) template.setName(currentLine.split("name *:")[1].trim());
        	if(currentLine.matches("^# *author *: *.*$")) template.setAuthor(currentLine.split("author *:")[1].trim());
        	       	
        	if(currentLine.matches("^# *category *: *.*$")) {
        		String[] categories = currentLine.split("category *:")[1].split("/");
        		ArrayList<String> categoryList = new ArrayList<String>(Arrays.asList(categories));
        		template.setCategories(categoryList);
        	}
        	
        	if(currentLine.matches("^# *preview *: *.*$")) {
        		String previewURL = currentLine.split("preview *:")[1].trim();
                if (!StringUtils.isBlank(previewURL)) {
                    // add a prefix if relative url
                    String rootLocation = templateFile.substring(0, templateFile.lastIndexOf("/") + 1);
                    previewURL = previewURL.contains("://") ? previewURL : rootLocation + previewURL.trim();

                    template.setPreviewURL(previewURL);
                }
        	}
        	rowCounter++;
        }

        // read the description
        StringBuffer descBuffer = new StringBuffer();
        String line = lines[rowCounter];
        while(!line.matches("^[#]{6}") && rowCounter < lines.length){
        	descBuffer.append(line + "\n");
        	line = lines[rowCounter++];
        }
        
        if(descBuffer.length() > 0) {
        	String description = descBuffer.toString();
        	description = StringUtils.trim(description);
        	if(!StringUtils.isBlank(description)) template.setDescription(description);
        }
        
        // template can be an rgg-template or raw R code
        StringBuffer templateBuffer = new StringBuffer();
        for (int i = rowCounter; i < lines.length; i++) {
            templateBuffer.append(lines[i] + "\n");
        }
        
        if(templateBuffer.length() > 0) {
        	String templateText = templateBuffer.toString();
        	templateText = StringUtils.trim(templateText);
        	
        	// prepare the template by insertion the description into it if necessary
        	if(template.getDescription() != null) 
        		templateText = templateText.replace(DESCRIPTION_PATTERN, template.getDescription());
        	if(template.getName() != null)
        		templateText = templateText.replace(NAME_PATTERN, template.getName());
            
        	if(!StringUtils.isBlank(templateText)) template.setTemplate(templateText);
        }
        
        if(template.getName() != null && template.getTemplate() != null) return template;
        else return null;
    }


    public boolean isRGG() {
        return template.contains("</rgg>");
    }


    public Map<String, Object> getPersistedConfig() {
        return persistedConfig;
    }


    public void setPersistedConfig(Map<String, Object> persistedConfig) {
        this.persistedConfig = persistedConfig;
    }


    public boolean isLinkedToScript() {
        return linkedToScript;
    }


    public void setLinkedToScript(boolean linkedToScript) {
        this.linkedToScript = linkedToScript;
    }


    @Override
    public Object clone() {
        ScriptTemplate o = null;

        try {
            o = (ScriptTemplate) super.clone();
        } catch (CloneNotSupportedException e) {
            throw new RuntimeException(e);
        }

        return o;
    }


    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ScriptTemplate)) return false;

        ScriptTemplate that = (ScriptTemplate) o;

        if (templateURL != null ? !templateURL.equals(that.templateURL) : that.templateURL != null) return false;
        if (linkedToScript != that.linkedToScript) return false;
        if (author != null ? !author.equals(that.author) : that.author != null) return false;
        if (categories != null ? !categories.equals(that.categories) : that.categories != null) return false;
        if (description != null ? !description.equals(that.description) : that.description != null) return false;
        if (name != null ? !name.equals(that.name) : that.name != null) return false;
        if (persistedConfig != null ? !persistedConfig.equals(that.persistedConfig) : that.persistedConfig != null)
            return false;
        if (previewURL != null ? !previewURL.equals(that.previewURL) : that.previewURL != null) return false;
        if (template != null ? !template.equals(that.template) : that.template != null) return false;

        return true;
    }


    @Override
    public int hashCode() {
        int result = author != null ? author.hashCode() : 0;
        result = 31 * result + (templateURL != null ? templateURL.hashCode() : 0);
        result = 31 * result + (categories != null ? categories.hashCode() : 0);
        result = 31 * result + (description != null ? description.hashCode() : 0);
        result = 31 * result + (template != null ? template.hashCode() : 0);
        result = 31 * result + (persistedConfig != null ? persistedConfig.hashCode() : 0);
        result = 31 * result + (name != null ? name.hashCode() : 0);
        result = 31 * result + (linkedToScript ? 1 : 0);
        result = 31 * result + (previewURL != null ? previewURL.hashCode() : 0);
        return result;
    }
    
    public static void main(String[] args) {
        // test equals()
        ScriptTemplate o1 = new ScriptTemplate();
        o1.setAuthor("me");
        o1.setLinkedToScript(true);

        ScriptTemplate o2 = (ScriptTemplate) o1.clone();
        ScriptTemplate o3 = (ScriptTemplate) o1.clone();
        o3.setAuthor("you");

        boolean result = o1.equals(o2);
        System.out.println(result);
        System.out.println(o1.hashCode() + " = " + o2.hashCode());
        System.out.println(o1.equals(o3));
        System.out.println(o1.hashCode() + " = " + o3.hashCode());
    }
}
