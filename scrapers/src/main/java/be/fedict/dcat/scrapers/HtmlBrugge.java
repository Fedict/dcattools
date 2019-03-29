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
import javax.swing.text.html.HTML.Attribute;
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
 * Scraper for website Brugge.
 *
 * @author Bart.Hanssens
 */
public class HtmlBrugge extends Html {

	private final Logger logger = LoggerFactory.getLogger(HtmlBrugge.class);

	private final static String LINKS_DATASETS = "div.user-content div a:eq(0)[href]";
	private final static String LINK_DATASET = "div.user-content h4";
	private final static String NAME_DATASET = "a[name]";

	private final static String SIBL_DESC = "em :contains(Omschrijving)";
	private final static String SIBL_FMTS = "em :contains(Bestandsformaten)";
	private final static String DIST_HREF = "a";
	
	/**
	 * Get the list of all the categories.
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
	 * Generate DCAT distribution.
	 *
	 * @param store RDF store
	 * @param dataset URI
	 * @param name short name
	 * @param access URL of the acess page
	 * @param link download link element
	 * @param lang language code
	 * @throws MalformedURLException
	 * @throws RepositoryException
	 */
	private void generateDist(Storage store, IRI dataset, String name, String access,
			Element link, String lang) throws MalformedURLException, RepositoryException {
		String href = link.attr(Attribute.HREF.toString());
		String fmt = link.ownText().replaceAll("/", "")
				.replaceAll(" ", "")
				.replaceAll("&nbsp;", "")
				.replaceAll("\u00A0", "");
		if (fmt.isEmpty()) {
			return;
		}
		URL u = makeDistURL(name + "/" + fmt);
		IRI dist = store.getURI(u.toString());
		logger.debug("Generating distribution {}", dist.toString());

		store.add(dataset, DCAT.HAS_DISTRIBUTION, dist);
		store.add(dist, RDF.TYPE, DCAT.DISTRIBUTION);
		store.add(dist, DCTERMS.LANGUAGE, MDR_LANG.MAP.get(lang));
		store.add(dist, DCTERMS.TITLE, fmt, lang);
		store.add(dist, DCAT.ACCESS_URL, makeAbsURL(access));
		store.add(dist, DCAT.DOWNLOAD_URL, makeAbsURL(href));
		store.add(dist, DCAT.MEDIA_TYPE, fmt);
	}

	/**
	 * Generate one dataset
	 *
	 * @param store RDF store
	 * @param page front
	 * @param el HTML element
	 * @param name anchor name
	 * @param lang language
	 * @throws MalformedURLException
	 * @throws RepositoryException
	 */
	private void generateDataset(Storage store, String page, Element el, String anchor,
			String lang) throws MalformedURLException, RepositoryException {
		String title = el.text();
		String name = title.toLowerCase().replaceAll(" ", "")
										.replaceAll("&nbsp;", "")
										.replaceAll("\\u00A0", "");
		// skip empty / invalid title
		if (name.isEmpty()) {
			return;
		}
		URL u = makeDatasetURL(name);
		IRI dataset = store.getURI(u.toString());
		logger.debug("Generating dataset {}", dataset);

		String desc = title;
		Element sib = el.nextElementSibling();
		while (sib != null && 
				(	sib.tagName().equals(Tag.P.toString()) || 
					sib.tagName().equals(Tag.H5.toString()))) {
			if (!sib.select(SIBL_DESC).isEmpty()) {
				desc = sib.text().replaceFirst("Omschrijving ", "");
			}
			sib = sib.nextElementSibling();
		}
		
		store.add(dataset, RDF.TYPE, DCAT.DATASET);
		store.add(dataset, DCTERMS.LANGUAGE, MDR_LANG.MAP.get(lang));
		store.add(dataset, DCTERMS.TITLE, title, lang);
		store.add(dataset, DCTERMS.DESCRIPTION, desc, lang);
		store.add(dataset, DCTERMS.IDENTIFIER, makeHashId(u.toString()));
		store.add(dataset, DCAT.LANDING_PAGE, store.getURI(page + "#" + anchor));
		store.add(dataset, DCAT.KEYWORD, page.substring(page.lastIndexOf("/") + 1), lang);

		Elements links = null;
		sib = el.nextElementSibling();
		while (sib != null && sib.tagName().equals(Tag.P.toString())) {
			if (!sib.select(SIBL_FMTS).isEmpty()) {
				links = sib.select(DIST_HREF);
			}
			sib = sib.nextElementSibling();
		}
		if (links != null) {
			for (Element link : links) {
				generateDist(store, dataset, name, page + "#" + anchor, link, lang);
			}
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

		Page p = page.get("");
		String html = p.getContent();
		Elements elements = Jsoup.parse(html).select(LINK_DATASET);

		for (Element element : elements) {
			String anchor = element.select(NAME_DATASET).attr(Attribute.NAME.toString());
			generateDataset(store, id, element, anchor, lang);
		}
	}

	/**
	 * Generate DCAT.
	 *
	 * @param cache DBC cache
	 * @param store RDF store
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
	 * Constructor
	 *
	 * @param caching DB cache file
	 * @param storage
	 * @param base
	 */
	public HtmlBrugge(File caching, File storage, URL base) {
		super(caching, storage, base);
		setName("brugge");
	}


}
