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
import be.fedict.dcat.vocab.DCAT;
import be.fedict.dcat.vocab.MDR_LANG;
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
     * Store front page containing datasets
     * 
     * @param cache 
     * @throws java.io.IOException 
     */
    private void scrapeFront(Cache cache) throws IOException {
        URL front = getBase();
        
        String deflang = getDefaultLang();
        for (String lang : getAllLangs()) {
            if (!lang.equals(deflang)) {
                URL url = switchLanguage(lang);
                cache.storePage(front, makeRequest(url), lang);
            }
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
        
        Map<String, String> front = cache.retrievePage(getBase());
        if (front.keySet().isEmpty()) {
            scrapeFront(cache);
            front = cache.retrievePage(getBase());   
        }
        // Calculate the number of datasets
        String datasets = front.get(getDefaultLang());
        Elements rows = Jsoup.parse(datasets).getElementsByTag(HTML.Tag.TR.toString());
        logger.info("Found {} datasets on page", String.valueOf(rows.size()));
        
        logger.info("Done scraping");
    }
    

    /**
     * Generate one dataset
     * 
     * @param row
     * @param i number
     * @param lang language
     * @param store  RDF store
     * @throws MalformedURLException
     * @throws RepositoryException
     */
    private void generateDataset(Element row, int i, String lang, Storage store) 
                            throws MalformedURLException, RepositoryException {
        URL u = makeDatasetURL(i);
        URI dataset = store.getURI(u.toString());  
        logger.debug("Generating dataset {}", dataset.toString());
                
        Elements cells = row.getElementsByTag(Tag.TD.toString());
        String desc = cells.get(0).text();
        String title = desc;
                
        store.add(dataset, RDF.TYPE, DCAT.A_DATASET);
        store.add(dataset, DCTERMS.TITLE, title, lang);
        store.add(dataset, DCTERMS.DESCRIPTION, desc, lang);
        store.add(dataset, DCTERMS.IDENTIFIER, makeHashId(u.toString()));
    
        Elements a = cells.get(1).getElementsByTag(Tag.A.toString());
        String href = a.first().attr(Attribute.HREF.toString());
    
        URI dist = store.getURI(makeDistributionURL(i).toString());
        store.add(dataset, DCAT.DISTRIBUTION, dist);
        store.add(dist, RDF.TYPE, DCAT.A_DISTRIBUTION);
        store.add(dist, DCAT.DOWNLOAD_URL, new URL(getBase(), href));
            
        int dot = href.lastIndexOf(".");
        if (dot > 0) {
            String ext = href.substring(i+1);
            store.add(dist, DCAT.MEDIA_TYPE, ext);
        }
    }
    
    /**
     * Generate DCAT datasets.
     * 
     * @param page
     * @param store
     * @throws MalformedURLException
     * @throws RepositoryException 
     */
    @Override
    public void generateDataset(Map<String,String> page, Storage store)
                            throws MalformedURLException, RepositoryException {
        for (String lang : getAllLangs()) {
            String p = page.get(lang);
            Elements rows = Jsoup.parse(p).body().getElementsByTag(Tag.TR.toString());
            int i = 0;
            
            for (Element row : rows) {
                generateDataset(row, i, lang, store);         
                i++;
            }
        }
    }
    
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
        Map<String, String> page = cache.retrievePage(getBase());
        generateDataset(page, store);
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
