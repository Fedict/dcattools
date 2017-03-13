/*
 * Copyright (c) 2017, Bart Hanssens <bart.hanssens@fedict.be>
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
import java.io.File;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import javax.swing.text.html.HTML;
import org.eclipse.rdf4j.model.IRI;

import org.eclipse.rdf4j.repository.RepositoryException;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Scraper for website Brugge.
 * 
 * @author Bart.Hanssens
 */
public class HtmlBrugge extends Html {
	private final Logger logger = LoggerFactory.getLogger(HtmlBrugge.class);
	
    private final static String LINKS_DATASETS = "div.user-content div a:has(.href)";
	private final static String LINK_DATASET = "div.user-content h4";

	@Override
	public void scrape() throws IOException {
        logger.info("Start scraping");
        Cache cache = getCache();
		
        List<URL> urls = cache.retrieveURLList();
        if (urls.isEmpty()) {
            urls = scrapeDatasetLists();
            cache.storeURLList(urls);
        }
    
		logger.info("Found {} dataset lists", String.valueOf(urls.size()));
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
     * Get the list of all the downloads (DCAT Dataset).
     * 
     * @return List of URLs
     * @throws IOException 
     */
	private List<URL> scrapeDatasetLists() throws IOException {
		List<URL> urls = new ArrayList<>();

		URL base = getBase();
		String front = makeRequest(base);
		Elements links = Jsoup.parse(front).select(LINKS_DATASETS);
        
		for(Element link : links) {
			String href = link.attr(HTML.Attribute.HREF.toString());
			urls.add(makeAbsURL(href));
		}
		return urls;
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
		IRI dataset = store.getURI(u.toString());  
		logger.debug("Generating dataset {}", dataset.toString());
		// TODO
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
			
		Elements rows = Jsoup.parse(html).select(LINK_DATASET);
            
		int i = 0;
		for (Element row : rows) {
			generateDataset(store, front, row, i, lang);
			i++;
		}
	}
	
	@Override
	public void generateDcat(Cache cache, Storage store) throws RepositoryException, MalformedURLException {
		logger.info("Generate DCAT");
        
		/* Get the list of all datasets */            
		List<URL> urls = cache.retrieveURLList();
		for(URL u : urls) {
			Map<String,Page> page = cache.retrievePage(u);
			String id = makeHashId(u.toString());
			generateDataset(store, id, page);
		}
        generateCatalog(store);
		throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
	}
	
	/**
	 * Constructor
	 * 
	 * @param caching DB cache file
	 * @param storage
	 * @param base 
	 */
	public HtmlBrugge(File caching, File storage, URL base) {
		super(caching, storage, base);
        setName("brugge");
	}
}
