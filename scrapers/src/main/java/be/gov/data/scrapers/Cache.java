/*
 * Copyright (c) 2015, FPS BOSA DG DT
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * * Redistributions of source code must retain the above copyright notice, this
 *   list of conditions and the following disclaimer.
 * * Redistributions in binary form must reproduce the above copyright notice,
 *   this list of conditions and the following disclaimer in the documentation
 *   and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 */
package be.gov.data.scrapers;

import java.io.File;
import java.net.URL;
import java.util.ArrayList;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentMap;

import org.mapdb.DB;
import org.mapdb.DBMaker;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Object store for keeping downloaded pages / feeds
 * 
 * @author Bart Hanssens
 */
public class Cache {
    private static final Logger LOG = LoggerFactory.getLogger(Cache.class);
        
    private DB db = null;
    private static final String CACHE = "cache";
    private static final String URLS = "urls";
    private static final String PAGES = "pages";

    /**
     * Store list of URLs.
     * 
     * @param urls 
     */
    public void storeURLList(List<URL> urls)  {
        ConcurrentMap<String, List<URL>> map = db.hashMap(Cache.CACHE);
        map.put(Cache.URLS, urls);
        //db.commit();
    }
    
    /**
     * Get the list of URLs in the cache.
     * 
     * @return 
     */
    public List<URL> retrieveURLList() {
        ConcurrentMap<String, List<URL>> map = db.hashMap(Cache.CACHE);
        return map.getOrDefault(Cache.URLS, new ArrayList<>());
    }
    
    /**
     * Store a web page
     * 
     * @param id
     * @param page
     * @param lang 
     */
    public void storePage(URL id, String lang, Page page) {
        LOG.debug("Storing page {} with lang {} to cache", id, lang);
        ConcurrentMap<URL, Map<String, Page>> map = db.hashMap(Cache.PAGES);
        Map<String, Page> p = map.getOrDefault(id, new HashMap<>());
        p.put(lang, page);
        map.put(id, p);
        //db.commit();
    }
    
    /**
     * Retrieve a page from the cache.
     * 
     * @param id
     * @return page object
     */
    public Map<String, Page> retrievePage(URL id) {
        LOG.debug("Retrieving page {} from cache", id);
        ConcurrentMap<URL, Map<String, Page>> map = db.hashMap(Cache.PAGES);
        return map.getOrDefault(id, Collections.emptyMap());
    }
	
	/**
     * Get the list of pages in the cache.
     * 
     * @return 
     */
    public Set<URL> retrievePageList() {
		ConcurrentMap<URL, Map<String, Page>> map  = db.hashMap(Cache.PAGES);
        return (map == null) ? Collections.emptySet() : map.keySet();
    }
    
    /**
     * Store a map to the cache.
     * 
     * @param id
     * @param map 
     */
    public void storeMap(URL id, Map<String,String> map) {
        ConcurrentMap<URL, Map<String,String>> m = db.hashMap(Cache.PAGES);
        m.put(id, map);
        //db.commit();
    }
    
    /**
     * Retrieve a map from the cache.
     * 
     * @param id
     * @return 
     */
    public Map<String,String> retrieveMap(URL id) {
        ConcurrentMap<URL, Map<String,String>> map = db.hashMap(Cache.PAGES);
        return map.getOrDefault(id, Collections.emptyMap());
    }
    
    /**
     * Close cache
     */
    public void shutdown() {
        LOG.info("Closing cache file ...");
		try {
			db.close();
		} catch(Exception e) {
			LOG.error(e.getMessage());
		}
		LOG.info(" ... closed");
    }
    
	/**
	 * Constructor
	 * 
	 * @param f 
	 */
    public Cache(File f) {
        LOG.info("Opening cache file {}", f.getAbsolutePath());
        db = DBMaker.fileDB(f).asyncWriteEnable().fileLockDisable().closeOnJvmShutdown().transactionDisable().make();
		LOG.info(" ... opened");
    }
}
