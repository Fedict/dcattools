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
 * Scraper FPS Finance
 *
 * @author Bart Hanssens <bart.hanssens@fedict.be>
 */
public class HtmlFodFin extends Html {

    private final static String LINK_THEME = "nav.block-menu-doormat ul.menu h2 a";
	private final static String LANG_LINK = "language-link";
	private final static String TITLE = "h1.page-title";
	//private final static String LIST_DATASETS = "section#content nav div.item-list li a";
	private final static String TABLE = "div.field-type-text-with-summary table tr";

	/**
	 * Switch to another language
	 *
	 * @param page
	 * @param lang language code
	 * @return
	 * @throws IOException
	 */
	private URL switchLanguage(String page, String lang) throws IOException {
		Elements lis = Jsoup.parse(page).getElementsByClass(LANG_LINK);
		
		for (Element li : lis) {
			if (li.text().equals(lang)) {
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

        // Get all the main themes
        Elements themes = Jsoup.parse(front).select(LINK_THEME);

        if (themes != null) {
            for (Element theme : themes) {
				logger.error("Data");
                String href = theme.attr(Attribute.HREF.toString());
                urls.add(makeAbsURL(href));
                sleep();
            }
        } else {
            logger.error("No themes {} found", LINK_THEME);
        }
        return urls;
    }

	
	/**
	 * Generate DCAT distribution.
	 *
	 * @param store RDF store
	 * @param dataset URI
	 * @param access access URL of the dataset
	 * @param row row element
	 * @param link link element
	 * @param lang language code
	 * @throws MalformedURLException
	 * @throws RepositoryException
	 */
	private void generateDist(Storage store, IRI dataset, URL access,
			String text, Element link, String lang)
			throws MalformedURLException, RepositoryException {
		String href = link.attr(Attribute.HREF.toString());
		URL download = makeAbsURL(href);

		String id = makeHashId(dataset.toString()) + "/" + makeHashId(download.toString());
		IRI dist = store.getURI(makeDistURL(id).toString());
		logger.debug("Generating distribution {}", dist.toString());

		store.add(dataset, DCAT.HAS_DISTRIBUTION, dist);
		store.add(dist, RDF.TYPE, DCAT.DISTRIBUTION);
		store.add(dist, DCTERMS.LANGUAGE, MDR_LANG.MAP.get(lang));
		store.add(dist, DCTERMS.TITLE, link.ownText(), lang);
		store.add(dist, DCTERMS.DESCRIPTION, text, lang);
		store.add(dist, DCAT.ACCESS_URL, access);
		store.add(dist, DCAT.DOWNLOAD_URL, download);
		store.add(dist, DCAT.MEDIA_TYPE, getFileExt(href));
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
			Element h = doc.select(TITLE).first();
			if (h == null) {
				logger.warn("No H1 element");
				continue;
			}
			String title = h.text();
			String desc = "";

			Elements rows = doc.select(TABLE);

			if (rows != null && !rows.isEmpty()) {
				store.add(dataset, DCTERMS.LANGUAGE, MDR_LANG.MAP.get(lang));
				store.add(dataset, DCTERMS.TITLE, title, lang);
				
				for (Element row : rows) {
					String text = null;
					Element el = row.getElementsByTag(Tag.TD.toString()).first();
					if (el != null) {
						text = el.ownText();
						desc += text + "\n";
					}
					// a row may contain multiple downloads / distributions
					Elements hrefs = row.getElementsByTag(Tag.A.toString());
					for (Element href: hrefs) {
						generateDist(store, dataset, p.getUrl(), text, href, lang);
					}
				}
			}
			if (desc.isEmpty()) {
				desc = title;
			}
			store.add(dataset, DCTERMS.DESCRIPTION, desc, lang);
		}
	}

	/**
	 * HTML scraper FPS Finance.
	 *
	 * @param caching DB cache file
	 * @param storage RDF backing file
	 * @param base base URL
	 */
	public HtmlFodFin(File caching, File storage, URL base) {
		super(caching, storage, base);
		setName("fpsfinance");
	}
}
