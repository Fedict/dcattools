/*
 * Copyright (c) 2019, FPS BOSA DG DT
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

import be.fedict.dcat.helpers.Storage;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import org.eclipse.rdf4j.model.IRI;

import org.eclipse.rdf4j.repository.RepositoryException;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.RDFParseException;
import org.eclipse.rdf4j.rio.Rio;

/**
 * Generic DCAT
 *
 * @author Bart Hanssens
 */
public abstract class Dcat extends BaseScraper {

	@Override
	public void generateCatalogInfo(Storage store, IRI catalog) throws RepositoryException {
		// don't add extra metadata since the catalog itself needs to include this info
	}
	
	/**
	 * Scrape DCAT catalog.
	 *
	 * @param cache
	 * @throws IOException
	 */
	protected void scrapeCat(Cache cache) throws IOException {
		URL url = getBase();
		String content = makeRequest(url);
		cache.storePage(url, "all", new Page(url, content));
	}

	/**
	 * Guess serialization based on either the file name or the content
	 * 
	 * @param name filename
	 * @param data content
	 * @return file format
	 */
	private RDFFormat guessFormat(String name, String data) {
		if (data.startsWith("<?xml") || data.startsWith("<rdf")) {
			return RDFFormat.RDFXML;
		}
		if (data.startsWith("@base") || data.startsWith("@prefix")) {
			return RDFFormat.TURTLE;
		}
		Optional<RDFFormat> fmt = Rio.getParserFormatForFileName(name);
		return fmt.orElse(RDFFormat.NTRIPLES);
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
		String data = map.get("all").getContent();

		// Load RDF file into store
		try (InputStream in = new ByteArrayInputStream(data.getBytes(StandardCharsets.UTF_8))) {
			RDFFormat fmt = guessFormat(getBase().toString(), data);
			logger.info("Guessing format is {}", fmt.getName());
			store.add(in, fmt);
		} catch (RDFParseException | IOException ex) {
			throw new RepositoryException(ex);
		}
		generateCatalog(store);
	}

	@Override
	public void scrape() throws IOException {
		logger.info("Start scraping");
		Cache cache = getCache();

		Map<String, Page> front = cache.retrievePage(getBase());
		if (front.keySet().isEmpty()) {
			scrapeCat(cache);
		}
		logger.info("Done scraping");
	}

	/**
	 * Constructor
	 *
	 * @param prop
	 * @throws IOException
	 */
	protected Dcat(Properties prop) throws IOException {
		super(prop);
	}
}
