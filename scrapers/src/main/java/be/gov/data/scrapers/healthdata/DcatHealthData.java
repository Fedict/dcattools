/*
 * Copyright (c) 2021, FPS BOSA DG DT
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
package be.gov.data.scrapers.healthdata;

import be.gov.data.scrapers.Cache;
import be.gov.data.scrapers.Page;
import be.gov.data.helpers.Storage;
import be.gov.data.scrapers.Dcat;

import com.github.jsonldjava.utils.JsonUtils;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.eclipse.rdf4j.model.vocabulary.DCAT;
import org.eclipse.rdf4j.model.vocabulary.DCTERMS;
import org.eclipse.rdf4j.model.vocabulary.FOAF;
import org.eclipse.rdf4j.model.vocabulary.ORG;
import org.eclipse.rdf4j.model.vocabulary.RDFS;
import org.eclipse.rdf4j.model.vocabulary.VCARD4;

import org.eclipse.rdf4j.repository.RepositoryException;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.RDFParseException;

/**
 * Scraper for FAIR HealthData portal, the format returned by the API is almost JSON-LD
 *
 * @see https://fair.healthdata.be/
 * @author Bart Hanssens
 */
public class DcatHealthData extends Dcat {

	private final static String KEYWORDS = "/api/1/metastore/schemas/keyword/items";
			
	private final static Map<String,String> CONTEXT = new HashMap<>();
	
	static {
		CONTEXT.put("dcat", DCAT.NAMESPACE);
		CONTEXT.put("org", ORG.NAMESPACE);
		CONTEXT.put("vcard", VCARD4.NAMESPACE);	
		CONTEXT.put("title", DCTERMS.TITLE.toString());
		CONTEXT.put("identifier", DCTERMS.IDENTIFIER.toString());
		CONTEXT.put("description", DCTERMS.DESCRIPTION.toString());
		CONTEXT.put("accessLevel", DCTERMS.ACCESS_RIGHTS.toString());
		CONTEXT.put("issued", DCTERMS.ISSUED.toString());
		CONTEXT.put("accrualPeriodicity", DCTERMS.ACCRUAL_PERIODICITY.toString());
		CONTEXT.put("modified", DCTERMS.MODIFIED.toString());
		CONTEXT.put("license", DCTERMS.LICENSE.toString());
		CONTEXT.put("spatial", DCTERMS.SPATIAL.toString());
		CONTEXT.put("temporal", DCTERMS.TEMPORAL.toString());
		CONTEXT.put("publisher", DCTERMS.PUBLISHER.toString());
		CONTEXT.put("fn", VCARD4.FN.toString());
		CONTEXT.put("hasEmail", VCARD4.HAS_EMAIL.toString());		
		CONTEXT.put("name", FOAF.NAME.toString());
		CONTEXT.put("isPartOf", DCTERMS.IS_PART_OF.toString());
		CONTEXT.put("contactPoint", DCAT.CONTACT_POINT.toString());
		CONTEXT.put("distribution", DCAT.HAS_DISTRIBUTION.toString());
		CONTEXT.put("format", DCTERMS.FORMAT.toString());
		CONTEXT.put("mediaType", DCAT.MEDIA_TYPE.toString());
		CONTEXT.put("downloadURL", DCAT.DOWNLOAD_URL.toString());
		CONTEXT.put("keyword", DCAT.KEYWORD.toString());
		CONTEXT.put("label", DCAT.KEYWORD.toString());
		CONTEXT.put("theme", DCAT.THEME.toString());
		CONTEXT.put("references", DCTERMS.REFERENCES.toString());
		CONTEXT.put("data", RDFS.LABEL.toString());
	}

	@Override
	public void generateDcat(Cache cache, Storage store) throws RepositoryException, MalformedURLException {
		Map<String, Page> map = cache.retrievePage(getBase());
		String json = map.get("all").getContent();

		List<Map> datasets;
		// Change JSON file to JSON-LD
		try (InputStream in = new ByteArrayInputStream(json.getBytes(StandardCharsets.UTF_8))) {
			datasets = (List<Map>) JsonUtils.fromInputStream(in);
		} catch (IOException ex) {
			throw new RepositoryException(ex);
		}

		Map<String,Object> jsonld = new HashMap<>();
		jsonld.put("@context", CONTEXT);
		jsonld.put("@graph", datasets);
		
		String str = "";
		try {
			str = JsonUtils.toString(jsonld);
		} catch (IOException ex) {
			throw new RepositoryException(ex);
		}
		
		try (InputStream jsonis = new ByteArrayInputStream(str.getBytes(StandardCharsets.UTF_8))) {
			store.add(jsonis, RDFFormat.JSONLD);
		} catch (RDFParseException | IOException ex) {
			throw new RepositoryException(ex);
		}
		generateCatalog(store);
	}
	
	@Override
	public void scrape() throws IOException {
		LOG.info("Start scraping");
		Cache cache = getCache();
	
		Map<String, Page> front = cache.retrievePage(getBase());
		if (front.keySet().isEmpty()) {
			scrapeCat(cache);
		}
		LOG.info("Done scraping");
	}
	
	/**
	 * Constructor
	 * 
	 * @param prop
	 * @throws IOException 
	 */
	public DcatHealthData(Properties prop) throws IOException {
		super(prop);
		setName("healthdata");
	}
}
