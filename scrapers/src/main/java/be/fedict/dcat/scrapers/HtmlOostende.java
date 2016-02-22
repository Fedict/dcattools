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
 * Scraper for Oostende DO2 website.
 * 
 * @author Bart Hanssens <bart.hanssens@fedict.be>
 */
public class HtmlOostende extends Html {
    private final Logger logger = LoggerFactory.getLogger(HtmlOostende.class);

    private final static String CONTENT_ID = "content";
    private final static String DIV_DESC = "opendata_long";
    private final static String LINK_DATASETS = "ul.dataviews li.item a:eq(1)";
    private final static String LINK_DISTS = "div.odsub a.file";
    private final static String LIST_CATS = "ul.listcategorien li a";
    
    /**
     * Generate DCAT distribution.
     * 
     * @param store RDF store
     * @param dataset URI
     * @param front URL of the front page
     * @param link link element
     * @param i dist sequence
     * @param lang language code
     * @throws MalformedURLException
     * @throws RepositoryException 
     */
    private void generateDist(Storage store, URI dataset, URL access, 
                                        Element link, int i, String lang) 
                            throws MalformedURLException, RepositoryException {
        String href = link.attr(HTML.Attribute.HREF.toString());
        URL download = makeAbsURL(href);        
     
        URL u = makeDistURL(access + "/" + i + "/" + lang);
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
        
        Page p = page.getOrDefault("", new Page());
        String html = p.getContent();
        URL u = p.getUrl();
        
        Element content = Jsoup.parse(html).body().getElementById(CONTENT_ID);
        
        URI dataset = store.getURI(u.toString());  
        logger.debug("Generating dataset {}", dataset.toString());
        
        Element h1 = content.getElementsByTag(HTML.Tag.H1.toString()).first();
        if (h1 == null) {
            logger.warn("Empty title, skipping");
            return;
        }
        String title = h1.text().trim();
        
        Element div = content.getElementsByClass(DIV_DESC).first();
        String desc = div.text();
        
        store.add(dataset, RDF.TYPE, DCAT.A_DATASET);
        store.add(dataset, DCTERMS.LANGUAGE, MDR_LANG.MAP.get(lang));
        store.add(dataset, DCTERMS.TITLE, title, lang);
        store.add(dataset, DCTERMS.DESCRIPTION, desc, lang);
        store.add(dataset, DCTERMS.IDENTIFIER, makeHashId(u.toString()));
        store.add(dataset, DCAT.LANDING_PAGE, u);
    
        Elements cats = content.select(LIST_CATS);
        for (Element cat : cats) {
            store.add(dataset, DCAT.KEYWORD, cat.text(), lang);
        }
        
        int i = 0;
        Elements dists = content.select(LINK_DISTS);
        for(Element dist : dists) {
            generateDist(store, dataset, u, dist, ++i, lang);
        }

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
        Page p = front.get("");
        String datasets = p.getContent();
        Elements links = Jsoup.parse(datasets).select(LINK_DATASETS);
        logger.info("Found {} datasets on page", String.valueOf(links.size()));
        logger.info("Start scraping (waiting between requests)");
        
        int i = 0;
        for (Element link : links) {
            String href = link.attr(HTML.Attribute.HREF.toString());  
            URL u = new URL(href);
            Map<String, Page> page = cache.retrievePage(u);
            if (page.isEmpty()) {
                sleep();
                if (++i % 100 == 0) {
                    logger.info("Download {}...", Integer.toString(i));
                }
                try {
                    String html = makeRequest(u);
                    cache.storePage(u, "", new Page(u, html));
                } catch (IOException ex) {
                    logger.error("Failed to scrape {}", u);
                }
            }
        }
        logger.info("Done scraping");
    }

    
    /**
     * Store page containing datasets
     * 
     * @param cache 
     * @throws java.io.IOException 
     */
    private void scrapePage(Cache cache) throws IOException {
        URL front = getBase();
        String content = makeRequest(front);
        cache.storePage(front, "", new Page(front, content));
    }
    
    /**
     * Generate DCAT catalog information.
     * 
     * @param store
     * @param catalog
     * @throws RepositoryException 
     */
    @Override
    public void generateCatalogInfo(Storage store, URI catalog) 
                                                    throws RepositoryException {
        super.generateCatalogInfo(store, catalog);
        store.add(catalog, DCTERMS.TITLE, "DCAT export Oostende D02", "en");
        store.add(catalog, DCTERMS.LANGUAGE, MDR_LANG.NL);
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
     * HTML scraper Oostende DO2.
     * 
     * @param caching
     * @param storage
     * @param base 
     */
    public HtmlOostende(File caching, File storage, URL base) {
        super(caching, storage, base);
        setName("oostende");
    }
}
