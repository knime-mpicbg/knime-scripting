package de.mpicbg.tds.knime.knutils.scripting.templatewizard;

import org.apache.commons.lang.StringUtils;

import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;


/**
 * Document me!
 *
 * @author Holger Brandl
 */
public class ScriptTemplate implements Cloneable {

    private String author;

    private List<String> categories = new ArrayList<String>();

    private String description;

    private String template;

    private Map<String, Object> persistedConfig;

    private String name;
    private boolean linkedToScript = true;

    private String previewURL;

    private String templateURL;

    public static final String DESCRIPTION_PATTERN = "$$$TEMPLATE_DESC$$$";
    public static final String NAME_PATTERN = "$$$TEMPLATE_NAME$$$";

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


    public static ScriptTemplate parse(String rawTemplateText, URL templateFile) {

        String[] lines = rawTemplateText.split("\n");

        if (lines.length < 3)
            return null;


        ScriptTemplate template = new ScriptTemplate();

        template.setTemplateURL(templateFile.toString());

        int rowCounter = 0;
        if (lines[rowCounter].contains("name")) {
            template.setName(lines[rowCounter++].split("name[:]")[1].trim());
        } else {
            throw new RuntimeException("R-templates must start with a line with the scheme # name: blabla");
        }

        if (lines[rowCounter].contains("author")) {
            template.setAuthor(lines[rowCounter++].split("author[:]")[1].trim());
        }

        if (lines[rowCounter].contains("category")) {
            template.setCategories(Arrays.asList(lines[rowCounter++].split("category[:]")[1].split(";")));
        } else {
            template.setCategories(new ArrayList<String>());
        }

        if (lines[rowCounter].contains("preview")) {
            String previewURL = lines[rowCounter++].split("preview[:]")[1];
            if (!StringUtils.isBlank(previewURL)) {
                // add a prefix if relative url
                String rootLocation = templateFile.toString().substring(0, templateFile.toString().lastIndexOf("/") + 1);
                previewURL = previewURL.contains("://") ? previewURL : rootLocation + previewURL.trim();

                template.setPreviewURL(previewURL);
            }
        }


        // read the description
        StringBuffer description = new StringBuffer();
        for (int i = rowCounter; i < lines.length; i++) {
            rowCounter++;
            String line = lines[i];

            if (line.startsWith("######")) {
                template.setDescription(description.toString());
                break;
            }

            if (line.trim().isEmpty() && description.length() == 0)
                continue;

            description.append(line + "\n");
        }

        StringBuffer rcode = new StringBuffer();
        for (int i = rowCounter; i < lines.length; i++) {
            String line = lines[i];

            if (line.trim().isEmpty() && rcode.length() == 0)
                continue;

            rcode.append(line + "\n");
        }

        String templateText = rcode.toString();

        // prepare the template by insertion the description into it if necessary
        templateText = templateText.replace(DESCRIPTION_PATTERN, template.getDescription());
        templateText = templateText.replace(NAME_PATTERN, template.getName());

        template.setTemplate(templateText);

        return template;
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
}
