/*
 * Copyright (c) 2025, FPS BOSA DG DT
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
import static be.gov.data.scrapers.BasicScraperJson.conf;
import static be.gov.data.scrapers.BasicScraperJson.parse;
import com.jayway.jsonpath.JsonPath;
import com.jayway.jsonpath.ReadContext;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Map;
import static java.util.Map.entry;
import java.util.Properties;
import net.minidev.json.JSONArray;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.vocabulary.DCAT;
import org.eclipse.rdf4j.model.vocabulary.DCTERMS;
import org.eclipse.rdf4j.model.vocabulary.FOAF;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.model.vocabulary.VCARD4;


/**
 * Scraper for Datapackage file.
 * 
 * @author Bart Hanssens
 * @see https://datapackage.org/standard/data-package
 */
public abstract class DatapackageJson extends BasicScraperJson  {

	private final static JsonPath DATASET_ID_PATH = JsonPath.compile("$.name");
		
	private final static JsonPath DIST_PATH = JsonPath.compile("$.resources[*]");
	private final static JsonPath DIST_ID_PATH = JsonPath.compile("$.name");

	private final static JsonPath AUTH_PATH = JsonPath.compile("$.author[*]");
	private final static JsonPath AUTH_ID_PATH = JsonPath.compile("$.given-names");

	private final static Map<IRI,Object> DATASET_MAP = Map.ofEntries(
			entry(DCTERMS.IDENTIFIER, 
				JsonPath.compile("$.id")),
			entry(DCTERMS.TITLE,
				JsonPath.compile("$.title")),
			entry(DCTERMS.DESCRIPTION, 
				JsonPath.compile("$.description")),
			entry(DCTERMS.CREATED, 
				JsonPath.compile("$.created")),		
			entry(DCTERMS.MODIFIED, 
				JsonPath.compile("$.modified")),
			entry(DCAT.VERSION, 
				JsonPath.compile("$.version")),
			entry(DCTERMS.LICENSE, 
				JsonPath.compile("$.licenses[0].name")),
			entry(DCAT.KEYWORD, 
				JsonPath.compile("$.keywords")),
			entry(DCTERMS.LANGUAGE, 
				JsonPath.compile("$.languages")),
			entry(DCTERMS.PUBLISHER, 
				JsonPath.compile("$.author[*].name"))
		);

	private final static Map<IRI,Object> AUTH_MAP = Map.of(
			FOAF.GIVEN_NAME, JsonPath.compile("$.given-names"),
			FOAF.FAMILY_NAME, JsonPath.compile("$.family-names"),
			FOAF.MEMBER, JsonPath.compile("$.affiliation")
		);

	private final static Map<IRI,Object> DIST_MAP = Map.of(
			DCTERMS.IDENTIFIER, JsonPath.compile("$.name"),
			DCTERMS.TITLE, JsonPath.compile("$.title"),
			DCTERMS.FORMAT, JsonPath.compile("$.format"),
			DCAT.MEDIA_TYPE, JsonPath.compile("$.mediatype"),
			DCAT.DOWNLOAD_URL, JsonPath.compile("$.path")
		);	

	/**
	 * Add persons (contacts, authors...)
	 * 
	 * @param store
	 * @param jsonObj
	 * @param path
	 * @param pathId
	 * @param subject
	 * @param predicate
	 * @param map 
	 */
	private void addPersons(Storage store, ReadContext jsonObj, JsonPath path, JsonPath pathId, String prefix,
							IRI subject, IRI predicate, Map<IRI,Object> map) {
		JSONArray contacts = jsonObj.read(path);
		for (Object c: contacts) {
			ReadContext node = JsonPath.using(conf).parse(c);
			String idPath = null;
			Object p = node.read(pathId);
			if (p != null && !p.toString().isBlank()) {
				idPath = p.toString();
			}
			if (idPath == null) {
				LOG.warn("No contact name for {}", node.jsonString());
				break;
			}
			IRI contactSubj = makePersonIRI(prefix + hash(idPath));
			store.add(subject, predicate, contactSubj);
			add(store, contactSubj, node, map);
		}
	}
		
	/**
	 * Map JSON fields to DCAT properties
	 * 
	 * @param store
	 * @param jsonObj JSON object
	 * @throws java.net.MalformedURLException
	 */
	protected void mapDataset(Storage store, ReadContext jsonObj) 
			throws MalformedURLException {
		IRI datasetSubj =  makeDatasetIRI(jsonObj.read(DATASET_ID_PATH).toString());
		store.add(datasetSubj, RDF.TYPE, DCAT.DATASET);
		add(store, datasetSubj, jsonObj, DATASET_MAP);
	
	//	addPersons(store, jsonObj, CONTACT_PATH, CONTACT_ID_PATH, "contact/", datasetSubj, DCAT.CONTACT_POINT, CONTACT_MAP);
		addPersons(store, jsonObj, AUTH_PATH, AUTH_ID_PATH, "", datasetSubj, DCTERMS.CREATOR, AUTH_MAP);


		JSONArray files = jsonObj.read(DIST_PATH);
		// download files / distribution
		for(Object f: files) {
			ReadContext node = JsonPath.using(conf).parse(f);
			String idPath = null;
			Object p = node.read(DIST_ID_PATH);
			if (p != null && !p.toString().isBlank()) {
				idPath = p.toString();
			}
			if (idPath == null) {
				LOG.warn("No dist ID for {}", node.jsonString());
				break;
			}
			IRI distSubj = makeDistIRI(idPath);
			store.add(datasetSubj, DCAT.HAS_DISTRIBUTION, distSubj);
			store.add(distSubj, RDF.TYPE, DCAT.DISTRIBUTION);
			add(store, distSubj, node, DIST_MAP);
		}
	}

	@Override
	protected void generateDcat(Cache cache, Storage store) throws IOException {
		Page page = cache.retrievePage(getBase()).get("all");
		ReadContext jsonObj = parse(page.getContent());
		mapDataset(store, jsonObj);		
		generateCatalog(store);		
	}

	@Override
	public void scrape() throws IOException {
		LOG.info("Start scraping");
		Cache cache = getCache();

		URL url = getBase();
		Map<String, Page> front = cache.retrievePage(url);
		if (front.keySet().isEmpty()) {
			try (InputStream ins = url.openStream()) {
				ReadContext ctx = JsonPath.using(conf).parse(ins);
				cache.storePage(url, "all", new Page(url, ctx.jsonString()));
			}
		}
		LOG.info("Done scraping");
	}

	/**
	 * Constructor
	 * 
	 * @param prop 
	 * @throws java.io.IOException 
	 */
	protected DatapackageJson(Properties prop) throws IOException {
		super(prop);
	}
}
