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

import be.fedict.dcat.helpers.Storage;
import be.fedict.dcat.helpers.Cache;
import be.fedict.dcat.helpers.Page;

import java.io.File;
import java.io.IOException;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.json.JsonString;

import org.eclipse.rdf4j.repository.RepositoryException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Abstract scraper for CKAN portals.
 *
 * @see http://ckan.org/
 *
 * @author Bart Hanssens <bart.hanssens@fedict.be>
 */
public abstract class Ckan extends Scraper {

	private final Logger logger = LoggerFactory.getLogger(Ckan.class);

	// CKAN JSON fields
	public final static String RESULT = "result";
	public final static String SUCCESS = "success";
	public final static String PACKAGES = "packages";

	// CKAN API
	public final static String API_LIST = "/api/3/action/package_list";
	public final static String API_PKG = "/api/3/action/package_show?id=";
	public final static String API_ORG = "/api/3/action/organization_show?id=";
	public final static String API_RES = "/api/3/action/resource_show?id=";

	public final static String DATASET = "/dataset/";
	public final static String ORG = "/organization";
	public final static String RESOURCE = "/resource/";

	/**
	 * Make an URL for retrieving JSON of CKAN Package (DCAT Dataset)
	 *
	 * @param id
	 * @return URL
	 * @throws MalformedURLException
	 */
	protected abstract URL ckanDatasetURL(String id) throws MalformedURLException;

	/**
	 * Generate DCAT Dataset.
	 *
	 * @param store RDF triple store
	 * @param id id
	 * @param page
	 * @throws MalformedURLException
	 * @throws RepositoryException
	 */
	protected abstract void generateDataset(Storage store, String id,
			Map<String, Page> page) throws MalformedURLException, RepositoryException;

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
		for (URL u : urls) {
			Map<String, Page> page = cache.retrievePage(u);
			generateDataset(store, null, page);
		}
		generateCatalog(store);
	}

	/**
	 * Get content of a (JSON / HTML / RDF) page
	 *
	 * @param url url
	 * @return content as string
	 * @throws IOException
	 */
	protected abstract String getPage(URL url) throws IOException;

	/**
	 * Get the list of all the CKAN packages (DCAT Dataset).
	 *
	 * @return List of URLs
	 * @throws MalformedURLException
	 * @throws IOException
	 */
	protected List<URL> scrapePackageList() throws MalformedURLException, IOException {
		List<URL> urls = new ArrayList<>();
		URL getPackages = new URL(getBase(), Ckan.API_LIST);

		JsonObject obj = makeJsonRequest(getPackages);
		if (!obj.getBoolean(Ckan.SUCCESS)) {
			return urls;
		}
		JsonArray arr = obj.getJsonArray(Ckan.RESULT);
		for (JsonString str : arr.getValuesAs(JsonString.class)) {
			urls.add(ckanDatasetURL(str.getString()));
		}
		return urls;
	}

	/**
	 * Fetch all metadata from the CKAN repository.
	 *
	 * @throws IOException
	 */
	@Override
	public void scrape() throws IOException {
		logger.info("Start scraping");
		Cache cache = getCache();

		List<URL> urls = cache.retrieveURLList();
		if (urls.isEmpty()) {
			urls = scrapePackageList();
			cache.storeURLList(urls);
		}
		urls = cache.retrieveURLList();

		logger.info("Found {} CKAN packages", String.valueOf(urls.size()));
		logger.info("Start scraping (waiting between requests)");
		int i = 0;
		for (URL u : urls) {
			Map<String, Page> page = cache.retrievePage(u);
			if (page.isEmpty()) {
				sleep();
				if (++i % 100 == 0) {
					logger.info("Package {}...", Integer.toString(i));
				}
				try {
					String s = getPage(u);
					if (!s.isEmpty()) {
						cache.storePage(u, "", new Page(u, s));
					}
				} catch (IOException e) {
					logger.warn("Failed to scrape {}", u);
				}
			}
		}
		logger.info("Done scraping");
	}

	/**
	 * CKAN scraper.
	 *
	 * @param caching local cache file
	 * @param storage local triple store file
	 * @param base URL of the CKAN site
	 */
	public Ckan(File caching, File storage, URL base) {
		super(caching, storage, base);
	}
}
