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
package be.fedict.dcat.scrapers;

import be.fedict.dcat.helpers.Storage;
import be.fedict.dcat.helpers.Cache;
import java.io.File;
import java.io.IOException;
import java.io.Writer;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Properties;
import org.apache.http.HttpHost;
import org.openrdf.repository.RepositoryException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Scraper scraper class
 * 
 * @author Bart Hanssens <bart.hanssens@fedict.be>
 */
public abstract class Scraper {
    private final Logger logger = LoggerFactory.getLogger(Scraper.class);

    public final static String PROP_PREFIX = "be.fedict.dcat.scrapers";
    
    private Properties prop = null;
    private String prefix = "";
    
    private HttpHost proxy = null;
    private int delay = 2000;
    private String lang = "";
    private Cache cache = null;
    private Storage store = null;
    private URL base = null;
    
    /**
     * Get cache
     * 
     * @return local cache file
     */
    public Cache getCache() {
        return cache;
    }
    
    /**
     * Get triple store
     * 
     * @return RDF triple store
     */
    public Storage getTripleStore() {
        return store;
    }
    
    /**
     * Get base URL
     * 
     * @return base URL
     */
    public URL getBase() {
        return base;
    }
    
    /**
     * Set default language
     * 
     * @param lang 
     */
    public void setDefaultLang(String lang) {
        this.lang = lang;
    }
    /**
     * Get default language
     * 
     * @return language
     */
    public String getDefaultLang() {
        return lang;
    }
    
    /**
     * Set HTTP proxy.
     * 
     * @param proxy proxy server
     * @param port proxy port
     */
    public void setProxy(String proxy, int port) {
        if (proxy == null || proxy.isEmpty()) {
            this.proxy = null;
        } else {
            this.proxy = new HttpHost(proxy, port);
        }
    }
    
    /**
     * Get HTTP proxy
     * 
     * @return proxy or null
     */
    public HttpHost getProxy() {
        return proxy;
    }
    
    /**
     * Get delay between HTTP requests
     * 
     * @return 
     */
    public int getDelay() {
        return delay;
    }
    
    /**
     * Set property from config file and property prefix.
     * 
     * @param prop
     * @param prefix 
     */
    public void setProperties(Properties prop, String prefix) {
        this.prop = prop;
        this.prefix = prefix;
    }
    
    /**
     * Get property from config file.
     * 
     * @param name
     * @return property value
     */
    public String getProperty(String name) {
        String p = prefix + "." + name;

        String value = prop.getProperty(p);
        if (value == null) {
            logger.error("No property {}", p);
        }
        return value;
    }
    
    /**
     * Sleep (between HTTP requests)
     */
    public void sleep() {
        try {
            Thread.sleep(getDelay());
        } catch (InterruptedException ex) {
        }
    }
    
    /**
     * Fetch all metadata from repository / site
     * 
     * @throws IOException 
     */
    public abstract void scrape() throws IOException;
    
    /**
     * Write DCAT file to output stream
     * 
     * @param out
     * @throws RepositoryException
     * @throws MalformedURLException 
     */
    public abstract void writeDcat(Writer out) throws RepositoryException, MalformedURLException;
    
    /**
     * Constructor
     * 
     * @param caching
     * @param storage 
     */
    public Scraper(File caching, File storage, URL base) {
        cache = new Cache(caching);
        store = new Storage(storage);
        this.base = base;
    }
}
