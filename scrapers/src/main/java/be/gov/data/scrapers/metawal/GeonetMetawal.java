/*
 * Copyright (c) 2024, FPS BOSA DG SD
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
package be.gov.data.scrapers.metawal;

import be.gov.data.helpers.Storage;
import be.gov.data.scrapers.Cache;
import be.gov.data.scrapers.Dcat;
import be.gov.data.scrapers.Page;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Properties;
import java.util.Set;
import org.eclipse.rdf4j.repository.RepositoryException;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.RDFParseException;

/**
 * MetaWal via DCAT-AP catalog.
 *
 * @see https://metawal.wallonie.be/
 * @author Bart Hanssens
 */
public class GeonetMetawal extends Dcat {
	@Override
	public void generateDcat(Cache cache, Storage store) throws RepositoryException, MalformedURLException {
		Set<URL> urls = cache.retrievePageList();
		for (URL url: urls) {
			Page page = cache.retrievePage(url).get("all");
			// fix missing default namespace
			String content = page.getContent();
			content = content.replaceAll("<csw:[^>]+>", "")
							.replaceAll("</csw:[^>]+>", "")
							.replaceAll("(?s)</rdf:RDF>.*<rdf:RDF [^>]+>", "");

			try (InputStream in = new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8))) {
				store.add(in, RDFFormat.RDFXML);
			} catch (RDFParseException | IOException ex) {
				if (ex.getMessage().contains("Premature end")) {
					LOG.warn("Premature end of file in {}", url);
				} else {
					throw new RepositoryException(url.toString(), ex);
				}
			}
		}
		generateCatalog(store);
	}

	/**
	 * Scrape DCAT catalog.
	 *
	 * @param cache
	 * @throws IOException
	 */
	@Override
	protected void scrapeCat(Cache cache) throws IOException {
		int size = 10;

		String prevhash = "";

		for(int start = 1; ;start += size) {
			URL url = new URL(getBase().toString() + "&startPosition=" + start);
			String xml = makeRequest(url);
			
			prevhash = detectLoop(prevhash, xml);
			
			if (!xml.contains("Dataset") && !xml.contains("DataService") && !xml.contains("DataSeries")) {
				LOG.info("Last (empty) page");
				break;
			}
			cache.storePage(url, "all", new Page(url, xml));
		}
	}

	@Override
	public void scrape() throws IOException {
		LOG.info("Start scraping");
		Cache cache = getCache();

		Set<URL> urls = cache.retrievePageList();
		if (urls.isEmpty()) {
			scrapeCat(cache);
		}
		LOG.info("Done scraping");
	}
	
	/**
	 * Constructor.
	 *
	 * @param prop
	 * @throws IOException
	 */
	public GeonetMetawal(Properties prop) throws IOException {
		super(prop);
		setName("metawal");
	}

}
