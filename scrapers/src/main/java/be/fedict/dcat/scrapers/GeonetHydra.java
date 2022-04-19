/*
 * Copyright (c) 2022, FPS BOSA DG DT
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

import java.io.IOException;
import java.io.StringReader;
import java.net.URL;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.DocumentFactory;
import org.dom4j.Node;
import org.dom4j.io.SAXReader;

/**
 * Geonetwork hydra paged scraper
 * 
 * @author Bart.Hanssens
 */
public abstract class GeonetHydra extends Geonet {
	private final static String HYDRA_NS = "http://www.w3.org/ns/hydra/core#";
	private final static String HYDRA_NEXT = "hydra:nextPage";

	private final SAXReader sax;
	
	/**
	 * Scrape DCAT catalog.
	 *
	 * @param cache
	 * @throws IOException
	 */
	protected void scrapeCat(Cache cache) throws IOException {
		URL url = getBase();
		while (url != null) {
			String xml = makeRequest(url);
			try {
				Document doc = sax.read(new StringReader(xml));
				cache.storePage(url, "all", new Page(url, xml));
				Node next = doc.selectSingleNode("//" + HYDRA_NEXT);
				url = (next != null) ? new URL(next.getStringValue()) : null;
			} catch (DocumentException ex) {
				logger.error("Error parsing XML " + url);
			}
		}			
	}

	@Override
	public void scrape() throws IOException {
		logger.info("Start scraping");
		Cache cache = getCache();

		Set<URL> urls = cache.retrievePageList();
		if (urls.isEmpty()) {
			scrapeCat(cache);
		}
		logger.info("Done scraping");
	}

	/**
	 * Constructor
	 *
	 * @param prop
	 * @throws java.io.IOException
	 */
	protected GeonetHydra(Properties prop) throws IOException {
		super(prop);
		
		DocumentFactory factory = DocumentFactory.getInstance();
		factory.setXPathNamespaceURIs(Map.of("hydra", HYDRA_NS));
		sax = new SAXReader();
		sax.setDocumentFactory(factory);
	}
}
