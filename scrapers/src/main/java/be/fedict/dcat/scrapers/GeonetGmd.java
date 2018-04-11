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

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;
import java.util.Map;
import org.eclipse.rdf4j.model.IRI;

import org.eclipse.rdf4j.repository.RepositoryException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xmlbeam.XBProjector;
import org.xmlbeam.annotation.XBRead;

/**
 * Abstract scraper for the GeonetRDF v3 portal software with DCAT export.
 *
 * @see http://geonetwork-opensource.org/
 *
 * @author Bart Hanssens <bart.hanssens@fedict.be>
 */
public abstract class GeonetGmd extends Geonet {

	private final Logger logger = LoggerFactory.getLogger(GeonetGmd.class);
	public final static String GMD = "http://www.isotc211.org/2005/gmd";
	public final static String API = "/eng/csw?service=CSW&version=2.0.2";
	public final static String API_RECORDS = API
			+ "&request=GetRecords&resultType=results"
			+ "&outputSchema=" + GMD
			+ "&elementSetName=full&typeNames=gmd:MD_Metadata"
			+ "&maxRecords=150";
	public final static String OFFSET = "startPosition";


	protected interface GmdContact {
		@XBRead("/gmd:organisationName/gco:CharacterString")
		public String getName();

		@XBRead("/gmd:contactInfo/gmd:CI_Contact/gmd:address/gmd:CI_Address/gmd:electronicMailAddress/gco:CharacterString")		
		public String getEmail();
		
	}
	
	protected interface GmdMetadata {
		@XBRead("/gmd:fileIdentifier/gco:CharacterString")
		public String getID();
		
		@XBRead("/gmd:contact/gmd:CI_ResponsibleParty")
		public GmdContact getContact();
		
		@XBRead("/gmd:dateStamp/gco:DateTime")
		public String getData();
	}
	
	protected interface GmdRoot {
		@XBRead("//gmd:MD_Metadata")
		public List<GmdMetadata> getEntries();
	}
	
	/**
	 * Generate DCAT dataset
	 * 
	 * @param store 
	 * @param id 
	 * @param meta 
	 * @throws java.net.MalformedURLException 
	 */
	protected void generateDataset(Storage store, String id, GmdMetadata meta) 
			throws MalformedURLException {
		IRI dataset = store.getURI(makeDatasetURL(id).toString());
		logger.info("Generating dataset {}", dataset.toString());
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
	public void generateDcat(Cache cache, Storage store)
			throws RepositoryException, MalformedURLException {
		Map<String, Page> map = cache.retrievePage(getBase());
		String xml = map.get("all").getContent();
System.err.println(xml);
		try {
			GmdRoot m = new XBProjector().projectXMLString(xml, GmdRoot.class);
			for (GmdMetadata e: m.getEntries()) {
				generateDataset(store, e.getID(), e);
			}
		} catch (IOException ex) {
			logger.error("Error projecting XML");
			throw new RepositoryException(ex);
		}
	
		generateCatalog(store);
	}

	/**
	 * Scrape DCAT catalog.
	 *
	 * @param cache
	 * @throws IOException
	 */
	protected void scrapeCat(Cache cache) throws IOException {
		URL front = getBase();
		URL url = new URL(getBase() + GeonetGmd.API_RECORDS);
		System.err.println(url);
		String content = makeRequest(url);
		cache.storePage(front, "all", new Page(url, content));
	}

	/**
	 * Scrape DCAT catalog.
	 *
	 * @throws IOException
	 */
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
	 * @param caching DB cache file
	 * @param storage SDB file to be used as triple store backend
	 * @param base base URL
	 */
	public GeonetGmd(File caching, File storage, URL base) {
		super(caching, storage, base);
	}
}
