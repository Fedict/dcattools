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
package be.gov.data.scrapers.podmiis;

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
 * POD MI-IS scraper.
 *
 * @see https://www.mi-is.be/
 * @author Bart Hanssens
 */
public class HtmlPodMiis extends Html {

	public final static String ABBR = "abbr";
	
	public final static String ITEM_SELECT = "section.sidebar ul ul li a";

	public final static String LANG_SELECT = "ul.nav li.dropdown ul.dropdown-menu li a";

	public final static String MODAL_LABEL = "myModalLabel";
	public final static String MODAL_BODY = "modal-body";
	public final static String DOWNLOAD = "download";
	public final static String SLIDER = "month-slider";
	public final static String SLIDER_DATA = "data-slider-ticks-labels";
	public final static String PICKER = "picker";
	public final static String PERIOD_PICKER = "period-picker";
	public final static String HREF_DATA = "data-href";
	public final static String PLACEHOLDER = "__period__";

	/**
	 * Get the URL of the page in another language
	 *
	 * @param page
	 * @param lang
	 * @return URL of the page in another language
	 * @throws IOException
	 */
	private URL switchLanguage(String page, String lang) throws IOException {
		Elements lis = Jsoup.parse(page).select(LANG_SELECT);

		for (Element li : lis) {
			String l = li.getElementsByTag(ABBR).first().attr(HTML.Attribute.LANG.toString());
			if (l.equals(lang)) {
				String href = li.attr(HTML.Attribute.HREF.toString());
				if (href != null && !href.isEmpty()) {
					return makeAbsURL(href);
				}
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
	 * Get the list of all the downloads (DCAT Dataset).
	 *
	 * @return List of URLs
	 * @throws IOException
	 */
	@Override
	protected List<URL> scrapeDatasetList() throws IOException {
		List<URL> urls = new ArrayList<>();

		URL base = getBase();
		String front = makeRequest(base);

		// Select the correct page from sidebar menu
		Elements items = Jsoup.parse(front).select(ITEM_SELECT);
		if (!items.isEmpty()) {
			for (Element item: items) {
				urls.add(makeAbsURL(item.attr(HTML.Attribute.HREF.toString())));
			}
		} else {
			LOG.error("Category {} not found", ITEM_SELECT);
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
	 * @param range range of time
	 * @param lang language code
	 * @throws MalformedUrlException
	 * @throws RepositoryException
	 */
	private void generateDist(Storage store, IRI dataset, URL access, Element link, 
			String range, String lang) throws MalformedURLException, RepositoryException {
		String href = link.attr(HREF_DATA).replace(PLACEHOLDER, range);
		URL download = makeAbsURL(href);

		// important for EDP: does not like different datasets pointing to same distribution
		String id = hash(dataset.toString()) + "/" + hash(download.toString());
		IRI dist = store.getURI(makeDistURL(id).toString());
		LOG.debug("Generating distribution {}", dist.toString());

		String title = link.ownText().isEmpty() ? range : link.ownText();
		
		store.add(dataset, DCAT.HAS_DISTRIBUTION, dist);
		store.add(dist, RDF.TYPE, DCAT.DISTRIBUTION);
		store.add(dist, DCTERMS.LANGUAGE, MDR_LANG.MAP.get(lang));
		store.add(dist, DCTERMS.TITLE, title, lang);
		store.add(dist, DCAT.ACCESS_URL, access);
		store.add(dist, DCAT.DOWNLOAD_URL, download);
		store.add(dist, DCAT.MEDIA_TYPE, getFileExt(href));
	}

	/**
	 * Create description from paragraphs
	 * 
	 * @param el HTML element containing p
	 * @param title title to be used as default
	 * @return description
	 */
	private String buildDesc(Element el, String title) {
		if (el == null) {
			LOG.warn("No {} element", MODAL_BODY);
			return title;
		}
		Elements paras = el.getElementsByTag(Tag.P.toString());
		if (paras != null) {
			StringBuilder buf = new StringBuilder();
			for (Element para : paras) {
				buf.append(para.text()).append('\n');
			}
			if (buf.length() > 0) {
				return buf.toString();
			}
		}
		return title;
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
			Element h = doc.getElementById(MODAL_LABEL);
			if (h == null) {
				LOG.warn("No H1 element");
				continue;
			}
			String title = h.text();
			// by default, also use the title as description

			Element divmain = doc.getElementsByClass(MODAL_BODY).first();
			String desc = buildDesc(divmain, title);

			store.add(dataset, DCTERMS.LANGUAGE, MDR_LANG.MAP.get(lang));
			store.add(dataset, DCTERMS.TITLE, title, lang);
			store.add(dataset, DCTERMS.DESCRIPTION, desc, lang);
			store.add(dataset, DCAT.LANDING_PAGE, p.getUrl());
			store.add(store.getURI(p.getUrl().toString()), DCTERMS.LANGUAGE, MDR_LANG.MAP.get(lang));
			Element link = doc.getElementById(DOWNLOAD);
					
			// Monthly
			Element slider = doc.getElementById(SLIDER);
			if (slider != null) {
				// extract min/max date from data attribute
				String months = slider.attr(SLIDER_DATA).trim()
											.replaceFirst("\\[", "")
											.replace("\"", "")
											.replaceFirst("]", "");
				String[] split = months.split(",");
				
				if (split.length > 1) {
					String start = split[0].replaceAll("\\\\/", "-");
					String end = split[split.length-1].replaceAll("\\\\/", "-");
					
					generateTemporal(store, dataset, start, end);
					generateDist(store, dataset, p.getUrl(), link, 
												end.replace("-", ""), lang);
				} else {
					LOG.warn("No distribution");
				}
				store.add(dataset, DCTERMS.ACCRUAL_PERIODICITY, "M");	
			}
		} 
	}

	/**
	 * Constructor
	 *
	 * @param prop
	 * @throws IOException
	 */
	public HtmlPodMiis(Properties prop) throws IOException {
		super(prop);
		setName("podmiis");
	}
}
