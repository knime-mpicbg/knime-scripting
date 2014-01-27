package de.mpicbg.tds.knime.knutils.scripting;

import de.mpicbg.tds.knime.knutils.scripting.prefs.TemplatePref;
import de.mpicbg.tds.knime.knutils.scripting.prefs.TemplatePrefString;
import de.mpicbg.tds.knime.knutils.scripting.templatewizard.ScriptTemplate;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Singleton
 * Only one instance per KNIME session stores all scripting templates.
 * It will be filled when a node of a certain type will be opened first or if a template needs to be updated
 * <p/>
 * Created by IntelliJ IDEA.
 * User: Antje Niederlein
 * Date: 10/26/11
 * Time: 8:27 AM
 */
public class TemplateCache {
    private static TemplateCache ourInstance = new TemplateCache();

    private Map<String, ScriptTemplateFile> templateCache = new HashMap<String, ScriptTemplateFile>();
    //private Map<URL, ScriptTemplateFile> templateCache = new HashMap<URL, ScriptTemplateFile>();

    public static TemplateCache getInstance() {
        return ourInstance;
    }

    private TemplateCache() {
    }

    /**
     * returns all templates of a given file and add them to the cache of not yet loaded
     *
     * @param filePath
     * @return list of templates
     */
    public List<ScriptTemplate> getTemplateCache(String filePath) throws IOException {
        List<ScriptTemplate> templates = null;
        // test if file already has been loaded
        if (!templateCache.containsKey(filePath)) {
            // if not, add it to the cache
            ScriptTemplateFile newTemplateFile = new ScriptTemplateFile(filePath);
            if (!newTemplateFile.isEmpty()) {
                templateCache.put(filePath, newTemplateFile);
            } else throw new IOException(filePath + " does not contain any valid template or cannot be accessed.");
        }

        // then load the content from the cache
        templates = templateCache.get(filePath).templates;

        return templates;
    }

    /**
     * reloads all given templateFiles into the Cache
     *
     * @param filePath
     */
    public List<ScriptTemplate> updateTemplateCache(String filePath) throws IOException {

        List<ScriptTemplate> templates = null;

        templateCache.remove(filePath);
        ScriptTemplateFile reloadedTemplate = new ScriptTemplateFile(filePath);
        if (!reloadedTemplate.isEmpty()) {
            templateCache.put(filePath, reloadedTemplate);
            templates = templateCache.get(filePath).templates;
        } else throw new IOException(filePath + " does not contain any valid template or cannot be accessed.");

        return templates;
    }

    /**
     * Template preference string will be splitted by a pattern
     *
     * @param templateFilePaths
     * @return List of active URLs
     * @see TemplatePrefString
     */
    public List<String> parseConcatendatedURLs(String templateFilePaths) {

        TemplatePrefString tString = new TemplatePrefString(templateFilePaths);
        List<TemplatePref> templateList = tString.parsePrefString();
        List<String> urls = new ArrayList<String>();

        for (TemplatePref pref : templateList) {
            if (pref.isActive()) {
                urls.add(pref.getUri());
                /*try {
                    URL newURL = new URL(pref.getUri());
                    urls.add(newURL);
                } catch (MalformedURLException e) {
                    e.printStackTrace();
                }    */
            }
        }
        return urls;
    }

    /**
     * Does the cache contain a certain file?
     *
     * @param filePath
     * @return
     */
    public boolean contains(String filePath) {
        return templateCache.containsKey(filePath);
    }

    /**
     * remove template file from cache
     *
     * @param filePath
     */
    public void remove(String filePath) {
        templateCache.remove(filePath);
    }
}
