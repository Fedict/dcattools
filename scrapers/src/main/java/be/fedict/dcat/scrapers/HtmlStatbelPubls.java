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
import be.fedict.dcat.helpers.Storage;
import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import javax.swing.text.html.HTML;
import javax.swing.text.html.HTML.Attribute;
import javax.swing.text.html.HTML.Tag;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.openrdf.model.URI;
import org.openrdf.model.vocabulary.DCTERMS;
import org.openrdf.model.vocabulary.FOAF;
import org.openrdf.repository.RepositoryException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Statbel "publications" scraper.
 * 
 * @author Bart Hanssens <bart.hanssens@fedict.be>
 */
public class HtmlStatbelPubls extends Html {
    private final Logger logger = LoggerFactory.getLogger(HtmlStatbelPubls.class);
    
    public final static String CAT_SELECT = "category_select";
    public final static String CAT_CAT = "Statistieken - Download-tabellen";
    public final static String LANG_LINK = "blgm_lSwitch";
    
    @Override
    public URL switchLanguage(String lang) throws IOException {
        URL base = getBase();
        
        String front= makeRequest(base);
        Elements lis = Jsoup.parse(front)
                            .getElementsByClass(HtmlStatbelPubls.LANG_LINK);
        for(Element li : lis) {
            if (li.text().equals(lang)) {
                String href = li.attr(HTML.Attribute.HREF.toString());
                if (href != null && !href.isEmpty()) {
                    return new URL(base, href);
                }
            }
        }
        return base;
    }

    @Override
    public void generateCatalogInfo(Storage store, URI catalog) 
                                                    throws RepositoryException {
        super.generateCatalogInfo(store, catalog);
        store.add(catalog, DCTERMS.TITLE, "Statbel downloads", "en");
    }

    @Override
    public void generateDatasets(Map<String, String> page, Storage store) throws MalformedURLException, RepositoryException {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }
    

    /**
     * Scrape dataset
     * 
     * @param u
     * @throws IOException
     */
    protected void scrapeDataset(URL u) throws IOException {
        Cache cache = getCache();
        String lang = getDefaultLang();
        
        cache.storePage(u, makeRequest(u), lang);
        
        for (String l : getAllLangs()) {
            if (l.equals(lang)) {
                URL url = switchLanguage(lang);
                cache.storePage(u, makeRequest(url), lang);
            }
        } 
    }
    
    /**
     * Get the list of all the downloads (DCAT Dataset).
     * 
     * @return List of URLs
     * @throws IOException 
     */
    protected List<URL> scrapeDatasetList() throws IOException {
        List<URL> urls = new ArrayList<>();
        
        URL base = getBase();
        String front = makeRequest(base);
        
        // Select the correct page from dropdown-list, displaying all items
        Element select = Jsoup.parse(front).getElementById(HtmlStatbelPubls.CAT_SELECT);
        Elements opt = select.getElementsMatchingText(HtmlStatbelPubls.CAT_SELECT);
        if (opt != null) {
            URL downloads = new URL(base, opt.val() + "&size=999");
            String page = makeRequest(downloads);
            
            // Extract links from list
            Elements rows = Jsoup.parse(page).getElementsByTag(Tag.TD.toString());
            for(Element row : rows) {
                Element link = row.getElementsByTag(Tag.A.toString()).first();
                String href = link.attr(Attribute.HREF.toString());
                urls.add(new URL(getBase(), href));
            }
        }
        return urls;
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
        
        List<URL> urls = cache.retrieveURLList();
        if (urls.isEmpty()) {
            urls = scrapeDatasetList();
            cache.storeURLList(urls);
        }
        urls = cache.retrieveURLList();
        
        logger.info("Found {} downloads", String.valueOf(urls.size()));
        logger.info("Start scraping (waiting between requests)");
        int i = 0;
        for (URL u : urls) {
            Map<String, String> page = cache.retrievePage(u);
            if (page.isEmpty()) {
                sleep();
                if (++i % 100 == 0) {
                    logger.info("Download {}...", Integer.toString(i));
                }
                scrapeDataset(u);
            }
        }
        logger.info("Done scraping");
    }
 
    /**
     * HTML parser for Statbel publications
     * 
     * @param caching
     * @param storage
     * @param base 
     */
    public HtmlStatbelPubls(File caching, File storage, URL base) {
        super(caching, storage, base);
    }
}
