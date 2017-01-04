package de.mpicbg.knime.scripting.core;

import java.io.BufferedReader;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URL;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.eclipse.core.runtime.IPath;

import de.mpicbg.knime.scripting.core.prefs.TemplatePref;
import de.mpicbg.knime.scripting.core.prefs.TemplatePrefString;
import de.mpicbg.knime.scripting.core.rgg.wizard.ScriptTemplate;

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
	
	/**
	 * instance of that singleton
	 */
    private static TemplateCache ourInstance = new TemplateCache();
    
    Path cacheDir = null;
    Path indexFile = null;

    /**
     * hashmap which contains for each script-file (full path string key)
     * a script template file object holding all templates from that file
     */
    private Map<String, ScriptTemplateFile> templateCache = new HashMap<String, ScriptTemplateFile>();
    
    private HashMap<String, Path> localFileCache = new HashMap<String, Path>();
    
    /**
     * lock-members to ensure that the hashmap is not accessed from different nodes
     * at the same time
     */
    private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();
    private final Lock readLock  = lock.readLock();
    private final Lock writeLock = lock.writeLock();


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

    /**
     * adds all templates from all template files of the given preference string
     * to the cache and caches a copy of these files to disk for offline access
     * @param prefString
     * @param path
     * @throws IOException
     */
	public void addTemplatesFromPref(String prefString, IPath path) throws IOException {
		List<String> templateFiles = parseConcatendatedURLs(prefString);
		
		for(String filePath : templateFiles) {
			writeLock.lock();
			try {
				if (!templateCache.containsKey(filePath)) {
		            // if not, add it to the cache
		            ScriptTemplateFile newTemplateFile = new ScriptTemplateFile(filePath);
		            if (!newTemplateFile.isEmpty()) {
		            	// add to template cache
		                templateCache.put(filePath, newTemplateFile);
		                // check local cache for files
		                updateLocalFileCache(path);
		                // cache file to disk if necessary
		                cacheFileOnDisk(filePath, path);
		                
		            } else throw new IOException(filePath + " does not contain any valid template or cannot be accessed.");
		        }
			} finally {
				writeLock.unlock();
			}
		}
	}

	private void updateLocalFileCache(IPath path) throws IOException {
		// set local cache directory and index-file as member
		if(this.cacheDir == null) 
			this.cacheDir = Paths.get(path.append("template_cache").toOSString());
		if(this.indexFile == null)
			this.indexFile = Paths.get(cacheDir.toString(), "tempFiles.index");
		
		// check for directory and index-file and create it if necessary
		try {
			Files.createDirectory(this.cacheDir);
		} catch (FileAlreadyExistsException faee) {}
		try {
			Files.createFile(this.indexFile);	
			return;	//nothing to fill into file cache hashmap as index was not existent yet
		} catch (FileAlreadyExistsException faee) {}
		
		// read content of index-file
		BufferedReader reader = Files.newBufferedReader(indexFile, StandardCharsets.UTF_8);
		
		String line = reader.readLine();
        while (line != null) {
        	String[] splitted = line.split(";");
        	if(splitted.length == 2) {
        		Path f = Paths.get(splitted[1]);
        		if(Files.exists(f))
        			localFileCache.put(splitted[0], f);
        	}
        	line = reader.readLine();
        }
        reader.close();
	}

	private void cacheFileOnDisk(String templateFile, IPath path) throws IOException {

		URL templateURL = new URL(templateFile);
		String protocol = templateURL.getProtocol();
		
		// do not cache local files
		if(protocol.equals("file"))
			return;
	
		// if the cached file exists, check for content equality
		boolean isContentEqual = false;
		boolean hasCachedFile = localFileCache.containsKey(templateFile);
		Path cachedFile = null;
		
		// compare cached version with original file
		if(hasCachedFile) {
			cachedFile = localFileCache.get(templateFile);	
			byte[] local = Files.readAllBytes(cachedFile);
			byte[] remote = Files.readAllBytes(Paths.get(templateFile));
			
			isContentEqual = Arrays.equals(remote, local);	
			
			// no need to cache
			if(isContentEqual) return;
			else {
				// delete old version and recreate an empty file
				Files.delete(cachedFile);
				Files.createFile(cachedFile);
			}
		} else {
			cachedFile = Files.createTempFile(cacheDir, "template_", ".txt");
		}
				
		ReadableByteChannel rbc = null;
		FileOutputStream fos = null;
		try {
			// cache file on disk
			rbc = Channels.newChannel(templateURL.openStream());
			fos = new FileOutputStream(cachedFile.toString());
			fos.getChannel().transferFrom(rbc, 0, Long.MAX_VALUE);
			fos.close();
			rbc.close();
			
			// add file to index file
			String addLine = new String(templateFile + ";" + cachedFile.toString());
			Files.write(this.indexFile, addLine.getBytes(), StandardOpenOption.APPEND);
			
			// add file to hashmap
			localFileCache.put(templateFile, cachedFile);
			
		} catch(IOException io) {
			Files.delete(cachedFile);
			throw io;
		} finally {
			if(rbc != null) rbc.close();
			if(fos != null) fos.close();
		}
	}
}
