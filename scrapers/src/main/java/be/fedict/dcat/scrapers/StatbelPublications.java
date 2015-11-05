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
import javax.swing.text.html.HTML;
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
public class StatbelPublications extends Html {
    private final Logger logger = LoggerFactory.getLogger(StatbelPublications.class);
    
    public final static String CAT_SELECT = "category_select";
    public final static String CAT_CAT = "Statistieken - Download-tabellen";
    public final static String LANG_LINK = "blgm_lSwitch";
    
    @Override
    public URL switchLanguage(String lang) throws IOException {
        URL base = getBase();
        
        String front= makeRequest(base);

        Elements lis = Jsoup.parse(front)
                            .getElementsByClass(StatbelPublications.LANG_LINK);
        for(Element li : lis) {
            if (li.text().equals(lang)) {
                String href = li.attr(HTML.Attribute.HREF.toString());
                if (href != null && !href.isEmpty()) {
                    return new URL(base, href);
                }
            }
        }
        logger.debug("base {}", base);
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
     * Store front page containing list of datasets
     * 
     * @param cache 
     * @throws java.io.IOException 
     */
    public void scrapeFront(Cache cache) throws IOException {
        URL front = getBase();
        
        for (String lang : getAllLangs()) {
            URL url = switchLanguage(lang);
            cache.storePage(front, makeRequest(url), lang);
        }
    }

    /**
     * Get the list of all the downloads (DCAT Dataset).
     * 
     * @return List of URLs
     * @throws MalformedURLException
     * @throws IOException 
     */
    protected List<URL> scrapeDatasetList() throws MalformedURLException, IOException {
        List<URL> urls = new ArrayList<>();
        
        URL base = getBase();
        String front = makeRequest(base);
        Element select = Jsoup.parse(front).getElementById(StatbelPublications.CAT_SELECT);
        Elements opt = select.getElementsMatchingText(StatbelPublications.CAT_SELECT);
        if (opt != null) {
            urls.add(new URL(base, opt.val()));
        }
        // TODO
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
    }
 
    /**
     * HTML parser for Statbel publications
     * 
     * @param caching
     * @param storage
     * @param base 
     */
    public StatbelPublications(File caching, File storage, URL base) {
        super(caching, storage, base);
        setDefaultLang("nl");
        setAllLangs(new String[]{"nl", "fr"});
    }
}
