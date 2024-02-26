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
package be.gov.data.scrapers.vlaanderen;

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
 * Vlaanderen via DCAT-AP catalog.
 *
 * @see https://opendata.vlaanderen.be/
 * @author Bart Hanssens
 */
public class GeonetVlaanderen extends Dcat {
	@Override
	public void generateDcat(Cache cache, Storage store) throws RepositoryException, MalformedURLException {
		Set<URL> urls = cache.retrievePageList();
		for (URL url: urls) {
			Page page = cache.retrievePage(url).get("all");
			// fix buggy input
			try (InputStream in = new ByteArrayInputStream(page.getContent().getBytes(StandardCharsets.UTF_8))) {
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
		int size = 20;

		for(int start = 0; ;start += size) {
			URL url = new URL(getBase().toString() + "?startindex=" + start + "&limit=" + size + "&f=dcat_ap_vl");
			String xml = makeRequest(url);
			if (!xml.contains("Dataset") && !xml.contains("DataService")) {
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
	public GeonetVlaanderen(Properties prop) throws IOException {
		super(prop);
		setName("vlaanderen");
	}

}
