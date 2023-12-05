/*
 * Copyright (c) 2021, FPS BOSA DG DT
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
package be.gov.data.scrapers.indicators;

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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
 * Scraper Federal Plan Bureau Indicators
 *
 * @see https://indicators.be/
 * @author Bart Hanssens
 */
public class HtmlIndicators extends Html {

    private final static String LINK_DATASETS = "div.row div.col-md-4 div.tag-box ul li a";
	private final static String LANG_LINK = "div.top ul.loginbar ul.lenguages li a";
	private final static String TITLE = "div.headline h4";
	private final static String DESC = "div.tab-content div#metadata p";

	private final static Pattern PATTERN = Pattern.compile(".*/(G\\w*)/.*");

	/**
	 * Switch to another language
	 *
	 * @param page
	 * @param lang language code
	 * @return
	 * @throws IOException
	 */
	private URL switchLanguage(String page, String lang) throws IOException {
		Elements lis = Jsoup.parse(page).select(LANG_LINK);
		
		for (Element li : lis) {
			if (li.text().trim().equals(lang)) {
				String href = li.attr(Attribute.HREF.toString());
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
	 * Get the list of all the SDG datasets.
	 *
	 * @return list of dataset URLs
	 * @throws IOException
	 */
	@Override
	protected List<URL> scrapeDatasetList() throws IOException {
        List<URL> urls = new ArrayList<>();

        URL base = getBase();
        String front = makeRequest(base);

        // Get all the main themes
        Elements datasets = Jsoup.parse(front).select(LINK_DATASETS);

        if (datasets != null) {
            for (Element dataset : datasets) {
                String href = dataset.attr(Attribute.HREF.toString());
                urls.add(makeAbsURL(href));
                sleep();
            }
        } else {
            logger.error("No datasets {} found", LINK_DATASETS);
        }
        return urls;
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
		
		for (String lang : getAllLangs()) {
			Page p = page.get(lang);
			if (p == null) {
				logger.warn("Page not available in {}", lang);
				continue;
			}
			String html = p.getContent();

			Element doc = Jsoup.parse(html).body();
			if (doc == null) {
				logger.warn("No body element");
				continue;
			}
			String title = doc.select(TITLE).first().text();
			String desc = doc.select(DESC).first().text();
			
			IRI dataset = store.getURI(makeDatasetURL(id).toString());
			logger.info("Generating dataset {}", dataset.toString());
		
			String link = p.getUrl().toString();
			Matcher matcher = PATTERN.matcher(link);
			String keyw = matcher.matches() ? matcher.group(1) : "";
			
			store.add(dataset, RDF.TYPE, DCAT.DATASET);
			store.add(dataset, DCTERMS.IDENTIFIER, id);
			store.add(dataset, DCTERMS.LANGUAGE, MDR_LANG.MAP.get(lang));
			store.add(dataset, DCTERMS.TITLE, title, lang);
			store.add(dataset, DCTERMS.DESCRIPTION, desc, lang);
			store.add(dataset, DCAT.KEYWORD, keyw);
		}
	}

	/**
	 * Constructor
	 *
	 * @param prop
	 * @throws IOException
	 */
	public HtmlIndicators(Properties prop) throws IOException {
		super(prop);
		setName("indicators");
	}
}
