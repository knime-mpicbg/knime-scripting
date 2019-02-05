package de.mpicbg.knime.scripting.core;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
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
    
    //====================================================================================
    // members
    //====================================================================================
    
    //Path cacheDir = null;
    //Path indexFile = null;

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
    
    //====================================================================================
    // initialization
    //====================================================================================

    public static TemplateCache getInstance() {
        return ourInstance;
    }

    private TemplateCache() {
    }
    
    //====================================================================================
    // methods
    //====================================================================================
    
	/**
     * adds all templates from all template files of the given preference string
     * to the cache and caches a copy of these files to disk for offline access
     * @param prefString
     * @param path
     * @throws IOException
     */
	public void addTemplatesFromPref(String prefString, Path bundlePath, Path indexFile) throws IOException {
		//List<String> templateFiles = parseConcatendatedURLs(prefString);
		TemplatePrefString tString = new TemplatePrefString(prefString);
        List<TemplatePref> templateList = tString.parsePrefString();
			
		for(TemplatePref tPref : templateList) {
			if(tPref.isActive()) {
				writeLock.lock();
				try {
					addTemplateFile(tPref.getUri(), bundlePath, indexFile);
				} finally {
					writeLock.unlock();
				}
			}
		}
	}

    /**
     * add all templates from a given template file
     * cache locally if necessary
     *
     * @param filePath
     */
    public void addTemplateFile(String filePath, Path bundlePath, Path indexFile) throws IOException {
    	
    	if (!templateCache.containsKey(filePath)) {
			// check local cache for files
            updateLocalFileCache(bundlePath, indexFile);
            
            ScriptTemplateFile newTemplateFile = null;
			if(isFileReachable(filePath)) {
				newTemplateFile = new ScriptTemplateFile(filePath);
				
				// cache file to disk if necessary
				if (!newTemplateFile.isEmpty())
	                cacheFileOnDisk(filePath, bundlePath, indexFile);
			} else {
				if(localFileCache.containsKey(filePath)) {
					newTemplateFile = new ScriptTemplateFile(localFileCache.get(filePath).toString());
				}
			}
			
			// add templates to cache
            if(newTemplateFile != null)
            	if(!newTemplateFile.isEmpty())
            		templateCache.put(filePath, newTemplateFile);
            	else
            		throw new IOException(filePath + " does not contain any valid template or cannot be accessed.");
    	}
    }
    
    /**
     * remove template file from template cache and from localFileMap
     *
     * @param filePath
     */
    public void removeTemplateFile(String filePath, Path bundlePath, Path indexFile) {
        templateCache.remove(filePath);
        
        assert bundlePath != null;
        assert indexFile != null;
        
        Path localFile = localFileCache.get(filePath);
        try {
			Files.delete(localFile);
		} catch (IOException e) {
			e.printStackTrace();
		}
        
        localFileCache.remove(filePath);
        
        try {
			removeFromIndexFile(filePath, indexFile);
		} catch (IOException e) {
			e.printStackTrace();
		}    
    }
    
    /**
     * remove template file only from template cache <br/>
     * do not delete locally cached file
     * 
     * @param filePath
     */
    public void removeTemplateFile(String filePath) {
    	templateCache.remove(filePath);
    }

    /**
     * read index file and rewrite it without the entry for the removed file
     * @param indexFile
     * @throws IOException
     */
    private void removeFromIndexFile(String filePath, Path indexFile) throws IOException {
    	
    	// read content of index-file
    	BufferedReader reader = null;
    	List<String> allLines = new ArrayList<String>();
    	
    	try {
    		reader = Files.newBufferedReader(indexFile, StandardCharsets.UTF_8);
    		
    		String line = reader.readLine();
    		while (line != null) {
    			String[] splitted = line.split(";");
    			if(splitted.length == 2) {
    				if(!splitted[0].equals(filePath))
    					allLines.add(line);
    			}
    			line = reader.readLine();
    		}
    	} finally {
    		if(reader != null) reader.close();
    	}
    	
    	PrintWriter pw = null;
    	try {
    	  BufferedWriter bw = Files.newBufferedWriter(indexFile, StandardCharsets.UTF_8);
    	  pw = new PrintWriter(bw);
    	  for(String line : allLines) {  
    		  pw.println(line);
    	  }
    	} finally {
    		if(pw != null) pw.close();
    	}
	}

	/**
     * reloads all given templateFiles into the Cache
     *
     * @param filePath
     */
    public void updateTemplateCache(String filePath, Path bundlePath, Path indexFile) throws IOException {

        if(!isFileReachable(filePath))
        	throw new IOException(filePath + " cannot be accessed. Refresh failed.");
    	
        ScriptTemplateFile reloadedTemplate = new ScriptTemplateFile(filePath);
        if (!reloadedTemplate.isEmpty()) {
        	templateCache.remove(filePath);
            templateCache.put(filePath, reloadedTemplate);
            cacheFileOnDisk(filePath, bundlePath, indexFile);
            //templates = templateCache.get(filePath).templates;
        } else throw new IOException(filePath + " does not contain any valid template");

        //return templates;
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

	private void updateLocalFileCache(Path bundlePath, Path indexFile) throws IOException {
		assert bundlePath != null;
		assert indexFile != null;
		
		// check for directory and index-file and create it if necessary
		try {
			Files.createDirectory(bundlePath);
		} catch (FileAlreadyExistsException faee) {}
		try {
			Files.createFile(indexFile);	
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

	/**
	 * if there is already a cached version, check for content equality;
	 * if not yet there => create a new cached version and add to index
	 * if not equal => overwrite old version
	 * @param templateFile
	 * @param path
	 * @throws IOException
	 * @throws URISyntaxException
	 */
	private void cacheFileOnDisk(String templateFile, Path bundlePath, Path indexFile) throws IOException {

		assert bundlePath != null;
		assert indexFile != null;
		
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
			byte[] remote = fetchRemoteFile(templateURL);
			
			isContentEqual = Arrays.equals(remote, local);	
			
			// no need to cache
			if(isContentEqual) return;
			else {
				// delete old version and recreate an empty file
				Files.delete(cachedFile);
				Files.createFile(cachedFile);
			}
		} else {
			cachedFile = Files.createTempFile(bundlePath, "template_", ".txt");
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
			String addLine = new String(templateFile + ";" + cachedFile.toString() + "\n");
			Files.write(indexFile, addLine.getBytes(), StandardOpenOption.APPEND);
			
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
	
	/**
	 * tries to access a remote file, returns true for local file
	 * @param filePath
	 * @return true, if file can be accessed
	 * @throws MalformedURLException
	 */
	private boolean isFileReachable(String filePath) throws MalformedURLException {
		URL templateURL = new URL(filePath);
		String protocol = templateURL.getProtocol();
		
		// returns true, if file is local; otherwise test connection
		if(protocol.equals("file"))
			return true;
		else
			return pingURL(filePath, 2000);	
	}
	
	/**
	 * {@link http://stackoverflow.com/questions/3584210/preferred-java-way-to-ping-an-http-url-for-availability}
	 * <br>
	 * Pings a HTTP URL. This effectively sends a GET (was HEAD before) request and returns <code>true</code> if the response code is in 
	 * the 200-399 range.
	 * @param url The HTTP URL to be pinged.
	 * @param timeout The timeout in millis for both the connection timeout and the response read timeout. Note that
	 * the total timeout is effectively two times the given timeout.
	 * @return <code>true</code> if the given HTTP URL has returned response code 200-399 on a HEAD request within the
	 * given timeout, otherwise <code>false</code>.
	 */
	public static boolean pingURL(String url, int timeout) {
	    url = url.replaceFirst("^https", "http"); // Otherwise an exception may be thrown on invalid SSL certificates.

	    try {
	        HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
	        connection.setConnectTimeout(timeout);
	        connection.setReadTimeout(timeout);
	        connection.setRequestMethod("GET");
	        int responseCode = connection.getResponseCode();
	        return (200 <= responseCode && responseCode <= 399);
	    } catch (IOException exception) {
	        return false;
	    }
	}
	
	/**
	 * retrieves content as byte array from URL
	 * @param url
	 * @return byte array
	 * @throws IOException
	 */
	private byte[] fetchRemoteFile(URL url) throws IOException {
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		InputStream is = null;
		try {
		  is = url.openStream ();
		  byte[] byteChunk = new byte[4096];
		  int n;

		  while ( (n = is.read(byteChunk)) > 0 ) {
		    baos.write(byteChunk, 0, n);
		  }
		}
		finally {
		  if (is != null) { is.close(); }
		}
		return baos.toByteArray();
	}
	


	public List<ScriptTemplate> getTemplates(String templateFilePath) {
		return templateCache.get(templateFilePath).getTemplates();
	}
}
