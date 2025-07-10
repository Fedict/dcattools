/*
 * Copyright (c) 2015, FPS BOSA DG DT
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
package be.gov.data.scrapers;


import be.gov.data.helpers.Storage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.stream.Collectors;
import org.eclipse.rdf4j.repository.RepositoryException;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.RDFParseException;

/**
 * Abstract OpenDataSoft scraper.
 *
 * @see https://www.opendatasoft.com/
 *
 * @author Bart Hanssens
 */
public abstract class Ods extends Dcat {
	
	public final static String API_DCAT = "/api/explore/v2.1/catalog/exports/dcat?lang=";
	
	/**
	 * Method to allow filtering (if needed)
	 * 
	 * @param str
	 * @return 
	 */
	protected String filter(String str) {
		return str;
	}

	/**
	 * Generate DCAT file
	 *
	 * @param cache
	 * @param store
	 * @throws RepositoryException
	 * @throws MalformedURLException
	 */
	@Override
	public void generateDcat(Cache cache, Storage store) throws RepositoryException, MalformedURLException {
		Map<String, Page> map = cache.retrievePage(getBase());

		for (String lang: super.getAllLangs()) {
			Page p = map.get(lang);
			if (p == null) {
				throw new RepositoryException("No page found for " + getBase());
			}
			String xml = p.getContent();
			// correction per language, required for ODS up to 2.1, since the language attribute by ODS is wrong
			xml = xml.replaceAll("xml:lang=\"[a-z]{2}\"", "xml:lang=\"" + lang + "\"");
			// Load RDF file into store
			try (InputStream in = new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8))) {
				store.add(in, RDFFormat.RDFXML);		
			} catch (RDFParseException | IOException ex) {
				throw new RepositoryException(ex);
			}
		}
	}
	
	/**
	 * Scrape DCAT catalog.
	 *
	 * @param cache
	 * @throws IOException
	 */
	@Override
	protected void scrapeCat(Cache cache) throws IOException {
		URL front = getBase();
		for (String lang: super.getAllLangs()) {
			URL url = new URL(getBase(), Ods.API_DCAT + lang);
			String content = makeRequest(url);
			LOG.info("Storing {} for {}", front, lang);
			cache.storePage(front, lang, new Page(url, content));
			sleep();
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
	 * Constructor
	 *
	 * @param prop
	 */
	protected Ods(Properties prop) throws IOException {
		super(prop);
	}
}
