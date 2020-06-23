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

import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.vocabulary.DCAT;
import org.eclipse.rdf4j.model.vocabulary.DCTERMS;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.repository.RepositoryException;

/**
 * Scraper for InnovationData website.
 *
 * @author Bart Hanssens <bart.hanssens@fedict.be>
 */
public class HtmlInnovation extends Html {
	private final static String H_TITLE = "div.headline h4";
	private final static String DIV_DESC = "div#metadata";
	private final static String LIST_CATS = "ul.breadcrumb li";
	private final static String LINK_DATASETS = "div.toc ul li a";
	private final static String LINK_DIST = "div.magazine-sb-categories a.btn-u";

	/**
	 * Get the ID from path
	 *
	 * @param url URL
	 * @return ID as string
	 */
	private String getIDParam(URL url) {
		String u = url.getPath();
		String[] parts = u.split("/");
		if (parts.length >= 3) {
			return parts[2];
		}
		logger.error("No path id found for {}", url);
		return "";
	}

	/**
	 * Get the list of all the data sets (DCAT Dataset).
	 *
	 * @return List of URLs
	 * @throws IOException
	 */
	@Override
	protected List<URL> scrapeDatasetList() throws IOException {
		List<URL> urls = new ArrayList<>();

		URL base = getBase();
		String front = makeRequest(base);
		Elements links = Jsoup.parse(front).select(LINK_DATASETS);

		for (Element link : links) {
			String href = link.attr(HTML.Attribute.HREF.toString());
			urls.add(makeAbsURL(href));
		}
		return urls;
	}

	/**
	 * Generate DCAT distribution.
	 *
	 * @param store RDF store
	 * @param dataset URI
	 * @param access URL of the front page
	 * @param link link element
	 * @param id id of the dataset
	 * @param lang language code
	 * @throws MalformedURLException
	 * @throws RepositoryException
	 */
	private void generateDist(Storage store, IRI dataset, URL access,
			Element link, String id, String lang)
			throws MalformedURLException, RepositoryException {
		if (link == null) {
			logger.error("Empty link for " + access.toString());
			return;
		}
		String href = link.attr(HTML.Attribute.HREF.toString());
		URL download = makeAbsURL(href);

		URL u = makeDistURL(id);
		IRI dist = store.getURI(u.toString());
		logger.debug("Generating distribution {}", dist.toString());

		String title = link.text().trim();
		store.add(dataset, DCAT.HAS_DISTRIBUTION, dist);
		store.add(dist, RDF.TYPE, DCAT.DISTRIBUTION);
		store.add(dist, DCTERMS.LANGUAGE, MDR_LANG.MAP.get(lang));
		store.add(dist, DCTERMS.TITLE, title, lang);
		store.add(dist, DCAT.ACCESS_URL, access);
		store.add(dist, DCAT.DOWNLOAD_URL, download);
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

		String param = getIDParam(u);
		IRI dataset = store.getURI(makeDatasetURL(param).toString());
		logger.debug("Generating dataset {}", dataset.toString());

		Element h1 = content.select(H_TITLE).first();
		if (h1 == null) {
			logger.warn("Empty title, skipping");
			return;
		}
		String title = h1.text().trim();

		Element div = content.select(DIV_DESC).first();
		String desc = (div != null) ? div.text() : title;

		store.add(dataset, RDF.TYPE, DCAT.DATASET);
		store.add(dataset, DCTERMS.LANGUAGE, MDR_LANG.MAP.get(lang));
		store.add(dataset, DCTERMS.TITLE, title, lang);
		store.add(dataset, DCTERMS.DESCRIPTION, desc, lang);
		store.add(dataset, DCTERMS.IDENTIFIER, makeHashId(u.toString()));
		store.add(dataset, DCAT.LANDING_PAGE, u);

		Elements cats = content.select(LIST_CATS);
		for (Element cat : cats) {
			store.add(dataset, DCAT.KEYWORD, cat.text(), lang);
		}

		Element dist = content.select(LINK_DIST).first();
		generateDist(store, dataset, u, dist, param, lang);
	}


	/**
	 * HTML scraper InnovationData.
	 *
	 * @param caching DB cache file
	 * @param storage RDF back-end
	 * @param base base URL
	 */
	public HtmlInnovation(File caching, File storage, URL base) {
		super(caching, storage, base);
		setName("innovation");
	}
}
