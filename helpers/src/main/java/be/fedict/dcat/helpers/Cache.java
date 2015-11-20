/*
 * Copyright (c) 2015, Bart Hanssens <bart.hanssens@fedict.be>
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
package be.fedict.dcat.helpers;

import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentMap;
import org.mapdb.DB;
import org.mapdb.DBMaker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Bart Hanssens <bart.hanssens@fedict.be>
 */
public class Cache {
    private final Logger logger = LoggerFactory.getLogger(Cache.class);
        
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
        db.commit();
    }
    
    /**
     * Get the list of URLs in the cache.
     * 
     * @return 
     */
    public List<URL> retrieveURLList() {
        ConcurrentMap<String, List<URL>> map = db.hashMap(Cache.CACHE);
        return map.getOrDefault(Cache.URLS, new ArrayList<URL>());
    }
    
    /**
     * Store a web page
     * 
     * @param id
     * @param page
     * @param lang 
     */
    public void storePage(URL id, String lang, Page page) {
        ConcurrentMap<URL, Map<String, Page>> map = db.hashMap(Cache.PAGES);
        Map<String, Page> p = map.getOrDefault(id, new HashMap<String, Page>());
        p.put(lang, page);
        map.put(id, p);
        db.commit();
    }
    
    /**
     * Retrieve a page from the cache.
     * 
     * @param id
     * @return page object
     */
    public Map<String, Page> retrievePage(URL id) {
        ConcurrentMap<URL, Map<String, Page>> map = db.hashMap(Cache.PAGES);
        return map.getOrDefault(id, new HashMap<String, Page>());
    }
    
    /**
     * Store a list to the cache.
     * 
     * @param id
     * @param list 
     */
    public void storeList(URL id, List<String> list) {
        ConcurrentMap<URL, List<String>> map = db.hashMap(Cache.PAGES);
        map.put(id, list);
        db.commit();
    }
    
    /**
     * Retrieve a list from the cache.
     * 
     * @param id
     * @return 
     */
    public List<String> retrieveList(URL id) {
        ConcurrentMap<URL, List<String>> map = db.hashMap(Cache.PAGES);
        return map.getOrDefault(id, new ArrayList<String>());
    }
    
    /**
     * Close cache
     */
    public void shutdown() {
        logger.info("Closing cache file");
        db.close();
    }
    
    public Cache(File f) {
        logger.info("Opening cache file " + f.getAbsolutePath());
        
        db = DBMaker.fileDB(f).make();
    }
}
