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
import java.util.Map;
import javax.swing.text.html.HTML.Tag;
import org.jsoup.Jsoup;
import org.jsoup.select.Elements;
import org.openrdf.repository.RepositoryException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Bart Hanssens <bart.hanssens@fedict.be>
 */
public abstract class Html extends Scraper {
    private final Logger logger = LoggerFactory.getLogger(Html.class);
    
    /**
     * Make a URL for a DCAT Dataset 
     * 
     * @param i
     * @return URL
     * @throws java.net.MalformedURLException 
     */
    public URL makeDatasetURL(int i) throws MalformedURLException {
        return new URL(getBase().toString() + "#" + String.valueOf(i));
    }
    
    /**
     * Make a URL for a DCAT Distribution 
     * 
     * @param i
     * @return URL
     * @throws java.net.MalformedURLException 
     */
    public URL makeDistributionURL(int i) throws MalformedURLException {
        return new URL(getBase().toString() + "#" + String.valueOf(i) + "/download");
    }
    
    /**
     * Switch to another language, if available.
     * 
     * @param lang 
     * @return URL
     */
    public abstract URL switchLanguage(String lang) throws IOException;
    
    @Override
    public void generateDcat(Cache cache, Storage store) 
                                throws RepositoryException, MalformedURLException {
        Map<String, String> page = cache.retrievePage(null);
    }
    
    /**
     * Scrape front page
     * 
     * @param cache
     * @throws IOException 
     */
    public abstract void scrapeFront(Cache cache) throws IOException;
    
    /**
     * Scrape the site.
     * 
     * @throws IOException 
     */
    @Override
    public void scrape() throws IOException {
        logger.info("Start scraping");
        Cache cache = getCache();
        
        Map<String, String> front = cache.retrievePage(getBase());
        if (front.keySet().isEmpty()) {
            scrapeFront(cache);
            front = cache.retrievePage(getBase());   
        }
        // Calculate the number of datasets
        String datasets = front.get(getDefaultLang());
        Elements rows = Jsoup.parse(datasets).getElementsByTag(Tag.TR.toString());
        logger.info("Found {} datasets on page", String.valueOf(rows.size()));
        
        logger.info("Done scraping");
    }
    
    /**
     * HTML page scraper.
     * 
     * @param caching local cache file
     * @param storage local triple store file
     * @param base URL of the CKAN site
     */
    public Html(File caching, File storage, URL base) {
        super(caching, storage, base);
    }
}
