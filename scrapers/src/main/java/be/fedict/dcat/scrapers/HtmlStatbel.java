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

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.swing.text.html.HTML;
import javax.swing.text.html.HTML.Attribute;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Abstract Statbel scraper.
 *
 * @author Bart Hanssens <bart.hanssens@fedict.be>
 */
public abstract class HtmlStatbel extends Html {
	private final Logger logger = LoggerFactory.getLogger(HtmlStatbel.class);
	
	public final static String LANG_LINK = "section.block--language li a";
	
	/**
	 * Get the URL of the page in another language
	 *
	 * @param page
	 * @param lang
	 * @return URL of the page in another language
	 * @throws IOException
	 */
	private URL switchLanguage(String page, String lang) throws IOException {
		Elements hrefs = Jsoup.parse(page).select(LANG_LINK);

		for (Element href : hrefs) {
			if (href.text().trim().toLowerCase().equals(lang)) {
				String link = href.attr(HTML.Attribute.HREF.toString());
				if (link != null && !link.isEmpty()) {
					return makeAbsURL(link);
				}
			}
		}
		logger.warn("No {} translation for page", lang);
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
	 * @param selector
	 * @return List of URLs
	 * @throws IOException
	 */
	protected List<URL> scrapeDatasetList(String selector) throws IOException {
		List<URL> urls = new ArrayList<>();

		URL base = getBase();
		String front = makeRequest(base);

		// Select the correct page from dropdown-list, displaying all items
		Elements links = Jsoup.parse(front).select(selector);
		if (links != null) {
			for (Element link: links) {
				String href = link.attr(Attribute.HREF.toString());
				urls.add(makeAbsURL(href));
			}
		} else {
			logger.error("No themes {} found", selector);
		}
		return urls;
	}

	/**
	 * Scrape the site.
	 *
	 * @param selector
	 * @throws IOException
	 */
	protected void scrape(String selector) throws IOException {
		logger.info("Start scraping");
		Cache cache = getCache();

		List<URL> urls = cache.retrieveURLList();
		if (urls.isEmpty()) {
			urls = scrapeDatasetList(selector);
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
	 * HTML parser for Statbel publications
	 *
	 * @param caching DB cache file
	 * @param storage RDF back-end
	 * @param base base URL
	 */
	public HtmlStatbel(File caching, File storage, URL base) {
		super(caching, storage, base);
		setName("statbelpub");
	}
}
