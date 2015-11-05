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
import be.fedict.dcat.vocab.DCAT;
import com.google.common.hash.HashFunction;
import com.google.common.hash.Hashing;
import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.io.Writer;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonReader;
import org.apache.http.HttpHost;
import org.apache.http.client.fluent.Request;
import org.openrdf.model.URI;
import org.openrdf.model.vocabulary.DCTERMS;
import org.openrdf.model.vocabulary.FOAF;
import org.openrdf.model.vocabulary.RDF;
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
    
    public final static String LICENSE_CC0 = 
                        "http://creativecommons.org/publicdomain/zero/1.0/";
    
    private Properties prop = null;
    private String prefix = "";
    
    private HttpHost proxy = null;
    private int delay = 1000;
    
    private String defLang = "";
    private String[] allLangs = {};
    
    private Cache cache = null;
    private Storage store = null;
    private URL base = null;
    
    private final static HashFunction HASHER = Hashing.sha1();
    public final static DateFormat DATEFMT = 
                            new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSSS");
    
    
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
     * @param lang language code
     */
    public void setDefaultLang(String lang) {
        this.defLang = lang;
    }
    /**
     * Get default language
     * 
     * @return language language code
     */
    public String getDefaultLang() {
        return defLang;
    }
    
    /**
     * Set all languages
     * 
     * @param langs array of language codes
     */
    public void setAllLangs(String langs[]) {
        this.allLangs = langs;
    }
    
    /**
     * Get all languages
     * 
     * @return array of language codes
     */
    public String[] getAllLangs() {
        return allLangs;
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
     * Make a hashed ID based upon a string.
     * 
     * @param s
     * @return 
     */
    public String makeHashId(String s) {
        return HASHER.hashBytes(s.getBytes()).toString();
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
     * Make HTTP GET request.
     * 
     * @param url
     * @return JsonObject containing CKAN info
     * @throws IOException 
     */
    public JsonObject makeJsonRequest(URL url) throws IOException {
        Request request = Request.Get(url.toString());
        if (getProxy() != null) {
            request = request.viaProxy(getProxy());
        }
        String json = request.execute().returnContent().asString();
        JsonReader reader = Json.createReader(new StringReader(json));
        return reader.readObject();
    }
    
    /**
     * Make HTTP GET request.
     * 
     * @param url
     * @return String containing raw page
     * @throws IOException 
     */
    public String makeRequest(URL url) throws IOException {
        Request request = Request.Get(url.toString());
        if (getProxy() != null) {
            request = request.viaProxy(getProxy());
        }
        return request.execute().returnContent().asString();
    }
    
    
    /**
     * Fetch all metadata from repository / site
     * 
     * @throws IOException 
     */
    public abstract void scrape() throws IOException;

    /**
     * Extra DCAT catalog info
     * 
     * @param store
     * @param catalog 
     * @throws org.openrdf.repository.RepositoryException 
     */
    public void generateCatalogInfo(Storage store, URI catalog) 
                                                    throws RepositoryException {
        store.add(catalog, DCTERMS.DESCRIPTION, "Converted by Fedict's converter", "en");
        store.add(catalog, DCTERMS.MODIFIED, DATEFMT.format(new Date()));
        store.add(catalog, DCTERMS.LICENSE, store.getURI(LICENSE_CC0));
        store.add(catalog, FOAF.HOMEPAGE, getBase());
    }
    
    /**
     * Generate DCAT Catalog.
     * 
     * @param urls
     * @param store
     * @throws RepositoryException 
     */
    public void generateCatalog(List<URL> urls, Storage store) throws RepositoryException {
        URI catalog = store.getURI(getBase().toString());
        
        store.add(catalog, RDF.TYPE, DCAT.A_CATALOG);
        store.add(catalog, DCTERMS.MODIFIED, DATEFMT.format(new Date()));
        
        for (URL u : urls ){
            store.add(catalog, DCAT.DATASET, store.getURI(u.toString()));
        }
        generateCatalogInfo(store, catalog);
    }
    
     /**
     * Generate DCAT Datasets
     * 
     * @param page
     * @param store
     * @throws MalformedURLException
     * @throws RepositoryException 
     */
    public abstract void generateDatasets(Map<String, String> page, Storage store) 
                            throws MalformedURLException, RepositoryException;
    
    /**
     * Generate DCAT from cache and write it to the RDF store
     * 
     * @param cache cache
     * @param store RDF store
     * @throws RepositoryException
     * @throws MalformedURLException 
     */
        
    public abstract void generateDcat(Cache cache, Storage store) 
            throws RepositoryException, MalformedURLException;
        
    /**
     * Write DCAT file to output stream
     * 
     * @param out
     * @throws RepositoryException
     * @throws MalformedURLException 
     */
    public void writeDcat(Writer out) throws RepositoryException, MalformedURLException {
        store.startup();

        generateDcat(cache, store);
        
        cache.shutdown();
    
        store.write(out);
        store.shutdown();
    }
    
    /**
     * Constructor
     * 
     * @param caching DB cache file
     * @param storage SDB file to be used as triplestore backend
     * @param base base URL
     */
    public Scraper(File caching, File storage, URL base) {
        cache = new Cache(caching);
        store = new Storage(storage);
        this.base = base;
    }
}
