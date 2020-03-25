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

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;
import java.util.Map;

import org.eclipse.rdf4j.repository.RepositoryException;

/**
 * Abstract scraper for HTML sites.
 *
 * @author Bart Hanssens <bart.hanssens@fedict.be>
 */
public abstract class Html extends Scraper {

	/**
	 * Generate DCAT Dataset
	 *
	 * @param store RDF store
	 * @param id dataset id
	 * @param page
	 * @throws MalformedURLException
	 * @throws RepositoryException
	 */
	protected abstract void generateDataset(Storage store, String id, Map<String, Page> page)
			throws MalformedURLException, RepositoryException;

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
		List<URL> urls = cache.retrieveURLList();
		for (URL u : urls) {
			Map<String, Page> page = cache.retrievePage(u);
			String id = makeHashId(u.toString());
			generateDataset(store, id, page);
		}
		generateCatalog(store);
	}
	
	/**
	 * Get the list of all the downloads (DCAT Dataset).
	 *
	 * @return List of URLs
	 * @throws IOException
	 */
	protected abstract List<URL> scrapeDatasetList() throws IOException;
	
	/**
	 * Scrape a dataset
	 * 
	 * @param u url
	 * @throws IOException 
	 */
	protected void scrapeDataset(URL u) throws IOException {
		Cache cache = getCache();
		String html = makeRequest(u);
		cache.storePage(u, "", new Page(u, html));
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

		logger.info("Found {} datasets on page", String.valueOf(urls.size()));
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
	 * Constructor
	 *
	 * @param caching DB cache file
	 * @param storage SDB file to be used as triple store backend
	 * @param base base URL
	 */
	public Html(File caching, File storage, URL base) {
		super(caching, storage, base);
	}
}
