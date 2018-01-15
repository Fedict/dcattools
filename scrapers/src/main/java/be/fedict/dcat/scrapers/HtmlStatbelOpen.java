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
import java.util.regex.Pattern;

import javax.swing.text.html.HTML;
import javax.swing.text.html.HTML.Attribute;

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
 *
 * @author Bart.Hanssens
 */
public class HtmlStatbelOpen extends Html {

	private final Logger logger = LoggerFactory.getLogger(HtmlStatbelOpen.class);

	public final static String VIEW_HREF = "div.view-open-data h2 a";
	
	public final static String LANG_LINK = "section.block--language li";

	public final static String P_DESC = "article.node div.field--name-body p";
	public final static String DIV_TEMP = "div.field--name-field-period-manual";
	public final static String DIV_CAT = "div.field--name-field-category div";
	public final static String DIV_FILES = "div.field--name-field-downloads a";
	
	public final static Pattern YEAR_PAT
			= Pattern.compile(".*((18|19|20)[0-9]{2}-(19|20)[0-9]{2}).*");
	
	/**
	 * Get the URL of the page in another language
	 *
	 * @param page
	 * @param lang
	 * @return URL of the page in another language
	 * @throws IOException
	 */
	private URL switchLanguage(String page, String lang) throws IOException {
		Elements lis = Jsoup.parse(page).select(LANG_LINK);

		for (Element li : lis) {
			if (li.text().toLowerCase().equals(lang)) {
				String href = li.attr(HTML.Attribute.HREF.toString());
				if (href != null && !href.isEmpty()) {
					return makeAbsURL(href);
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
	 * Get the list of all the downloads (DCAT Dataset).
	 *
	 * @return List of URLs
	 * @throws IOException
	 */
	private List<URL> scrapeDatasetList() throws IOException {
		List<URL> urls = new ArrayList<>();

		URL base = getBase();
		
		// Go through paginated list
		for(int i = 0; ; i++) { 
			URL p = new URL(base + "?page=" + i);
			String page = makeRequest(p);
			Elements links = Jsoup.parse(page).select(VIEW_HREF);
			if (links == null || links.isEmpty()) {
				break;
			}
			
			for(Element link: links) {
				String href = link.attr(Attribute.HREF.toString());
				urls.add(new URL(base, href));
			}
			sleep();
		}
		return urls;
	}

	
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
		String href = link.attr(Attribute.HREF.toString());
		URL download = makeAbsURL(href);

		// important for EDP: does not like different datasets pointing to same distribution
		String id = makeHashId(dataset.toString()) + "/" + makeHashId(download.toString());
		IRI dist = store.getURI(makeDistURL(id).toString());
		logger.debug("Generating distribution {}", dist.toString());

		store.add(dataset, DCAT.HAS_DISTRIBUTION, dist);
		store.add(dist, RDF.TYPE, DCAT.DISTRIBUTION);
		store.add(dist, DCTERMS.LANGUAGE, MDR_LANG.MAP.get(lang));
		store.add(dist, DCTERMS.TITLE, link.ownText(), lang);
		store.add(dist, DCAT.ACCESS_URL, access);
		store.add(dist, DCAT.DOWNLOAD_URL, download);
		store.add(dist, DCAT.MEDIA_TYPE, link.ownText());
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
			Element h = doc.getElementsByTag(HTML.Tag.H1.toString()).first();
			if (h == null) {
				logger.warn("No H1 element");
				continue;
			}
			String title = h.text();
			// by default, also use the title as description
			String desc = title;

			Elements paras = doc.select(P_DESC);
			if (paras != null) {
				StringBuilder buf = new StringBuilder();
				for (Element para : paras) {
					buf.append(para.text()).append('\n');
				}
				if (buf.length() == 0) {
					buf.append(title);
				}
				desc = buf.toString();
			} else {
				logger.warn("No {} element", P_DESC);
			}

			Element t = doc.select(DIV_TEMP).first();
			if (t != null) {
				String temp = t.text().trim();
				if (temp.length() == 4) {
					temp = temp + "-" + temp;
				}
				generateTemporal(store, dataset, temp, YEAR_PAT, "-");
			}
			store.add(dataset, DCTERMS.LANGUAGE, MDR_LANG.MAP.get(lang));
			store.add(dataset, DCTERMS.TITLE, title, lang);
			store.add(dataset, DCTERMS.DESCRIPTION, desc, lang);

			Elements cats = doc.select(DIV_CAT);
			if (cats != null) {
				for (Element cat: cats) {
					store.add(dataset, DCAT.KEYWORD, cat.text().toLowerCase().trim(), lang);
				}
			}

			Elements links = doc.select(DIV_FILES);
			for (Element link : links) {
				generateDist(store, dataset, p.getUrl(), link, lang);
			}
		}
	}

	/**
	 * HTML parser for Statbel opendata publications
	 *
	 * @param caching DB cache file
	 * @param storage RDF back-end
	 * @param base base URL
	 */
	public HtmlStatbelOpen(File caching, File storage, URL base) {
		super(caching, storage, base);
		setName("statbelopen");
	}
}
