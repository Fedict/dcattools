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

import be.fedict.dcat.helpers.Page;
import be.fedict.dcat.helpers.Storage;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import javax.swing.text.html.HTML;

import org.eclipse.rdf4j.repository.RepositoryException;
import org.eclipse.rdf4j.rio.RDFFormat;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

/**
 * Scraper for (part of the) HealthData portal
 *
 * @author Bart Hanssens
 */
public class HtmlFAIR extends Html {
	private final String SOURCES = "/sources/covid19?items_per_page=40";
	private final String PATH_PAGE = "h2.node-title a";

	@Override
	protected void generateDataset(Storage store, String id, Map<String, Page> page) 
				throws MalformedURLException, RepositoryException {
		if (page == null || page.isEmpty()) {
			logger.error("Page not found " + id);
			return;
		}
		String url = page.keySet().iterator().next();
		Page p = page.get(url);
		String content = p.getContent();
		try (InputStream is = new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8))) {
			store.add(is, RDFFormat.RDFXML);
		} catch (IOException ex) {
			logger.error(ex.getMessage());
		}
	}

	@Override
	protected List<URL> scrapeDatasetList() throws IOException {
		List<URL> urls = new ArrayList<>();
		
		String front = makeRequest(new URL(getBase().toString() + SOURCES));
		Document doc = Jsoup.parse(front);
		Elements links = doc.select(PATH_PAGE);
		for (Element link: links) {
			String href = link.attr(HTML.Attribute.HREF.toString());
			if (href != null) {
				urls.add(makeAbsURL(href + ".xml"));
			}
		}

		return urls;
	}

	/**
	 * Constructor
	 * 
	 * @param caching
	 * @param storage
	 * @param base 
	 */
	public HtmlFAIR(File caching, File storage, URL base) {
		super(caching, storage, base);
		setName("fair");
	}
}
