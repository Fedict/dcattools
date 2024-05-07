/*
 * Copyright (c) 2016, FPS BOSA DG DT
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
package be.gov.data.scrapers;

import be.gov.data.helpers.Storage;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.eclipse.rdf4j.repository.RepositoryException;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.RDFParseException;

/**
 * Scraper for CKAN portals with RDF support enabled.
 *
 * @author Bart Hanssens
 */
public abstract class CkanRDF extends Ckan {
	private final Charset charset;

	/**
	 * Scrape paginated catalog file
	 *
	 * @param cache
	 * @throws IOException
	 */
	protected void scrapeCat(Cache cache) throws IOException {
		int lastpage = 1000;
		List<URL> urls = new ArrayList<>();
		LOG.info("Assuming charset {}", charset);

		for (int i = 1; i < lastpage; i++) {
			URL url = new URL(getBase() + Ckan.CATALOG + ".xml?page=" + i);
			String content = makeRequest(url, charset);
			cache.storePage(url, "all", new Page(url, content));
			// check if the page is actually empty
			if (! (content.contains("dcat:Dataset") || content.contains("dcat:DataService"))) {
				lastpage = i;
			} else {
				urls.add(url);
			}
		}
		cache.storeURLList(urls);
	}

	@Override
	public void scrape() throws IOException {
		LOG.info("Start scraping");
		Cache cache = getCache();

		List<URL> urls = cache.retrieveURLList();
		if (urls.isEmpty()) {
			scrapeCat(cache);
		}
		LOG.info("Done scraping");
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
		for (URL url : urls) {
			LOG.info("Generating from " + url);
			Map<String, Page> map = cache.retrievePage(url);
			String ttl = map.get("all").getContent();

			// Load rdf file into store
			try (InputStream in = new ByteArrayInputStream(ttl.getBytes(StandardCharsets.UTF_8))) {
				store.add(in, RDFFormat.RDFXML);
			} catch (RDFParseException | IOException ex) {
				LOG.error("Exception" + url, ex);
			}
		}
		generateCatalog(store);
	}

	/**
	 * Constructor
	 * 
	 * @param prop
	 * @throws IOException
	 */
	protected CkanRDF(Properties prop) throws IOException {
		super(prop);
		String chr = getProperty(prop, "charset");
		this.charset = (chr != null) ? Charset.forName(chr) : StandardCharsets.UTF_8;
	}
}
