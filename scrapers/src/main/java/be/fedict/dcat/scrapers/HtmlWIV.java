/*
 * Copyright (c) 2020, FPS BOSA DG DT
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
 * @author Bart Hanssens <bart.hanssens@fedict.be>
 */
public class HtmlWIV extends Html {
	private final static String H_TITLE = "h2";
	private final static String SECTION_P = "section#Covid p";
	//private final static String DIST_LINKS = "section#Data ul li";
	private final static String DIST_ROW = "table.table tbody tr";
	
	private final static String HREFS = "a";

	@Override
	protected List<URL> scrapeDatasetList() throws IOException {
		List<URL> urls = new ArrayList<>();

		urls.add(getBase());
		return urls;
	}
	
	/**
	 * Generate DCAT distribution.
	 *
	 * @param store RDF store
	 * @param dataset URI
	 * @param access access URL of the dataset
	 * @param row row element
	 * @param lang language code
	 * @throws MalformedURLException
	 * @throws RepositoryException
	 */
	private void generateDist(Storage store, IRI dataset, URL access,
			Elements rows, String lang) throws MalformedURLException, RepositoryException {

		for (Element row: rows) {
			Elements cols = row.select("td");
			String title = cols.get(0).ownText().trim();
			Element link = cols.get(1).selectFirst(HREFS);

			String href = link.attr(Attribute.HREF.toString());
			URL download = makeAbsURL(href);

			String ftype = link.text().trim();

			URL u = makeDistURL(makeHashId(title) + "/" + ftype);
			IRI dist = store.getURI(u.toString());
			logger.debug("Generating distribution {}", dist.toString());

			store.add(dataset, DCAT.HAS_DISTRIBUTION, dist);
			store.add(dist, RDF.TYPE, DCAT.DISTRIBUTION);
			store.add(dist, DCTERMS.LANGUAGE, MDR_LANG.MAP.get(lang));
			store.add(dist, DCTERMS.TITLE, title, lang);
			store.add(dist, DCAT.ACCESS_URL, access);
			store.add(dist, DCAT.DOWNLOAD_URL, download);
			store.add(dist, DCAT.MEDIA_TYPE, ftype.toLowerCase());
		}
	}

	/**
	 * Generate DCAT Dataset
	 *
	 * @param store RDF store
	 * @param id dataset id
	 * @param page
	 * @throws MalformedURLException
	 * @throws RepositoryException
	 */
	@Override
	protected void generateDataset(Storage store, String id, Map<String, Page> page)
			throws MalformedURLException, RepositoryException {
		String lang = getDefaultLang();

		Page p = page.getOrDefault("", new Page());
		String html = p.getContent();
		URL u = p.getUrl();

		Element content = Jsoup.parse(html).body();

		IRI dataset = store.getURI(makeDatasetURL(makeHashId(id)).toString());
		logger.debug("Generating dataset {}", dataset.toString());

		Element h2 = content.select(H_TITLE).first();
		if (h2 == null) {
			logger.warn("Empty title, skipping");
			return;
		}
		String title = h2.text().trim();

		Element div = content.select(SECTION_P).first();
		String desc = (div != null) ? div.text() : title;

		store.add(dataset, RDF.TYPE, DCAT.DATASET);
		store.add(dataset, DCTERMS.LANGUAGE, MDR_LANG.MAP.get(lang));
		store.add(dataset, DCTERMS.TITLE, title, lang);
		store.add(dataset, DCTERMS.DESCRIPTION, desc, lang);
		store.add(dataset, DCTERMS.IDENTIFIER, makeHashId(id));
		store.add(dataset, DCAT.LANDING_PAGE, u);

		Elements dist = content.select(DIST_ROW);
		generateDist(store, dataset, u, dist, lang);
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
		List<URL> urls = cache.retrieveURLList();
		for (URL u : urls) {
			Map<String, Page> page = cache.retrievePage(u);
			generateDataset(store, u.toString(), page);
		}
		generateCatalog(store);
	}

	/**
	 * HTML scraper FPS Mobility.
	 *
	 * @param caching DB cache file
	 * @param storage RDF backing file
	 * @param base base URL
	 */
	public HtmlWIV(File caching, File storage, URL base) {
		super(caching, storage, base);
		setName("wiv");
	}
}
