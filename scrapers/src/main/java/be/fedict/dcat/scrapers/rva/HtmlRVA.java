/*
 * Copyright (c) 2017, FPS BOSA DG DT
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
package be.fedict.dcat.scrapers.rva;

import be.fedict.dcat.scrapers.Cache;
import be.fedict.dcat.scrapers.Page;
import be.fedict.dcat.helpers.Storage;
import be.fedict.dcat.scrapers.Html;

import be.fedict.dcat.vocab.MDR_LANG;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import javax.swing.text.html.HTML;
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
 * Scraper RVA-ONSS (work in progress)
 *
 * @author Bart Hanssens
 */
public class HtmlRVA extends Html {

	public final static String LANG_LINK = "language-link";
	
	public final static String ITEM_LIST = "div.view-navigation-view div.item-list a";
	public final static String NODE_PAGE = "div.node-page";
	

	/**
	 * Switch to another language
	 *
	 * @param lang
	 * @return
	 * @throws IOException
	 */
	private URL switchLanguage(String lang, String html) throws IOException {
		URL base = getBase();

		Elements lis = Jsoup.parse(html).getElementsByClass(HtmlRVA.LANG_LINK);
		for (Element li : lis) {
			if (li.text().equals(lang)) {
				String href = li.attr(Attribute.HREF.toString());
				return makeAbsURL(href);
			}
		}
		return base;
	}
	
	/**
	 * Scrape dataset
	 *
	 * @param u
	 * @throws IOException
	 */
	@Override
	protected void scrapeDataset(URL u) throws IOException {
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
	protected List<URL> scrapeDatasetList() throws IOException {
		List<URL> urls = new ArrayList<>();

		URL base = getBase();
		String front = makeRequest(base);

		// Select the correct page from sidebar menu
		Elements items = Jsoup.parse(front).select(ITEM_LIST);
		if (!items.isEmpty()) {
			for (Element item: items) {
				urls.add(makeAbsURL(item.attr(HTML.Attribute.HREF.toString())));
			}
		} else {
			logger.error("Category {} not found", ITEM_LIST);
		} 
		return urls;
	}

	/**
	 * Generate DCAT distribution.
	 *
	 * @param store RDF store
	 * @param dataset URI
	 * @param access access URL of the dataset
	 * @param link link element
	 * @param code dataset code
	 * @param lang language code
	 * @throws MalformedURLException
	 * @throws RepositoryException
	 */
	private void generateDist(Storage store, IRI dataset, URL access,
			Elements link, String code, String lang)
			throws MalformedURLException, RepositoryException {
		String href = link.first().attr(Attribute.HREF.toString());
		URL download = makeAbsURL(href);

		// file type e.g. in "Link (pdf)"
		String txt = link.first().text();
		String ftype = txt.replaceAll("(\\w+\\s*\\()(\\w+)\\)", "$2");
		
		URL u = makeDistURL(code + "/" + lang);
		IRI dist = store.getURI(u.toString());
		logger.debug("Generating distribution {}", dist.toString());

		store.add(dataset, DCAT.HAS_DISTRIBUTION, dist);
		store.add(dist, RDF.TYPE, DCAT.DISTRIBUTION);
		store.add(dist, DCTERMS.LANGUAGE, MDR_LANG.MAP.get(lang));
		store.add(dist, DCTERMS.TITLE, link.first().ownText(), lang);
		store.add(dist, DCAT.ACCESS_URL, access);
		store.add(dist, DCAT.DOWNLOAD_URL, download);
		store.add(dist, DCAT.MEDIA_TYPE, ftype.toLowerCase());
	}


	/**
	 * HTML scraper RVA/ONEM
	 *
	 * @param prop
	 * @throws IOException
	 */
	public HtmlRVA(Properties prop) throws IOException {
		super(prop);
		setName("rva");
	}

	@Override
	protected void generateDataset(Storage store, String id, Map<String, Page> page) throws MalformedURLException, RepositoryException {
		throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
	}
}
