/*
 * Copyright (c) 2023, FPS BOSA DG DT
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
package be.gov.data.scrapers.pensionstat;

import be.gov.data.scrapers.Cache;
import be.gov.data.scrapers.Page;
import be.gov.data.helpers.Storage;
import be.gov.data.scrapers.Html;

import be.gov.data.dcat.vocab.MDR_LANG;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.regex.Pattern;

import javax.swing.text.html.HTML;
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
 * Scraper Sidegis PensionStat
 *
 * @see https://pensionstat.be/
 * @author Bart Hanssens
 */
public class HtmlPensionstat extends Html {

	private final static String LANG_LINK = "nav ul li div[aria-labelledby='dropdownLocales'] a";
	private final static String LINKS_DATASETS = "nav div li.pl-4 a";
	private final static String DOWNLOADS = "div.donwloads div.py-2"; //typo
	private final static String DOWNLOAD_YEAR = "div:first-child";
	private final static String DESC_PARA_H1 = "main div h1 + p";
	private final static String DESC_PARA_H2 = "main div h2 + p";
	private final static Pattern YEAR = Pattern.compile("20[0-9]{2}");
	

	/**
	 * Switch to another language
	 *
	 * @param lang
	 * @return
	 * @throws IOException
	 */
	private URL switchLanguage(String page, String lang) throws IOException {
		Elements links = Jsoup.parse(page).select(LANG_LINK);

		for (Element link : links) {
			String l = link.attr(HTML.Attribute.LANG.toString());
			if (l != null && !l.isEmpty() && l.toLowerCase().equals(lang)) {
				return makeAbsURL(link.attr(HTML.Attribute.HREF.toString()));
			}
		}
		LOG.warn("No {} translation for page {}", lang, page);
		return null;
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
	 * Get the list of all the statistics.
	 *
	 * @return list of category URLs
	 * @throws IOException
	 */
	@Override
	protected List<URL> scrapeDatasetList() throws IOException {
		List<URL> urls = new ArrayList<>();

		URL base = getBase();
		String front = makeRequest(base);
		Elements links = Jsoup.parse(front).select(LINKS_DATASETS);

		for (Element link : links) {
			String href = link.attr(HTML.Attribute.HREF.toString());
			urls.add(makeAbsURL(href));
		}
		return urls;
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
	private void generateDist(Storage store, IRI dataset, URL access, String prefix, Element link,
			String lang) throws MalformedURLException, RepositoryException {
		String l = link.attr(HTML.Attribute.HREF.toString()).replace("../", "");
		String href = makeAbsURL(l).toString();

		String id = hash(href);
		IRI dist = makeDistIRI(id);
		LOG.debug("Generating distribution {}", dist.toString());
		
		String format = link.ownText();

		store.add(dataset, DCAT.HAS_DISTRIBUTION, dist);
		store.add(dist, RDF.TYPE, DCAT.DISTRIBUTION);
		store.add(dist, DCTERMS.LANGUAGE, MDR_LANG.MAP.get(lang));
		store.add(dist, DCTERMS.TITLE, String.join(" ", prefix, format).strip(), lang);
		store.add(dist, DCAT.ACCESS_URL, access);
		store.add(dist, DCAT.DOWNLOAD_URL, store.getURI(href));
		store.add(dist, DCTERMS.FORMAT, format);
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
		IRI dataset = makeDatasetIRI(id);
		LOG.info("Generating dataset {}", dataset.toString());

		store.add(dataset, RDF.TYPE, DCAT.DATASET);
		store.add(dataset, DCTERMS.IDENTIFIER, id);

		for (String lang : getAllLangs()) {
			Page p = page.get(lang);
			if (p == null) {
				LOG.warn("Page {} not available in {}", dataset.toString(), lang);
				continue;
			}
			String html = p.getContent();

			Element doc = Jsoup.parse(html).body();
			if (doc == null) {
				LOG.warn("No body element");
				continue;
			}
			Element h = doc.getElementsByTag(Tag.H1.toString()).first();
			if (h == null) {
				LOG.warn("No H2 element");
				continue;
			}
			String title = h.text();
			// by default, also use the title as description
			String desc = "";

			Elements paras1 = doc.select(DESC_PARA_H1);
			if (paras1 != null && !paras1.isEmpty()) {
				desc += paras1.text();
			}
			Elements paras2 = doc.select(DESC_PARA_H2);
			if (paras2 != null && !paras2.isEmpty()) {
				desc += "\n" + paras2.text();
			}
			if (desc.isEmpty()) {
				LOG.warn("No description");
				desc = title;
			}

			store.add(dataset, DCTERMS.LANGUAGE, MDR_LANG.MAP.get(lang));
			store.add(dataset, DCTERMS.TITLE, title, lang);
			store.add(dataset, DCTERMS.DESCRIPTION, desc, lang);
			store.add(dataset, DCAT.LANDING_PAGE, p.getUrl());
			store.add(store.getURI(p.getUrl().toString()), DCTERMS.LANGUAGE, MDR_LANG.MAP.get(lang));

			Element download = doc.selectFirst(DOWNLOADS);
			if (download != null) {
				Elements years = download.children().select(DOWNLOAD_YEAR);
				for (Element year : years) {
					String prefix = year.ownText();
					Elements files = download.select(Tag.A.toString());
					for (Element file: files) {
						generateDist(store, dataset, p.getUrl(), prefix, file, lang);
					}
				}
				if (!years.isEmpty()) {
					String first = years.first().ownText();
					String last = years.last().ownText();

					if (YEAR.matcher(first).matches() && YEAR.matcher(last).matches()) {
						generateTemporal(store, dataset, last + "-01-01", first + "-01-01");
					}
				}
			}
		}
	}

	/**
	 * Constructor
	 *
	 * @param prop
	 * @throws IOException
	 */
	public HtmlPensionstat(Properties prop) throws IOException {
		super(prop);
		setName("pensionstat");
	}
}
