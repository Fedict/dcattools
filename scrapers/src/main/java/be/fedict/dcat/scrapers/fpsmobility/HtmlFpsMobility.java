/*
 * Copyright (c) 2015, FPS BOSA DG DT
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
package be.fedict.dcat.scrapers.fpsmobility;

import be.fedict.dcat.scrapers.Cache;
import be.fedict.dcat.scrapers.Page;
import be.fedict.dcat.helpers.Storage;
import be.fedict.dcat.scrapers.Html;

import be.fedict.dcat.vocab.MDR_LANG;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import javax.swing.text.html.HTML;
import javax.swing.text.html.HTML.Attribute;
import javax.swing.text.html.HTML.Tag;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.vocabulary.DCAT;
import org.eclipse.rdf4j.model.vocabulary.DCTERMS;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.repository.RepositoryException;

/**
 * Scraper FPS Mobility
 *
 * @see https://mobilit.belgium.be/nl/publicaties/open_data
 * @author Bart Hanssens
 */
public class HtmlFpsMobility extends Html {

	public final static String LANG_LINK = "language-link";

	@Override
	protected List<URL> scrapeDatasetList() throws IOException {
		throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
	}
	
	/**
	 * Switch to another language
	 *
	 * @param lang
	 * @return
	 * @throws IOException
	 */
	private URL switchLanguage(String lang) throws IOException {
		URL base = getBase();

		String front = makeRequest(base);

		Elements lis = Jsoup.parse(front).getElementsByClass(HtmlFpsMobility.LANG_LINK);
		for (Element li : lis) {
			if (li.text().equals(lang)) {
				String href = li.attr(Attribute.HREF.toString());
				return new URL(base, href);
			}
		}
		return base;
	}

	/**
	 * Store page containing datasets
	 *
	 * @param cache
	 * @throws java.io.IOException
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
		logger.info("Start scraping");
		Cache cache = getCache();

		Map<String, Page> front = cache.retrievePage(getBase());
		if (front.keySet().isEmpty()) {
			scrapePage(cache);
			front = cache.retrievePage(getBase());
		}
		// Calculate the number of datasets
		Page p = front.get(getDefaultLang());
		String datasets = p.getContent();
		Elements rows = Jsoup.parse(datasets).getElementsByTag(HTML.Tag.TR.toString());
		logger.info("Found {} datasets on page", String.valueOf(rows.size()));

		logger.info("Done scraping");
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
			Elements link, int i, String lang)
			throws MalformedURLException, RepositoryException {
		String href = link.first().attr(Attribute.HREF.toString());
		URL download = makeAbsURL(href);

		URL u = makeDistURL(i + "/" + lang);
		IRI dist = store.getURI(u.toString());
		logger.debug("Generating distribution {}", dist.toString());

		store.add(dataset, DCAT.HAS_DISTRIBUTION, dist);
		store.add(dist, RDF.TYPE, DCAT.DISTRIBUTION);
		store.add(dist, DCTERMS.LANGUAGE, MDR_LANG.MAP.get(lang));
		store.add(dist, DCTERMS.TITLE, link.first().ownText(), lang);
		store.add(dist, DCAT.ACCESS_URL, access);
		store.add(dist, DCAT.DOWNLOAD_URL, download);
		store.add(dist, DCAT.MEDIA_TYPE, getFileExt(href));
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
		URL u = makeDatasetURL(String.valueOf(i));
		IRI dataset = store.getURI(u.toString());
		logger.debug("Generating dataset {}", dataset.toString());

		Elements cells = row.getElementsByTag(Tag.TD.toString());
		String desc = cells.get(0).text();
		String title = desc;

		store.add(dataset, RDF.TYPE, DCAT.DATASET);
		store.add(dataset, DCTERMS.LANGUAGE, MDR_LANG.MAP.get(lang));
		store.add(dataset, DCTERMS.TITLE, title, lang);
		store.add(dataset, DCTERMS.DESCRIPTION, desc, lang);
		store.add(dataset, DCTERMS.IDENTIFIER, makeHashId(u.toString()));

		Elements link = cells.get(1).getElementsByTag(Tag.A.toString());
		generateDist(store, dataset, front, link, i, lang);
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
			URL front = p.getUrl();
			Elements rows = Jsoup.parse(html).body().getElementsByTag(Tag.TR.toString());

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
		logger.info("Generate DCAT");

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
	public HtmlFpsMobility(Properties prop) throws IOException {
		super(prop);
		setName("fpsmobility");
	}
}
