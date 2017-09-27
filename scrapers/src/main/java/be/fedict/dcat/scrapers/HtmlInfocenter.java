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
import be.fedict.dcat.vocab.MDR_LANG;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import javax.swing.text.html.HTML;
import javax.swing.text.html.HTML.Tag;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.vocabulary.DCAT;
import org.eclipse.rdf4j.model.vocabulary.DCTERMS;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.repository.RepositoryException;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Infocenter / federal statistics scraper.
 *
 * @author Bart.Hanssens
 */
public class HtmlInfocenter extends Html {

	private final Logger logger = LoggerFactory.getLogger(HtmlInfocenter.class);

	public final static String LANG_LINK = "blgm_lSwitch";

	private final static String HEADER = "h1 a[href]";
	private final static String LINKS_DATASETS = "div.menu ul.no-style a[href]";
	private final static String LINKS_SECOND = "div.nav-stats-wrapper ul.stats-second-menu li a";

	/**
	 * Get the URL of the page in another language
	 *
	 * @param page
	 * @param lang
	 * @return URL of the page in another language
	 * @throws IOException
	 */
	private URL switchLanguage(String page, String lang) throws IOException {
		Elements lis = Jsoup.parse(page)
				.getElementsByClass(HtmlInfocenter.LANG_LINK);

		for (Element li : lis) {
			if (li.text().equals(lang)) {
				String href = li.attr(HTML.Attribute.HREF.toString());
				if (href != null && !href.isEmpty()) {
					return new URL(href);
				}
			}
		}
		logger.warn("No {} translation for page {}", lang, page);
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
			if (!lang.equals(deflang)) {
				URL url = switchLanguage(html, lang);
				if (url != null) {
					String body = makeRequest(url);
					cache.storePage(u, lang, new Page(url, body));
				}
				sleep();
			}
		}
	}

	/**
	 * Get the list of all the statistics.
	 *
	 * @return list of category URLs
	 * @throws IOException
	 */
	private List<URL> scrapeDatasetList() throws IOException {
		List<URL> urls = new ArrayList<>();

		URL base = getBase();
		String front = makeRequest(base);
		Elements links = Jsoup.parse(front).select(LINKS_DATASETS);

		for (Element link : links) {
			String href = link.attr(HTML.Attribute.HREF.toString());
			urls.add(makeAbsURL(href.substring(0, href.lastIndexOf("/"))));
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
	private void generateDist(Storage store, IRI dataset, URL access, Element link,
			String lang) throws MalformedURLException, RepositoryException {
		String href = link.attr(HTML.Attribute.HREF.toString());

		String id = makeHashId(href);
		IRI dist = store.getURI(makeDistURL(id).toString());
		logger.debug("Generating distribution {}", dist.toString());

		store.add(dataset, DCAT.HAS_DISTRIBUTION, dist);
		store.add(dist, RDF.TYPE, DCAT.DISTRIBUTION);
		store.add(dist, DCTERMS.LANGUAGE, MDR_LANG.MAP.get(lang));
		store.add(dist, DCTERMS.TITLE, link.ownText(), lang);
		store.add(dist, DCAT.ACCESS_URL, access);
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
	public void generateDataset(Storage store, String id, Map<String, Page> page)
			throws MalformedURLException, RepositoryException {
		IRI dataset = store.getURI(makeDatasetURL(id).toString());
		logger.info("Generating dataset {}", dataset.toString());

		store.add(dataset, RDF.TYPE, DCAT.DATASET);
		store.add(dataset, DCTERMS.IDENTIFIER, id);

		for (String lang : getAllLangs()) {
			Page p = page.get(lang);
			if (p == null) {
				logger.warn("Page {} not available in {}", dataset.toString(), lang);
				continue;
			}
			String html = p.getContent();

			Element doc = Jsoup.parse(html).body();
			if (doc == null) {
				logger.warn("No body element");
				continue;
			}
			Element h = doc.getElementsByTag(Tag.H2.toString()).first();
			if (h == null) {
				logger.warn("No H2 element");
				continue;
			}
			String title = h.text();
			// by default, also use the title as description
			String desc = title;

			Elements navs = doc.select(LINKS_SECOND);
			if (navs != null && !navs.isEmpty()) {
				StringBuilder buf = new StringBuilder();
				String h1 = doc.select(HEADER).text();
				if (h1 != null) {
					buf.append(h1).append(":").append('\n');
				} else {
					logger.warn("No H1 element");
				}
				for (Element nav : navs) {
					buf.append("- ").append(nav.text()).append('\n');
				}
				desc = buf.toString();
			} else {
				logger.warn("No second menu element");
			}

			store.add(dataset, DCTERMS.LANGUAGE, MDR_LANG.MAP.get(lang));
			store.add(dataset, DCTERMS.TITLE, title, lang);
			store.add(dataset, DCTERMS.DESCRIPTION, desc, lang);

			if (navs != null) {
				for (Element nav : navs) {
					generateDist(store, dataset, p.getUrl(), nav, lang);
				}
			}
		}
	}

	/**
	 * Constructor
	 *
	 * @param caching DB cache file
	 * @param storage SDB file to be used as triple store backend
	 * @param base base URL
	 */
	public HtmlInfocenter(File caching, File storage, URL base) {
		super(caching, storage, base);
		setName("infocenter");
	}
}
