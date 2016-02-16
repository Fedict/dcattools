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

import be.fedict.dcat.helpers.Cache;
import be.fedict.dcat.helpers.Page;
import be.fedict.dcat.helpers.Storage;
import be.fedict.dcat.vocab.DCAT;
import be.fedict.dcat.vocab.MDR_LANG;
import be.fedict.dcat.vocab.MDR_THEME;
import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Map;
import javax.swing.text.html.HTML;
import javax.swing.text.html.HTML.Attribute;
import javax.swing.text.html.HTML.Tag;
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
 * Scraper FPS Mobility
 * 
 * @author Bart Hanssens <bart.hanssens@fedict.be>
 */
public class HtmlFodMobilit extends Html {
    private final Logger logger = LoggerFactory.getLogger(HtmlFodMobilit.class);

    public final static String LANG_LINK = "language-link";
        
    /**
     * Switch to another language
     * 
     * @param lang
     * @return
     * @throws IOException 
     */
    private URL switchLanguage(String lang) throws IOException {
        URL base = getBase();
        
        String front= makeRequest(base);

        Elements lis = Jsoup.parse(front).getElementsByClass(HtmlFodMobilit.LANG_LINK);
        for(Element li : lis) {
            if (li.text().equals(lang)) {
                String href = li.attr(Attribute.HREF.toString());
                return new URL(base, href);
            }
        }
        return base;
    }

    /**
     * Store page containing datasets
     * 
     * @param cache 
     * @throws java.io.IOException 
     */
    private void scrapePage(Cache cache) throws IOException {
        URL front = getBase();
        
        for (String lang : getAllLangs()) {
            URL url = switchLanguage(lang);
            String content = makeRequest(url);
            cache.storePage(front, lang, new Page(url, content));
        }
    }
    
    /**
     * Scrape the site.
     * 
     * @throws IOException 
     */
    @Override  
    public void scrape() throws IOException{
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
        Elements rows = Jsoup.parse(datasets).getElementsByTag(HTML.Tag.TR.toString());
        logger.info("Found {} datasets on page", String.valueOf(rows.size()));
        
        logger.info("Done scraping");
    }
    
    /**
     * Generate DCAT distribution.
     * 
     * @param store RDF store
     * @param dataset URI
     * @param front URL of the front page
     * @param link link element
     * @param i row sequence
     * @param lang language code
     * @throws MalformedURLException
     * @throws RepositoryException 
     */
    private void generateDist(Storage store, URI dataset, URL access, 
                                            Elements link, int i, String lang) 
                            throws MalformedURLException, RepositoryException {
        String href = link.first().attr(Attribute.HREF.toString());
        URL download = makeAbsURL(href);        
     
        URL u = makeDistURL(i + "/" + lang);
        URI dist = store.getURI(u.toString());
        logger.debug("Generating distribution {}", dist.toString());
        
        store.add(dataset, DCAT.DISTRIBUTION, dist);
        store.add(dist, RDF.TYPE, DCAT.A_DISTRIBUTION);
        store.add(dist, DCTERMS.LANGUAGE, MDR_LANG.MAP.get(lang));
        store.add(dist, DCTERMS.TITLE, link.first().ownText(), lang);
        store.add(dist, DCAT.ACCESS_URL, access);
        store.add(dist, DCAT.DOWNLOAD_URL, download);
        store.add(dist, DCAT.MEDIA_TYPE, getFileExt(href));
    }
    
    /**
     * Generate one dataset
     * 
     * @param store  RDF store
     * @param URL front
     * @param row HTML row
     * @param i number
     * @param lang language
     * @throws MalformedURLException
     * @throws RepositoryException
     */
    private void generateDataset(Storage store, URL front, Element row, int i, String lang) 
                            throws MalformedURLException, RepositoryException {
        URL u = makeDatasetURL(String.valueOf(i));
        URI dataset = store.getURI(u.toString());  
        logger.debug("Generating dataset {}", dataset.toString());
                
        Elements cells = row.getElementsByTag(Tag.TD.toString());
        String desc = cells.get(0).text();
        String title = desc;
                
        store.add(dataset, RDF.TYPE, DCAT.A_DATASET);
        store.add(dataset, DCTERMS.LANGUAGE, MDR_LANG.MAP.get(lang));
        store.add(dataset, DCTERMS.TITLE, title, lang);
        store.add(dataset, DCTERMS.DESCRIPTION, desc, lang);
        store.add(dataset, DCTERMS.IDENTIFIER, makeHashId(u.toString()));
        store.add(dataset, DCAT.LANDING_PAGE, front);
    
        Elements link = cells.get(1).getElementsByTag(Tag.A.toString());
        generateDist(store, dataset, front, link, i, lang);
    }
    
    /**
     * Generate DCAT datasets.
     * 
     * @param store RDF store
     * @param id
     * @param page
     * @throws MalformedURLException
     * @throws RepositoryException 
     */
    @Override
    public void generateDataset(Storage store, String id, Map<String,Page> page)
                            throws MalformedURLException, RepositoryException {
        String[] langs = getAllLangs();
        for (String lang : langs) {
            Page p = page.getOrDefault(lang, new Page());
            String html = p.getContent();
            URL front = p.getUrl();
            Elements rows = Jsoup.parse(html).body().getElementsByTag(Tag.TR.toString());
            
            int i = 0;
            for (Element row : rows) {
                generateDataset(store, front, row, i, lang);
                i++;
            }
        }
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
        store.add(catalog, DCTERMS.TITLE, "DCAT export FPS Mobility", "en");
        store.add(catalog, DCTERMS.LANGUAGE, MDR_LANG.NL);
        store.add(catalog, DCTERMS.LANGUAGE, MDR_LANG.FR);
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
     * HTML scraper FPS Mobility.
     * 
     * @param caching
     * @param storage
     * @param base 
     */
    public HtmlFodMobilit(File caching, File storage, URL base) {
        super(caching, storage, base);
        setName("fpsmobilit");
    }
}
