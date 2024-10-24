/*
 * Copyright (c) 2019, FPS BOSA DG DT
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
package be.gov.data.scrapers.bipt;

import be.gov.data.scrapers.Cache;
import be.gov.data.scrapers.Page;
import be.gov.data.helpers.Storage;
import be.gov.data.scrapers.Html;

import be.gov.data.dcat.vocab.MDR_LANG;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import javax.swing.text.html.HTML.Attribute;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.vocabulary.DCAT;
import org.eclipse.rdf4j.model.vocabulary.DCTERMS;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.repository.RepositoryException;

/**
 * Scraper BIPT
 *
 * @see https://www.bipt.be/consumers/open-data
 * @author Bart Hanssens
 */
public class HtmlBIPT extends Html {
	private final static String PAGE = "/opendata";
	private final static String DIV_DATASET = "div.project";
	private final static String TITLE = "div.project-title h3";
	private final static String DESC = "div.project-description p";
	private final static String FILES = "div.files ul li a";
	
	
	@Override
	protected List<URL> scrapeDatasetList() throws IOException {
		throw new UnsupportedOperationException("Not supported yet."); 
	}
	
	/**
	 * Switch to another language
	 *
	 * @param lang
	 * @return
	 * @throws IOException
	 */
	private URL switchLanguage(String lang) throws IOException {
		return new URL(getBase(), lang + PAGE);
	}

	/**
	 * Store page containing datasets
	 *
	 * @param cache
	 * @throws IOException
	 */
	private void scrapePage(Cache cache) throws IOException {
		URL front = getBase();

		for (String lang : getAllLangs()) {
			URL url = switchLanguage(lang);
			String content = makeRequest(url);
			cache.storePage(front, lang, new Page(url, content));
		}
	}

	/**
	 * Scrape the site.
	 *
	 * @throws IOException
	 */
	@Override
	public void scrape() throws IOException {
		LOG.info("Start scraping");
		Cache cache = getCache();

		Map<String, Page> front = cache.retrievePage(getBase());
		if (front.keySet().isEmpty()) {
			scrapePage(cache);
			front = cache.retrievePage(getBase());
		}
		// Calculate the number of datasets
		Page p = front.get(getDefaultLang());
		String datasets = p.getContent();
		Elements rows = Jsoup.parse(datasets).select(DIV_DATASET);
		LOG.info("Found {} datasets on page", String.valueOf(rows.size()));

		LOG.info("Done scraping");
	}

	/**
	 * Generate DCAT distribution.
	 *
	 * @param store RDF store
	 * @param dataset URI
	 * @param access access URL of the dataset
	 * @param link link element
	 * @param i row sequence
	 * @param lang language code
	 * @throws MalformedURLException
	 * @throws RepositoryException
	 */
	private void generateDist(Storage store, IRI dataset, URL access,
			Element link, int i, int j, String lang)
			throws MalformedURLException, RepositoryException {
		String href = link.attr(Attribute.HREF.toString());
		URL download = makeAbsURL(href);

		IRI dist = makeDistIRI(i + "/" + j + "/" + lang);
		LOG.debug("Generating distribution {}", dist.toString());

		store.add(dataset, DCAT.HAS_DISTRIBUTION, dist);
		store.add(dist, RDF.TYPE, DCAT.DISTRIBUTION);
		store.add(dist, DCTERMS.LANGUAGE, MDR_LANG.MAP.get(lang));
		store.add(dist, DCTERMS.TITLE, link.ownText(), lang);
		store.add(dist, DCAT.ACCESS_URL, access);
		store.add(dist, DCAT.DOWNLOAD_URL, download);
		store.add(dist, DCAT.MEDIA_TYPE, "csv");
	}

	/**
	 * Generate one dataset
	 *
	 * @param store RDF store
	 * @param front front
	 * @param row HTML row
	 * @param i number
	 * @param lang language
	 * @throws MalformedURLException
	 * @throws RepositoryException
	 */
	private void generateDataset(Storage store, URL front, Element row, int i, String lang)
			throws MalformedURLException, RepositoryException {
		IRI dataset = makeDatasetIRI(String.valueOf(i));
		LOG.debug("Generating dataset {}", dataset.toString());

		String title = row.select(TITLE).first().text();
		String desc = row.select(DESC).text();
		store.add(dataset, RDF.TYPE, DCAT.DATASET);
		store.add(dataset, DCTERMS.LANGUAGE, MDR_LANG.MAP.get(lang));
		store.add(dataset, DCTERMS.TITLE, title, lang);
		store.add(dataset, DCTERMS.DESCRIPTION, desc, lang);
		store.add(dataset, DCTERMS.IDENTIFIER, hash(dataset.toString()));

		Elements files = row.select(FILES);
		int j = 0;
		for (Element file : files) {
			generateDist(store, dataset, front, file, i, j, lang);
			j++;
		}
	}

	/**
	 * Generate DCAT datasets.
	 *
	 * @param store RDF store
	 * @param id
	 * @param page
	 * @throws MalformedURLException
	 * @throws RepositoryException
	 */
	@Override
	public void generateDataset(Storage store, String id, Map<String, Page> page)
			throws MalformedURLException, RepositoryException {
		String[] langs = getAllLangs();
		for (String lang : langs) {
			Page p = page.getOrDefault(lang, new Page());
			String html = p.getContent();
			URL front = null;
			try {
				front = switchLanguage(lang);
			} catch (IOException ioe) {
				//
			}
			Elements rows = Jsoup.parse(html).body().select(DIV_DATASET);

			int i = 0;
			for (Element row : rows) {
				generateDataset(store, front, row, i, lang);
				i++;
			}
		}
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
		LOG.info("Generate DCAT");

		/* Get the list of all datasets */
		Map<String, Page> page = cache.retrievePage(getBase());
		generateDataset(store, null, page);
		generateCatalog(store);
	}

	/**
	 * Constructor
	 *
	 * @param prop
	 * @throws IOException
	 */
	public HtmlBIPT(Properties prop) throws IOException {
		super(prop);
		setName("bipt");
	}
}
