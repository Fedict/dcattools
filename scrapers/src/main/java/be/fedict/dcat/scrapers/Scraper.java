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
import be.fedict.dcat.helpers.Fetcher;
import be.fedict.dcat.vocab.DATAGOVBE;
import be.fedict.dcat.vocab.DCAT;
import be.fedict.dcat.vocab.MDR_LANG;
import com.google.common.hash.HashFunction;
import com.google.common.hash.Hashing;
import java.io.File;
import java.io.IOException;
import java.io.Writer;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Properties;
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
public abstract class Scraper extends Fetcher {
    private final Logger logger = LoggerFactory.getLogger(Scraper.class);
    
    public final static String PROP_PREFIX = "be.fedict.dcat.scrapers";
    
    private Properties prop = null;
    private String prefix = "";
    
    private String defLang = "";
    private String[] allLangs = {};
    
    private Cache cache = null;
    private Storage store = null;
    private URL base = null;
    private String name = "";
    
    private final static HashFunction HASHER = Hashing.sha1();
    public final static DateFormat DATEFMT = 
                            new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSSS");
    
    
    /**
     * Get cache
     * 
     * @return local cache file
     */
    public final Cache getCache() {
        return cache;
    }
    
    /**
     * Get triple store
     * 
     * @return RDF triple store
     */
    public final Storage getTripleStore() {
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
    public final void setDefaultLang(String lang) {
        this.defLang = lang;
    }
    /**
     * Get default language
     * 
     * @return language language code
     */
    public final String getDefaultLang() {
        return defLang;
    }
    
    /**
     * Set all languages
     * 
     * @param langs array of language codes
     */
    public final void setAllLangs(String langs[]) {
        this.allLangs = langs;
    }
    
    /**
     * Get all languages
     * 
     * @return array of language codes
     */
    public final String[] getAllLangs() {
        return allLangs;
    }
    
    /**
     * Set name
     * 
     * @param name name
     */
    public final void setName(String name) {
        this.name = name;
    }
    
    /**
     * Get name
     * 
     * @return name
     */
    public final String getName() {
        return name;
    }
    
    /**
     * Get lowercase file extension
     * 
     * @param href
     * @return file extension or empty string
     */
    public String getFileExt(String href) {
        String ext = "";
        int dot = href.lastIndexOf(".");
        if (dot > 0) {
            ext = href.substring(dot+1);
            int q = ext.lastIndexOf("?");
            if (q > 0) {
                ext = ext.substring(0, q);
            }   
        }
        return ext.toLowerCase();
    }
    
    /**
     * Make a hashed ID based upon a string.
     * 
     * @param s
     * @return 
     */
    public final String makeHashId(String s) {
        return HASHER.hashBytes(s.getBytes()).toString();
    }
    
    /**
     * Return an absolute URL
     * 
     * @param rel relative URL
     * @return 
     * @throws MalformedURLException 
     */
    public final URL makeAbsURL(String rel) throws MalformedURLException {
        return new URL(getBase().getProtocol(), getBase().getHost(), rel);
    }
    
    /**
     * Make an URL for a DCAT Catalog 
     * 
     * @return URL
     * @throws MalformedURLException
     */
    public URL makeCatalogURL() throws MalformedURLException {
        return new URL(DATAGOVBE.PREFIX_URI_CAT + "/" + getName());
    }
    
    /**
     * Make an URL for a DCAT Dataset
     * 
     * @param id
     * @return URL
     * @throws MalformedURLException 
     */
    public URL makeDatasetURL(String id) throws MalformedURLException {
        return new URL(DATAGOVBE.PREFIX_URI_DATASET + "/" + id);
    }
    
    /**
     * Make an URL for a DCAT Distribution 
     * 
     * @param id
     * @return URL
     * @throws java.net.MalformedURLException 
     */
    public URL makeDistURL(String id) throws MalformedURLException {
        return new URL(DATAGOVBE.PREFIX_URI_DIST + "/" + id);
    }
  
    /**
     * Make an URL for an organization 
     * 
     * @param id
     * @return URL
     * @throws java.net.MalformedURLException 
     */
    public final URL makeOrgURL(String id) throws MalformedURLException {
        return new URL(DATAGOVBE.PREFIX_URI_ORG + "/" + id);
    }
    
    /**
     * Set property from config file and property prefix.
     * 
     * @param prop
     * @param prefix 
     */
    public final void setProperties(Properties prop, String prefix) {
        this.prop = prop;
        this.prefix = prefix;
    }
    
    /**
     * Get property from config file.
     * 
     * @param name
     * @return property value
     */
    public final String getProperty(String name) {
        String p = prefix + "." + name;

        String value = prop.getProperty(p);
        if (value == null) {
            logger.error("No property {}", p);
        }
        return value;
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
     * @param store RDF store
     * @param catalog catalog URI
     * @throws org.openrdf.repository.RepositoryException 
     */
    public void generateCatalogInfo(Storage store, URI catalog) 
                                                    throws RepositoryException {
        store.add(catalog, DCTERMS.DESCRIPTION, "Converted by Fedict's converter", "en");
        store.add(catalog, DCTERMS.MODIFIED, DATEFMT.format(new Date()));
        store.add(catalog, DCTERMS.LICENSE, DATAGOVBE.LICENSE_CC0);
        store.add(catalog, FOAF.HOMEPAGE, getBase());
                
        String[] langs = getAllLangs();
        for (String lang : langs) {
            store.add(catalog, DCTERMS.LANGUAGE, MDR_LANG.MAP.get(lang));
        }
    }

        
    /**
     * Generate DCAT Catalog.
     * 
     * @param store RDF store
     * @throws RepositoryException 
     * @throws java.net.MalformedURLException 
     */
    public void generateCatalog(Storage store) 
                            throws RepositoryException, MalformedURLException {
        URI catalog = store.getURI(makeCatalogURL().toString());
        store.add(catalog, RDF.TYPE, DCAT.A_CATALOG);
        
        List<URI> uris = store.query(DCAT.A_DATASET);        
        for (URI u : uris){
            store.add(catalog, DCAT.DATASET, u);
        }
        generateCatalogInfo(store, catalog);
    }
    
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
     * @param storage SDB file to be used as triple store backend
     * @param base base URL
     */
    public Scraper(File caching, File storage, URL base) {
        cache = new Cache(caching);
        store = new Storage(storage);
        this.base = base;
    }
}