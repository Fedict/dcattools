/*
 * Copyright (c) 2016, Bart Hanssens <bart.hanssens@fedict.be>
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

import be.fedict.dcat.helpers.Cache;
import be.fedict.dcat.helpers.Page;
import be.fedict.dcat.helpers.Storage;
import be.fedict.dcat.vocab.DCAT;
import be.fedict.dcat.vocab.MDR_LANG;
import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Map;
import javax.swing.text.html.HTML;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.openrdf.model.URI;
import org.openrdf.model.vocabulary.DCTERMS;
import org.openrdf.model.vocabulary.RDF;
import org.openrdf.repository.RepositoryException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Scraper for EANDIS website.
 * 
 * @author Bart Hanssens <bart.hanssens@fedict.be>
 */
public class HtmlEandis extends Html {
    private final Logger logger = LoggerFactory.getLogger(HtmlEandis.class);

    /**
     * Store page containing datasets
     * 
     * @param cache 
     * @throws java.io.IOException 
     */
    private void scrapePage(Cache cache) throws IOException {
        URL front = getBase();
        String lang = getDefaultLang();
        String content = makeRequest(front);
        cache.storePage(front, lang, new Page(front, content));
    }
    
    /**
     * Scrape the site.
     * 
     * @throws IOException 
     */
    @Override
    public void scrape() throws IOException {
        logger.info("Start scraping");
        Cache cache = getCache();
        
        Map<String, Page> front = cache.retrievePage(getBase());
        if (front.keySet().isEmpty()) {
            scrapePage(cache);
            front = cache.retrievePage(getBase());   
        }
        // Calculate the number of datasets
        Page p = front.get(getDefaultLang());
        String datasets = p.getContent();
        Elements tables = Jsoup.parse(datasets).getElementsByTag(HTML.Tag.TABLE.toString());
        logger.info("Found {} datasets on page", String.valueOf(tables.size()));
        
        logger.info("Done scraping");
    }

    
    /**
     * Generate DCAT distribution.
     * 
     * @param store RDF store
     * @param dataset URI
     * @param front URL of the front page
     * @param link link element
     * @param i dataset sequence
     * @param j dist sequence
     * @param lang language code
     * @throws MalformedURLException
     * @throws RepositoryException 
     */
    private void generateDist(Storage store, URI dataset, URL access, 
                                        Element link, int i, int j, String lang) 
                            throws MalformedURLException, RepositoryException {
        String href = link.attr(HTML.Attribute.HREF.toString());
        URL download = makeAbsURL(href);        
     
        URL u = makeDistURL(i + "/" + j + "/" + lang);
        URI dist = store.getURI(u.toString());
        logger.debug("Generating distribution {}", dist.toString());
        
        store.add(dataset, DCAT.DISTRIBUTION, dist);
        store.add(dist, RDF.TYPE, DCAT.A_DISTRIBUTION);
        store.add(dist, DCTERMS.LANGUAGE, MDR_LANG.MAP.get(lang));
        store.add(dist, DCTERMS.TITLE, link.text().trim(), lang);
        store.add(dist, DCAT.ACCESS_URL, access);
        store.add(dist, DCAT.DOWNLOAD_URL, download);
        store.add(dist, DCAT.MEDIA_TYPE, getFileExt(href));
    }
    
    /**
     * Generate one dataset
     * 
     * @param store  RDF store
     * @param URL front
     * @param table HTML table
     * @param i number
     * @param lang language
     * @throws MalformedURLException
     * @throws RepositoryException
     */
    private void generateDataset(Storage store, URL front, Element table, int i, String lang) 
                            throws MalformedURLException, RepositoryException {
        URL u = makeDatasetURL(String.valueOf(i));
        URI dataset = store.getURI(u.toString());  
        logger.debug("Generating dataset {}", dataset.toString());
        
        Element th = table.getElementsByTag(HTML.Tag.TH.toString()).first();
        if (th == null) {
            logger.warn("Empty title, skipping");
            return;
        }
        String title = th.text().trim().toLowerCase();
        String desc = title;
        
        store.add(dataset, RDF.TYPE, DCAT.A_DATASET);
        store.add(dataset, DCTERMS.LANGUAGE, MDR_LANG.MAP.get(lang));
        store.add(dataset, DCTERMS.TITLE, title, lang);
        store.add(dataset, DCTERMS.DESCRIPTION, desc, lang);
        store.add(dataset, DCTERMS.IDENTIFIER, makeHashId(u.toString()));
        store.add(dataset, DCAT.LANDING_PAGE, front);
    
        int j = 0;
        Elements links = table.getElementsByTag(HTML.Tag.A.toString());
        for(Element link : links) {
            generateDist(store, dataset, front, link, i, j++, lang);
        }
    }
    
    /**
     * Generate DCAT Dataset
     * 
     * @param store RDF store
     * @param id dataset id
     * @param page
     * @throws MalformedURLException
     * @throws RepositoryException 
     */
    @Override
    protected void generateDataset(Storage store, String id, Map<String, Page> page) 
                            throws MalformedURLException, RepositoryException {
        String lang = getDefaultLang();
        
        Page p = page.getOrDefault(lang, new Page());
        String html = p.getContent();
        URL front = p.getUrl();
        Elements tables = 
                Jsoup.parse(html).body().getElementsByTag(HTML.Tag.TABLE.toString());
            
        int i = 0;
        for (Element table : tables) {
            generateDataset(store, front, table, i, lang);
            i++;
        }
    }
    
    /**
     * Generate DCAT.
     * 
     * @param cache
     * @param store
     * @throws RepositoryException
     * @throws MalformedURLException 
     */
    @Override
    public void generateDcat(Cache cache, Storage store) 
                            throws RepositoryException, MalformedURLException {
        logger.info("Generate DCAT");
        
        /* Get the list of all datasets */            
        Map<String,Page> page = cache.retrievePage(getBase());
        generateDataset(store, null, page);
        generateCatalog(store);
    }
    
    /**
     * Constructor
     * 
     * @param caching DB cache file
     * @param storage SDB file to be used as triple store backend
     * @param base base URL
     */
    public HtmlEandis(File caching, File storage, URL base) {
        super(caching, storage, base);
        setName("eandis");
    }
}
