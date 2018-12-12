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

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;

import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.eclipse.rdf4j.repository.RepositoryException;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.RDFParseException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Scraper for CKAN portals with RDF support enabled.
 *
 * @author Bart Hanssens <bart.hanssens@fedict.be>
 */
public abstract class CkanRDF extends Ckan {

	private final Logger logger = LoggerFactory.getLogger(CkanRDF.class);

	/**
	 * Scrape paginated catalog file
	 * 
	 * @param cache
	 * @throws IOException 
	 */
	protected void scrapeCat(Cache cache) throws IOException {
		int lastpage = 100;
		List<URL> urls = new ArrayList<>();
		
		for(int i = 1; i < lastpage; i++) {
			URL url = new URL(getBase(), Ckan.CATALOG + 
										".ttl?page=" + String.valueOf(i));
			String content = makeRequest(url);
			cache.storePage(url, "all", new Page(url, content));
			if (content.length() < 1500) {
				lastpage = i;
			} else {
				urls.add(url);
			}
		}
		cache.storeURLList(urls);
	}
	

	@Override
	public void scrape() throws IOException {
		logger.info("Start scraping");
		Cache cache = getCache();
		
		List<URL> urls = cache.retrieveURLList();
		if (urls.isEmpty()) {
			scrapeCat(cache);
		}
		logger.info("Done scraping");
	}
	
	/**
	 * Generate DCAT file
	 *
	 * @param cache
	 * @param store
	 * @throws RepositoryException
	 * @throws MalformedURLException
	 */
	@Override
	public void generateDcat(Cache cache, Storage store)
			throws RepositoryException, MalformedURLException {
		
		List<URL> urls = cache.retrieveURLList();
		for (URL url: urls) {
			logger.info("Generating from " + url);
			Map<String, Page> map = cache.retrievePage(url);
			String ttl = map.get("all").getContent();
			
			// Load turtle file into store
			try (InputStream in = new ByteArrayInputStream(ttl.getBytes(StandardCharsets.UTF_8))) {
				store.add(in, RDFFormat.TURTLE);
			} catch (RDFParseException | IOException ex) {
				throw new RepositoryException(ex);
			}
		}
		generateCatalog(store);
	}

	/**
	 * CKAN scraper.
	 *
	 * @param caching local cache file
	 * @param storage local triple store file
	 * @param base URL of the CKAN site
	 */
	public CkanRDF(File caching, File storage, URL base) {
		super(caching, storage, base);
	}
}
