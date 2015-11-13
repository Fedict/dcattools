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
import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import javax.swing.text.html.HTML;
import javax.swing.text.html.HTML.Attribute;
import javax.swing.text.html.HTML.Tag;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;
import org.jsoup.select.Elements;
import org.openrdf.model.URI;
import org.openrdf.model.vocabulary.DCTERMS;
import org.openrdf.model.vocabulary.FOAF;
import org.openrdf.model.vocabulary.RDF;
import org.openrdf.repository.RepositoryException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Statbel "publications" scraper.
 * 
 * @author Bart Hanssens <bart.hanssens@fedict.be>
 */
public class HtmlStatbelPubs extends Html {
    private final Logger logger = LoggerFactory.getLogger(HtmlStatbelPubs.class);
    
    public final static String CAT_SELECT = "category_select";
    public final static String CAT_CAT = "Statistieken - Download-tabellen";
    
    public final static String LANG_LINK = "blgm_lSwitch";
    public final static String DIV_MAIN = "mainContent";
    public final static String DIV_FRST = "detailFirstPart";
    public final static String DIV_DATE = "date"; 
    public final static String DIV_CAT = "facets";
    public final static String DIV_SCND = "detailScdPart";
    
    /**
     * Get the URL of the  page in another language
     * 
     * @param page
     * @param lang
     * @return URL of the page in another language
     * @throws IOException 
     */
    private URL switchLanguage(String page, String lang) throws IOException {
        URL base = getBase();
        
        Elements lis = Jsoup.parse(page)
                            .getElementsByClass(HtmlStatbelPubs.LANG_LINK);
        
        for(Element li : lis) {
            if (li.text().equals(lang)) {
                String href = li.attr(HTML.Attribute.HREF.toString());
                if (href != null && !href.isEmpty()) {
                    return new URL(base.getProtocol(), base.getHost(), href);
                }
            }
        }
        return null;
    }
    

    /**
     * Scrape dataset
     * 
     * @param u
     * @throws IOException
     */
    private void scrapeDataset(URL u) throws IOException {
        Cache cache = getCache();
        String deflang = getDefaultLang();
        String html = makeRequest(u);
        
        cache.storePage(u, deflang, new Page(u, html));
        
        String[] langs = getAllLangs();
        for (String lang : langs) {
            if (! lang.equals(deflang)) {
                URL url = switchLanguage(html, lang);
                String body = makeRequest(url);
                cache.storePage(u, lang, new Page(url, body));
                sleep();
            }
        } 
    }
    
    /**
     * Get the list of all the downloads (DCAT Dataset).
     * 
     * @return List of URLs
     * @throws IOException 
     */
    private List<URL> scrapeDatasetList() throws IOException {
        List<URL> urls = new ArrayList<>();
        
        URL base = getBase();
        String front = makeRequest(base);
        
        // Select the correct page from dropdown-list, displaying all items
        Element select = Jsoup.parse(front).getElementById(HtmlStatbelPubs.CAT_SELECT);
        Element opt = select.getElementsMatchingOwnText(HtmlStatbelPubs.CAT_CAT).first();
        if (opt != null) {
            URL downloads = new URL(base, opt.val() + "&size=250");
            String page = makeRequest(downloads);
            
            // Extract links from list
            Elements rows = Jsoup.parse(page).getElementsByTag(Tag.TD.toString());
            for(Element row : rows) {
                Element link = row.getElementsByTag(Tag.A.toString()).first();
                String href = link.attr(Attribute.HREF.toString());
                urls.add(new URL(getBase(), href));
            }
        } else {
            logger.error("Category {} not found", HtmlStatbelPubs.CAT_CAT);
        }
        return urls;
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
            Map<String, Page> page = cache.retrievePage(u);
            if (page.isEmpty()) {
                sleep();
                if (++i % 100 == 0) {
                    logger.info("Download {}...", Integer.toString(i));
                }
                try {
                    scrapeDataset(u);
                } catch (IOException ex) {
                    logger.error("Failed to scrape {}", u);
                }
            }
        }
        logger.info("Done scraping");
    }

    /**
     * Generate DCAT Distribution.
     * 
     * @param store RDF store
     * @param dataset dataset URI
     * @param access access URL
     * @param link link element
     * @param lang language code
     * @throws MalformedUrlException
     * @throws RepositoryException
     */
    private void generateDist(Storage store, URI dataset, URL access, Element link, 
                String lang) throws MalformedURLException, RepositoryException {
        String href = link.attr(Attribute.HREF.toString());
        URL download = makeAbsURL(href);
        
        String id = makeHashId(download.toString());
        URI dist = store.getURI(makeDistURL(id).toString());
        logger.debug("Generating distribution {}", dist.toString());
        
        store.add(dataset, DCAT.DISTRIBUTION, dist);
        store.add(dist, RDF.TYPE, DCAT.A_DISTRIBUTION);
        store.add(dist, DCTERMS.LANGUAGE, MDR_LANG.MAP.get(lang));
        store.add(dist, DCTERMS.TITLE, link.ownText(), lang);
        store.add(dist, DCAT.ACCESS_URL, access);
        store.add(dist, DCAT.DOWNLOAD_URL, download);
        store.add(dist, DCAT.MEDIA_TYPE, getFileExt(href));
    }
    
    /**
     * Generate DCAT Dataset.
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
  
        URI dataset = store.getURI(makeDatasetURL(id).toString());
        logger.info("Generating dataset {}", dataset.toString());
        
        store.add(dataset, RDF.TYPE, DCAT.A_DATASET);
        store.add(dataset, DCTERMS.IDENTIFIER, id);
            
        for (String lang : getAllLangs()) {
            Page p = page.get(lang);
            if (p == null) {
                logger.warn("Page {} not available in {}", dataset.toString(), lang);
                continue;
            }
            String html = p.getContent();
            Element doc = Jsoup.parse(html).body();
            String title = doc.getElementsByTag(Tag.H1.toString()).first().text();
            
            Element divmain = doc.getElementsByClass(HtmlStatbelPubs.DIV_MAIN).first();
            Elements paras  = divmain.getElementsByTag(Tag.P.toString());
            String desc = "";
            for (Element para : paras) {
                desc += para.text() + "\n";
            }
            if (desc.isEmpty()) {
                desc = title;
            }
            
            store.add(dataset, DCTERMS.LANGUAGE, MDR_LANG.MAP.get(lang));
            store.add(dataset, DCTERMS.TITLE, title, lang);
            store.add(dataset, DCTERMS.DESCRIPTION, desc, lang);
            
            Element divdate = doc.getElementsByClass(HtmlStatbelPubs.DIV_DATE).first();
            if (divdate != null) {
                Node n = divdate.childNodes().get(1);
                store.add(dataset, DCTERMS.MODIFIED, n.toString().trim());
            }
            
            Element divcat = doc.getElementsByClass(HtmlStatbelPubs.DIV_CAT).first();
            if (divcat != null) {
                Node n = divcat.childNodes().get(1);
                String[] cats = n.toString().split(",");
                for (String cat : cats) {
                    store.add(dataset, DCAT.THEME, cat.trim(), lang);
                }
            }
            
            Element divlinks = doc.getElementsByClass(HtmlStatbelPubs.DIV_SCND).first();
            if (divlinks != null) {
                Elements links = divlinks.getElementsByTag(Tag.A.toString());
                for(Element link : links) {
                    generateDist(store, dataset, p.getUrl(), link, lang);
                }
            }
        }
    }
    
    /**
     * Generate DCAT Catalog info.
     * 
     * @param store RDF store
     * @param catalog
     * @throws RepositoryException 
     */
    @Override
    public void generateCatalogInfo(Storage store, URI catalog) 
                                                    throws RepositoryException {
        super.generateCatalogInfo(store, catalog);
        store.add(catalog, DCTERMS.TITLE, "DCAT export Statbel downloads", "en");
        store.add(catalog, DCTERMS.LANGUAGE, MDR_LANG.FR);
        store.add(catalog, DCTERMS.LANGUAGE, MDR_LANG.NL);
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
        for(URL u : urls) {
            Map<String,Page> page = cache.retrievePage(u);
            String id = makeHashId(u.toString());
            generateDataset(store, id, page);
        }
        generateCatalog(store);
        
    }
    
    
    /**
     * HTML parser for Statbel publications
     * 
     * @param caching
     * @param storage
     * @param base 
     */
    public HtmlStatbelPubs(File caching, File storage, URL base) {
        super(caching, storage, base);
        setName("statbelpub");
    }
}
