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
import be.fedict.dcat.helpers.Page;
import be.fedict.dcat.vocab.DCAT;
import be.fedict.dcat.vocab.MDR_LANG;
import be.fedict.dcat.vocab.VCARD;
import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.json.JsonReader;
import javax.json.JsonString;
import org.openrdf.model.URI;
import org.openrdf.model.vocabulary.DCTERMS;
import org.openrdf.model.vocabulary.FOAF;
import org.openrdf.model.vocabulary.RDF;
import org.openrdf.repository.RepositoryException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Scraper CKAN
 * 
 * @author Bart Hanssens <bart.hanssens@fedict.be>
 */
public abstract class Ckan extends Scraper {
    private final Logger logger = LoggerFactory.getLogger(Ckan.class);
   
    // CKAN JSON fields
    public final static String RESULT = "result";
    public final static String SUCCESS = "success";
    
    public final static String ID = "id";
    public final static String AUTHOR = "author";
    public final static String AUTHOR_EML = "author_email";
    public final static String CREATED = "created";
    public final static String FORMAT = "format";
    public final static String LICENSE_URL = "license_url";
    public final static String MAINT = "maintainer";
    public final static String MAINT_EML = "maintainer_email";
    public final static String META_CREATED = "metadata_created";
    public final static String META_MODIFIED = "metadata_modified";
    public final static String MODIFIED = "last_modified";
    public final static String NAME = "name";
    public final static String NOTES = "notes";
    public final static String ORGANIZATION = "organization";
    public final static String IS_ORG = "is_organization";
    public final static String RESOURCES = "resources";
    public final static String TAGS = "tags";
    public final static String TITLE = "title";
    public final static String URL = "url";
    
    public final static String EXTRA = "extras";
    
    public final static String KEY = "key";
    public final static String VALUE = "value";

    
    // CKAN API
    public final static String API_LIST = "/api/3/action/package_list";
    public final static String API_PKG = "/api/3/action/package_show?id=";
    public final static String API_ORG = "/api/3/action/organization_show?id=";
    public final static String API_RES = "/api/3/action/resource_show?id=";
    public final static String DATASET = "/dataset";
    
    /**
     * Make an URL for a CKAN Package (DCAT Dataset) 
     * 
     * @param id
     * @return URL
     * @throws java.net.MalformedURLException 
     */
    protected URL ckanDatasetURL(String id) throws MalformedURLException {
        return new URL(getBase(), Ckan.API_PKG + id);
    }
    
    /**
     * Get URL of a CKAN resource (DCAT Distribution).
     * 
     * @param id
     * @return URL
     * @throws MalformedURLException 
     */
    protected URL ckanResourceURL(String id) throws MalformedURLException {
        return new URL(getBase(), Ckan.API_RES + id);
    }
    
    /**
     * Get URL of a CKAN organization (DCAT Publisher).
     * 
     * @param id
     * @return URL
     * @throws MalformedURLException 
     */
    protected URL ckanOrganizationURL(String id) throws MalformedURLException {
        return new URL(getBase(), Ckan.API_ORG + id);
    }
    
    /**
     * Get URL of a CKAN page.
     * 
     * @param id
     * @return URL
     * @throws MalformedURLException 
     */
    protected URL ckanPageURL(String id) throws MalformedURLException {        
        return new URL(getBase(), Ckan.DATASET + "/" + id);
    }
    
    
    /**
     * Generate URL from a string.
     * 
     * @param str
     * @return URL
     * @throws MalformedURLException 
     */
    protected URL getHashUrl(String str) throws MalformedURLException {
        return new URL(getBase(), makeHashId(str));
    }
    
    /**
     * Parse a CKAN string and store it in the RDF store.
     * 
     * @param store RDF store
     * @param uri RDF subject URI
     * @param obj JsonObject
     * @param field CKAN field name 
     * @param property RDF property
     * @param lang language
     * @throws RepositoryException 
     */
    protected void parseString(Storage store, URI uri, JsonObject obj, 
            String field, URI property, String lang) throws RepositoryException {
        String s = obj.getString(field, "");
        if (! s.isEmpty()) {
            if (lang != null) {
                store.add(uri, property, s, lang);
            } else {
                store.add(uri, property, s);
            }
        }
    }
    
    /**
     * Parse a CKAN string and store it in the RDF store
     * 
     * @param store RDF store
     * @param uri RDF subject URI
     * @param obj JsonObject
     * @param field CKAN field name 
     * @param property RDF property
     * @throws RepositoryException 
     * @throws MalformedURLException 
     */
    protected void parseURI(Storage store, URI uri, JsonObject obj, String field, 
                URI property) throws RepositoryException, MalformedURLException {
        String s = obj.getString(field, "");
        if (! s.isEmpty()) {
            URL url = new URL(s);
            store.add(uri, property, url);
        }
    }
    
   /**
     * Parse a CKAN date and store it in the RDF store
     * 
     * @param store RDF store
     * @param uri RDF subject URI
     * @param obj JsonObject
     * @param field CKAN field name 
     * @param property RDF property
     * @throws RepositoryException 
     */
    protected void parseDate(Storage store, URI uri, JsonObject obj, 
                        String field, URI property) throws RepositoryException {
        String s = obj.getString(field, "");
        if (! s.isEmpty()) {
            try {
                store.add(uri, property, DATEFMT.parse(s));
            } catch (ParseException ex) {
                logger.warn("Could not parse date {}", s, ex);
            }
        }
    }
    
    /**
     * Parse a CKAN contact and store it in the RDF store
     * 
     * @param store RDF store
     * @param uri RDF subject URI
     * @param obj JsonObject
     * @param field CKAN field name
     * @param field2 CKAN field email
     * @param property RDF property
     * @throws RepositoryException 
     */
    protected void parseContact(Storage store, URI uri, JsonObject obj,
            String field, String field2, URI property) throws RepositoryException{
        String name = obj.getString(field, "");
        String email = obj.getString(field2, "");
       
        String v = "";
        try {
            v = getHashUrl(name + email).toString();
        } catch (MalformedURLException e) {
            logger.error("Could not generate hash url", e);
        }
       
        if (!name.isEmpty() || !email.isEmpty()) {
            URI vcard = store.getURI(v);
            store.add(uri, DCAT.CONTACT_POINT, vcard);
            store.add(vcard, RDF.TYPE, VCARD.A_ORGANIZATION);
            if (! name.isEmpty()) {
                store.add(vcard, VCARD.HAS_FN, name);
            }
            if(! email.isEmpty()) {
                store.add(vcard, VCARD.HAS_EMAIL, store.getURI("mailto:" + email));
            }
        }
    }
    
    /**
     * Parse CKAN dataset in JSON format.
     * 
     * @param store RDF store
     * @param uri RDF subject
     * @param lang language
     * @param json JSON object with CKAN data
     * @throws RepositoryException 
     * @throws MalformedURLException 
     */
    protected void ckanGeneral(Storage store, URI uri, JsonObject json, String lang) 
                            throws RepositoryException, MalformedURLException {
        parseString(store, uri, json, Ckan.ID, DCTERMS.IDENTIFIER, null);
        parseString(store, uri, json, Ckan.TITLE, DCTERMS.TITLE, lang); 
        parseString(store, uri, json, Ckan.NOTES, DCTERMS.DESCRIPTION, lang);
        
        parseDate(store, uri, json, Ckan.META_CREATED, DCTERMS.CREATED);
        parseDate(store, uri, json, Ckan.META_MODIFIED, DCTERMS.MODIFIED);
        
        parseURI(store, uri, json, Ckan.LICENSE_URL , DCTERMS.LICENSE);
        
        parseContact(store, uri, json, Ckan.AUTHOR, Ckan.AUTHOR_EML, DCAT.CONTACT_POINT);
        parseContact(store, uri, json, Ckan.MAINT, Ckan.MAINT_EML, DCAT.CONTACT_POINT);       
    }
    
    /**
     * Parse CKAN tags in JSON format.
     * 
     * @param store RDF store
     * @param uri RDF subject
     * @param json JSON object with Ckan data
     * @param lang language
     * @throws RepositoryException 
     */
    protected void ckanTags(Storage store, URI uri, JsonObject json, String lang) 
                                                    throws RepositoryException {
        JsonArray arr = json.getJsonArray(Ckan.TAGS);
        
        for (JsonObject obj : arr.getValuesAs(JsonObject.class)) {
            parseString(store, uri, obj, Ckan.NAME, DCAT.KEYWORD, lang);
        }
    }
    
    /**
     * Parse CKAN resources.
     * 
     * @param store RDF store
     * @param dataset dataset
     * @param json JSON
     * @param lang language
     * @throws RepositoryException
     * @throws MalformedURLException 
     */
    protected void ckanResources(Storage store, URI dataset, JsonObject json, String lang) 
                                throws RepositoryException, MalformedURLException {
        
        /* CKAN page / access page */
        URL access = ckanPageURL(json.getString(Ckan.ID, ""));
        
        JsonArray arr = json.getJsonArray(Ckan.RESOURCES);
        
        for (JsonObject obj : arr.getValuesAs(JsonObject.class)) {
            String id = obj.getString(Ckan.ID, "");
            URI dist = store.getURI(makeDistURL(id).toString());
            logger.debug("Generating distribution {}", dist.toString());
                    
            store.add(dataset, DCAT.DISTRIBUTION, dist);
            store.add(dist, RDF.TYPE, DCAT.A_DISTRIBUTION);
        
            parseString(store, dist, obj, Ckan.ID, DCTERMS.IDENTIFIER, null);
            parseString(store, dist, obj, Ckan.NAME, DCTERMS.TITLE, lang);
            parseDate(store, dist, obj, Ckan.CREATED, DCTERMS.CREATED);
            parseDate(store, dist, obj, Ckan.MODIFIED, DCTERMS.MODIFIED);
            parseString(store, dist, obj, Ckan.FORMAT, DCAT.MEDIA_TYPE, null);
            parseURI(store, dist, obj, Ckan.URL, DCAT.DOWNLOAD_URL);
            
            store.add(dist, DCAT.ACCESS_URL, access);
            store.add(dist, DCTERMS.LANGUAGE, MDR_LANG.MAP.get(lang));
        }
    }
   
    /**
     * Parse CKAN organization
     * 
     * @param store
     * @param uri
     * @param json
     * @param lang
     * @throws RepositoryException
     * @throws MalformedURLException 
     */
    protected void ckanOrganization(Storage store, URI uri, JsonObject json, String lang)
                               throws RepositoryException, MalformedURLException {
        if(! json.isNull(Ckan.ORGANIZATION)) {
            JsonObject obj = json.getJsonObject(Ckan.ORGANIZATION);
        
            if (obj.getBoolean(Ckan.IS_ORG)) {
                String s = obj.getString(Ckan.ID, "");
                URI org = store.getURI(ckanOrganizationURL(s).toString());
                store.add(uri, DCTERMS.PUBLISHER, org);
                store.add(org, RDF.TYPE, FOAF.ORGANIZATION);
        
                parseString(store, org, obj, Ckan.NAME, FOAF.NAME, lang);
            }
        }
    }
    
    /**
     * Parse CKAN extra fields.
     * 
     * @param store RDF store
     * @param uri
     * @param json
     * @param lang language
     * @throws RepositoryException
     * @throws MalformedURLException 
     */
    protected abstract void ckanExtras(Storage store, URI uri, JsonObject json, String lang)
                               throws RepositoryException, MalformedURLException;
    
    /**
     * Generate DCAT Dataset
     * 
     * @param store RDF store
     * @param id
     * @param page
     * @throws MalformedURLException
     * @throws RepositoryException 
     */
    public void generateDataset(Storage store, String id, Map<String,Page> page) 
                            throws MalformedURLException, RepositoryException {
        String lang = getDefaultLang();
        
        Page p = page.getOrDefault(lang, new Page());
        JsonReader reader = Json.createReader(new StringReader(p.getContent()));
        JsonObject obj = reader.readObject();
        
        String ckanid = obj.getString(Ckan.ID, "");
        URI dataset = store.getURI(makeDatasetURL(ckanid).toString());
        logger.info("Generating dataset {}", dataset.toString());
        
        store.add(dataset, RDF.TYPE, DCAT.A_DATASET);
        store.add(dataset, DCTERMS.LANGUAGE, MDR_LANG.MAP.get(lang));
        
        /* Parse different sections of CKAN JSON */
        ckanGeneral(store, dataset, obj, lang);
        ckanTags(store, dataset, obj, lang);
        ckanResources(store, dataset, obj, lang);
        ckanOrganization(store, dataset, obj, lang);
        ckanExtras(store, dataset,obj, lang);
    }
   
    /**
     * Generate DCAT.
     * 
     * @param cache
     * @param store RDF store
     * @throws RepositoryException
     * @throws MalformedURLException 
     */
    @Override
    public void generateDcat(Cache cache, Storage store) 
                            throws RepositoryException, MalformedURLException {
        logger.info("Generate DCAT");
        
        /* Get the list of all datasets */
        List<URL> urls = cache.retrieveURLList();
        for (URL u : urls) {
            Map<String, Page> page = cache.retrievePage(u);
            generateDataset(store, null, page);
        }
        generateCatalog(store);
    }
  
    /**
     * Get a CKAN package (DCAT Dataset).
     * 
     * @param url
     * @return JsonObject containing CKAN Package or NULL
     * @throws IOException 
     */
    protected JsonObject scrapePackage(URL url) throws IOException {
        JsonObject obj = makeJsonRequest(url);
        if (obj.getBoolean(Ckan.SUCCESS)) {
            return obj.getJsonObject(Ckan.RESULT);
        }
        return null;
    }
    
    /**
     * Get the list of all the CKAN packages (DCAT Dataset).
     * 
     * @return List of URLs
     * @throws MalformedURLException
     * @throws IOException 
     */
    protected List<URL> scrapePackageList() throws MalformedURLException, IOException {
        List<URL> urls = new ArrayList<>();
        URL getPackages = new URL(getBase(), Ckan.API_LIST);
        
        JsonObject obj = makeJsonRequest(getPackages);
        if (! obj.getBoolean(Ckan.SUCCESS)) {
            return urls;
        }
        JsonArray arr = obj.getJsonArray(Ckan.RESULT);
        for (JsonString str : arr.getValuesAs(JsonString.class)) {
            urls.add(ckanDatasetURL(str.getString()));
        }
        return urls;
    }
    
    /**
     * Fetch all metadata from the CKAN repository.
     * 
     * @throws IOException 
     */
    @Override
    public void scrape() throws IOException {
        logger.info("Start scraping");
        Cache cache = getCache();
        String lang = getDefaultLang();
        
        List<URL> urls = cache.retrieveURLList();
        if (urls.isEmpty()) {
            urls = scrapePackageList();
            cache.storeURLList(urls);
        }
        urls = cache.retrieveURLList();
        
        logger.info("Found {} CKAN packages", String.valueOf(urls.size()));
        logger.info("Start scraping (waiting between requests)");
        int i = 0;
        for (URL u : urls) {
            Map<String, Page> page = cache.retrievePage(u);
            if (page.isEmpty()) {
                sleep();
                if (++i % 100 == 0) {
                    logger.info("Package {}...", Integer.toString(i));
                }
                try {
                    JsonObject obj = scrapePackage(u);
                    cache.storePage(u, lang, new Page(u, obj.toString()));
                } catch (IOException e) {
                    logger.warn("Failed to scrape {}", u);
                }
            }
        }
        logger.info("Done scraping");
    }
    
    
    /**
     * CKAN scraper.
     * 
     * @param caching local cache file
     * @param storage local triple store file
     * @param base URL of the CKAN site
     */
    public Ckan(File caching, File storage, URL base) {
        super(caching, storage, base);
   } 
}
