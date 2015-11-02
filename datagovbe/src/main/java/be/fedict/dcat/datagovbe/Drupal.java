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
package be.fedict.dcat.datagovbe;

import be.fedict.dcat.helpers.Storage;
import be.fedict.dcat.vocab.DCAT;
import be.fedict.dcat.vocab.DATAGOVBE;
import com.google.common.collect.ListMultimap;
import java.io.IOException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.json.Json;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import javax.net.ssl.SSLContext;
import org.apache.http.Header;
import org.apache.http.HttpHeaders;
import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.client.fluent.Executor;
import org.apache.http.client.fluent.Request;
import org.apache.http.conn.ssl.TrustSelfSignedStrategy;
import org.apache.http.entity.ContentType;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.client.LaxRedirectStrategy;
import org.apache.http.ssl.SSLContexts;
import org.openrdf.model.URI;
import org.openrdf.model.vocabulary.DCTERMS;
import org.openrdf.repository.RepositoryException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Drupal REST service class.
 * 
 * @author Bart Hanssens <bart.hanssens@fedict.be>
 */
public class Drupal {
    private final Logger logger = LoggerFactory.getLogger(Drupal.class);
    
    public final static String PROP_PREFIX = "be.fedict.datagovbe7";
     
    public final static String NODE = "/node";
    public final static String TOKEN = "/restws/session/token";
    public final static String X_TOKEN = "X-CSRF-Token";
    
    public final static String POST = "Post";
    public final static String PUT = "Put";
    
    // Drupal fields
    public final static String TITLE = "title";
    public final static String BODY = "body";
    public final static String LANGUAGE = "language";
    public final static String URL = "url";
    public final static String AUTHOR = "author";
    public final static String MODIFIED = "changed_date";
    public final static String FLD_CAT = "field_category";
    public final static String FLD_DETAILS = "field_details_";
    public final static String FLD_FORMAT = "field_file_type";
    public final static String FLD_GEO = "field_geo_coverage";
    public final static String FLD_KEYWORDS = "field_keywords";
    public final static String FLD_LICENSE = "field_license";
    public final static String FLD_LINKS = "field_links_";
    public final static String FLD_ID = "field_id";
    public final static String FLD_UPSTAMP = "field_upstamp";
   
    public final static String ID = "id";
    public final static String TYPE = "type";
    public final static String TYPE_DATA = "dataset";
    
    public final static String FORMAT = "format";
    public final static String FORMAT_HTML = "filtered_html";
    public final static String SOURCE = "source";
    public final static String SUMMARY = "summary";
    public final static String VALUE = "value";
    
    public final static String TAXO_PREFIX = "http://data.gov.be/en/taxonomy";
   
    public final static Pattern SHORTLINK = 
                            Pattern.compile("/([0-9]+)>; rel=\"shortlink\"");
                    
    private final String[] langs;
    private final URL url;
    private String userid;
    
    private Executor exec;
    private HttpHost proxy = null;
    private HttpHost host = null;
    private String token = null;
    
    private SimpleDateFormat iso = new SimpleDateFormat("yyyy-MM-dd");
   
    /**
     * Prepare a POST or PUT action.
     * 
     * @param method POST or PUT
     * @param relpath relative path
     * @return 
     */
    private Request prepare(String method, String relpath) {
        String u = this.url.toString() + relpath;
        
        Request r = method.equals(Drupal.POST) ? Request.Post(u) : Request.Put(u);

        r.setHeader(HttpHeaders.ACCEPT, ContentType.APPLICATION_JSON.getMimeType())
         .setHeader(Drupal.X_TOKEN, token);
        
        if (proxy != null) {
            r.viaProxy(proxy);
        }
        return r;
    }
    
    
    /**
     * Decode "\\u..." to a character.
     * 
     * @param s
     * @return 
     */
    private String decode(String s) {
        return new String(s.getBytes(StandardCharsets.UTF_8), StandardCharsets.ISO_8859_1);     
    }
    
    /**
     * Get multiple values from map structure.
     * 
     * @param map
     * @param prop
     * @param lang
     * @return 
     */
    private List<String> getMany(Map<URI, ListMultimap<String, String>> map, 
                                                        URI prop, String lang) {
        List<String> res = new ArrayList<>();
        
        ListMultimap<String, String> multi = map.get(prop);
        if (multi != null && !multi.isEmpty()) {
            List<String> list = multi.get(lang);
            if (list != null && !list.isEmpty()) {
                res = list;
            }
        }
        return res;
    }
    
    /**
     * Get one value from map structure.
     * 
     * @param map
     * @param prop
     * @param lang
     * @return 
     */
    private String getOne(Map<URI, ListMultimap<String, String>> map, URI prop, 
                                                                String lang) {
        String res = "";
        
        ListMultimap<String, String> multi = map.get(prop);
        if (multi != null && !multi.isEmpty()) {
            List<String> list = multi.get(lang);
            if (list != null && !list.isEmpty()) {
                res = list.get(0);
            }
        }
        return res;
    }
    

    /**
     * Add a DCAT Theme / category
     * 
     * @param dataset
     * @param property
     */
    private JsonArrayBuilder getCategories(
            Map<URI, ListMultimap<String, String>> dataset, URI property) {
        JsonArrayBuilder arr = Json.createArrayBuilder();
        
        List<String> themes = getMany(dataset, property, ""); 
        for(String theme : themes) {
            if (theme.startsWith(Drupal.TAXO_PREFIX)) {
                String id = theme.substring(theme.lastIndexOf("/") + 1);
                arr.add(Json.createObjectBuilder().add(Drupal.ID, id).build());
            }
        }
        return arr;
    }
 
    
    /**
     * Check if dataset with ID already exists on drupal site.
     * 
     * @param id
     * @param lang
     * @return 
     */
    private String checkExists(String id, String lang) {
        String node = "";
        
        String u = this.url.toString() + "/" + lang + "/" + Drupal.TYPE_DATA + "/" + id;
        
        Request r = Request.Head(u);
        if (proxy != null) {
            r.viaProxy(proxy);
        }
        
        try {
            HttpResponse resp = exec.execute(r).returnResponse();
            Header header = resp.getFirstHeader("Link");
            if (header != null) {
                Matcher m = SHORTLINK.matcher(header.getValue());
                if (m.find()) {
                    node = m.group(1);
                    logger.info("Dataset {} exists, node {}", id, node);
                }
            }
        } catch (IOException ex) {
            logger.error("Exception getting dataset {}", id);
        }

        return node;
    }
    
    /**
     * Check if node or translations exist.
     * 
     * @param builder
     * @param id
     * @param lang
     * @return 
     */
    private String checkExistsTrans(JsonObjectBuilder builder, String id, String lang) {
        String node = checkExists(id, lang);
            
        // Exists in another language ?
        if (node.isEmpty()) {
            for (String otherlang : langs) {
                if (!otherlang.equals(lang)) {
                    node = checkExists(id, otherlang);
                    if (! node.isEmpty()) {
                        builder.add(Drupal.SOURCE, Json.createObjectBuilder()
                                .add(otherlang, Integer.parseInt(node)));
                        node = "";
                    }
                }
            }
        }
        return node;
    }
    
    /**
     * Add a dataset to Drupal form
     * 
     * @param builder
     * @param dataset
     * @param lang 
     */
    private void addDataset(JsonObjectBuilder builder, Map<URI, 
                            ListMultimap<String, String>> dataset, String lang) {
        String id = getOne(dataset, DCTERMS.IDENTIFIER, "");
        String title = getOne(dataset, DCTERMS.TITLE, lang);
        String desc = getOne(dataset, DCTERMS.DESCRIPTION, lang);
        if (desc.isEmpty()) {
            desc = title;
        }
        
        Date modif = new Date();
        String m = getOne(dataset, DCTERMS.MODIFIED, "");
        if (! m.isEmpty() && (m.length() >= 10)) {
            try {
                modif = iso.parse(m.substring(0, 10));
            } catch (ParseException ex) {
                logger.error("Exception parsing {} as date", m);
            }
        }
        
        JsonArrayBuilder keywords = Json.createArrayBuilder();
        List<String> words = getMany(dataset, DCAT.KEYWORD, lang);
        for(String word : words) {
            keywords.add(word);
        }
        
        builder.add(Drupal.TYPE, Drupal.TYPE_DATA)
                .add(Drupal.LANGUAGE, lang)
                .add(Drupal.AUTHOR, Json.createObjectBuilder().add(Drupal.ID, userid)) 
                .add(Drupal.TITLE, title)
                .add(Drupal.BODY, Json.createObjectBuilder()
                        .add(Drupal.VALUE, desc)
                        .add(Drupal.SUMMARY, "")
                        .add(Drupal.FORMAT, Drupal.FORMAT_HTML))
                .add(Drupal.FLD_UPSTAMP, modif.getTime()/1000L)
                .add(Drupal.FLD_LICENSE, getCategories(dataset, DATAGOVBE.LICENSE))
                .add(Drupal.FLD_CAT, getCategories(dataset, DATAGOVBE.THEME))
                .add(Drupal.FLD_GEO, getCategories(dataset, DATAGOVBE.SPATIAL))
                .add(Drupal.FLD_KEYWORDS, keywords)
                .add(Drupal.FLD_ID, id);
    }

    /**
     * 
     * @param dist
     * @param property
     * @return 
     */
    private String getLink(Map<URI, ListMultimap<String, String>> dist, URI property) {
        String link = "";
        
        String l = getOne(dist, property, "");
        if (! l.isEmpty()) {
            link = l.replaceAll(" ", "%20");
        }
        if (link.length() > 255) {
            logger.warn("Download URL too long ({}): {} ", l.length(), l);
        }
        return link;
    }

    /**
     * Add a dataset to the Drupal website.
     * 
     * @param store RDF store
     * @param uri identifier of the dataset
     * @throws RepositoryException
     */
    private void add(Storage store, URI uri) throws RepositoryException {
        Map<URI, ListMultimap<String, String>> dataset = store.queryProperties(uri);
        
        if (dataset.isEmpty()) {
            logger.warn("Empty dataset for {}", uri.stringValue());
            return;
        }
                
        //List<String> langs = getMany(dataset, DCTERMS.LANGUAGE, "");
        //String[] langs = new String[]{ "nl" };
        
        for(String lang : langs) {
            JsonObjectBuilder builder = Json.createObjectBuilder();
            
            addDataset(builder, dataset, lang);

            // Get DCAT distributions
            List<String> dists = getMany(dataset, DCAT.DISTRIBUTION, "");
            JsonArrayBuilder accesses = Json.createArrayBuilder();
            JsonArrayBuilder downloads = Json.createArrayBuilder();
            
            for (String d : dists) {
                Map<URI, ListMultimap<String, String>> dist = 
                        store.queryProperties(store.getURI(d));
                
                // Landing page / page(s) with more info on dataset
                String access = getLink(dist, DCAT.ACCESS_URL);
                if (! access.isEmpty()) {
                    accesses.add(Json.createObjectBuilder().add(Drupal.URL, access));
                }
        
                // Download URL
                String download = getLink(dist, DCAT.DOWNLOAD_URL);
                if (! download.isEmpty()) {
                    downloads.add(Json.createObjectBuilder().add(Drupal.URL, download));
                }
                builder.add(Drupal.FLD_FORMAT, getCategories(dist, DATAGOVBE.MEDIA_TYPE));
            }
            builder.add(Drupal.FLD_DETAILS + lang, accesses);
            builder.add(Drupal.FLD_LINKS + lang, downloads);
            
            // Add new or update existing dataset ?
            String id = getOne(dataset, DCTERMS.IDENTIFIER, "");
            String node = checkExistsTrans(builder, id, lang);
    
            // Build the JSON array
            JsonObject obj = builder.build();
            logger.debug(obj.toString());

            Request r = node.isEmpty() 
                            ? prepare(Drupal.POST, Drupal.NODE)
                            : prepare(Drupal.PUT, Drupal.NODE + "/" + node);              
            r.bodyString(obj.toString(), ContentType.APPLICATION_JSON);
            
            try {            
                String resp = exec.authPreemptive(host)
                                    .execute(r)
                                    .returnContent().asString();
            } catch (IOException ex) {
                logger.error("Could not update {}", uri.toString(), ex);
            }
        }     
    }
    
    /**
     * Set username and password.
     * 
     * @param user username
     * @param password password
     * @param userid drupal user ID
     */
    public void setUserPassID(String user, String password, String userid) {
        exec = exec.clearAuth().clearCookies()
                    .auth(host, user, password);
        this.userid = userid; 
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
     * Get CSRF Token
     * 
     * @throws IOException
     */
    private void getCSRFToken() throws IOException {
        Request r = Request.Get(this.url.toString() + Drupal.TOKEN)
                           .setHeader(HttpHeaders.CACHE_CONTROL, "no-cache");
        
        if (proxy != null) {
            r.viaProxy(proxy);
        }
        token = exec.authPreemptive(host)
                    .execute(r)
                    .returnContent().asString();
        logger.debug("CSRF Token is {}", token);
    }
    
    /**
     * Update site
     * 
     * @param store triple store containing the info
     * @throws IOException 
     * @throws org.openrdf.repository.RepositoryException 
     */
    public void update(Storage store) throws IOException, RepositoryException {
        List<URI> datasets = store.query(DCAT.A_DATASET);
        
        logger.info("Updating {} datasets...", Integer.toString(datasets.size()));
        
        getCSRFToken();
        
        int i = 0;
        for (URI d : datasets) {
            add(store, d);
            if (++i % 100 == 0) {
                logger.info("Updated {}", Integer.toString(i));
            }
        }
        logger.info("Done updating datasets");
    }
    
    
    /**
     * Drupal REST Service.
     * 
     * @param url service endpoint
     * @param langs languages
     */
    public Drupal(URL url, String[] langs) {
        this.url = url;
        this.langs = langs;
        this.host = new HttpHost(url.getHost());
        
        Executor e = null;
        try {
            /* Self signed certificates are OK */
            SSLContext ctx = SSLContexts.custom()
                    .loadTrustMaterial(new TrustSelfSignedStrategy()).build();
            
            /* Allow redirect on POST */
            CloseableHttpClient client = HttpClients.custom()
                    .setRedirectStrategy(new LaxRedirectStrategy())
                    .setSSLContext(ctx)
                    .build();
            e = Executor.newInstance(client);
        } catch (NoSuchAlgorithmException ex) {
            logger.error("Algo error", ex);
        } catch (KeyStoreException|KeyManagementException ex) {
            logger.error("Store exception", ex);
        }
        this.exec = e;
    }
}
