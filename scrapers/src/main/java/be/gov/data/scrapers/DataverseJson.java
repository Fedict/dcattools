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
package be.gov.data.scrapers;


import be.gov.data.dcat.vocab.ADMS;
import be.gov.data.helpers.Storage;
import static be.gov.data.scrapers.BasicScraperJson.conf;
import com.jayway.jsonpath.JsonPath;
import com.jayway.jsonpath.ReadContext;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import static java.util.Map.entry;
import java.util.Properties;
import java.util.Set;

import net.minidev.json.JSONArray;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.vocabulary.DCAT;
import org.eclipse.rdf4j.model.vocabulary.DCTERMS;
import org.eclipse.rdf4j.model.vocabulary.FOAF;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.model.vocabulary.VCARD4;


/**
 * Scraper for Dataverse portals.While dataverse offers several APIs / metadata export formats (e.g.
 * schema.org or dcterms), the "native" JSON exposes the most metadata fields
 * 
 * @author Bart Hanssens
 * @see https://guides.dataverse.org/en/latest/
 */
public abstract class DataverseJson extends BasicScraperJson implements ScraperPaginated<ReadContext>  {
	private final static String API_LIST = "/api/search?q=*&type=dataset";
	private final static String API_DATASET = "/api/datasets/export?exporter=dataverse_json&persistentId=";
	
	private final static JsonPath DATASET_ID_PATH = JsonPath.compile("$.datasetVersion.datasetPersistentId");

	private final static String CITATION = "$.datasetVersion.metadataBlocks.citation";
	private final static JsonPath START_PATH =
			JsonPath.compile(CITATION + ".fields[?(@.typeName=='timePeriodCovered')].value[0].timePeriodCoveredStart.value");
	private final static JsonPath END_PATH =
			JsonPath.compile(CITATION + ".fields[?(@.typeName=='timePeriodCovered')].value[0].timePeriodCoveredEnd.value");

	private final static JsonPath CONTACT_PATH =
			JsonPath.compile(CITATION + ".fields[?(@.typeName=='datasetContact')].value[*]");
	private final static JsonPath CONTACT_ID_PATH = JsonPath.compile("$.datasetContactName.value");
	
	private final static JsonPath DIST_PATH = JsonPath.compile("$.datasetVersion.files[*]");
	private final static JsonPath DIST_ID_PATH = JsonPath.compile("$.dataFile.persistentId");
	private final static JsonPath DIST_ID_PATH2 = JsonPath.compile("$.dataFile.storageIdentifier");

	private final static JsonPath AUTH_PATH = 
			JsonPath.compile(CITATION + ".fields[?(@.typeName=='author')].value[*]");
	private final static JsonPath AUTH_ID_PATH = JsonPath.compile("$.authorName.value");

	private final static JsonPath CONTRIB_PATH = 
			JsonPath.compile(CITATION + ".fields[?(@.typeName=='contributor')].value[*]");
	private final static JsonPath CONTRIB_ID_PATH = JsonPath.compile("$.contributorName.value");
				
	private final static Map<IRI,Object> DATASET_MAP = Map.ofEntries(
			entry(DCTERMS.IDENTIFIER, 
				JsonPath.compile("$.datasetVersion.datasetPersistentId")),
			entry(DCTERMS.TITLE,
				JsonPath.compile(CITATION + ".fields[?(@.typeName=='title')].value")),
			entry(DCTERMS.DESCRIPTION, 
				JsonPath.compile(CITATION + ".fields[?(@.typeName=='dsDescription')].value[0].dsDescriptionValue.value")),
			entry(DCTERMS.CREATED, 
				JsonPath.compile("$.datasetVersion.createTime")),
//			entry(DCTERMS.CREATOR, 
//				JsonPath.compile("$.data.latestVersion.metadataBlocks.citation.fields[?(@.typeName=='author')].value[*].authorName.value")),
			entry(DCTERMS.ISSUED, 
				JsonPath.compile("$.datasetVersion.releaseTime")),			
			entry(DCTERMS.MODIFIED, 
				JsonPath.compile("$.datasetVersion.lastUpdateTime")),
			entry(DCAT.VERSION, 
				JsonPath.compile("$.datasetVersion.versionNumber")),
			entry(DCTERMS.LICENSE, 
				JsonPath.compile("$.datasetVersion.license.uri")),
			entry(DCTERMS.RIGHTS, 
				JsonPath.compile("$.datasetVersion.termsOfUse")),
			entry(DCTERMS.ACCESS_RIGHTS,
				JsonPath.compile("$.datasetVersion.termsOfAccess")),
			entry(DCAT.THEME, 
				JsonPath.compile(CITATION + ".fields[?(@.typeName=='subject')].value[*]")),
			entry(DCAT.KEYWORD, 
				JsonPath.compile(CITATION + ".fields[?(@.typeName=='keyword')].value[*].keywordValue.value")),
			entry(DCTERMS.SUBJECT,
				JsonPath.compile(CITATION + ".fields[?(@.typeName=='topicClassification')].value[*].topicClassValue.value")),
			entry(DCTERMS.LANGUAGE, 
				JsonPath.compile(CITATION + ".fields[?(@.typeName=='language')].value[*]")),
			entry(DCTERMS.SPATIAL, 
				JsonPath.compile("$.datasetVersion.metadataBlocks.geospatial.fields[?(@.typeName=='geographicCoverage')].value[*].*.value")),
			entry(DCTERMS.PUBLISHER, 
				JsonPath.compile(CITATION + ".fields[?(@.typeName=='datasetContact')].value[*].datasetContactAffiliation.value")),
			entry(DCTERMS.BIBLIOGRAPHIC_CITATION, 
				JsonPath.compile("$.datasetVersion.citation"))
		);

	private final static Map<IRI,Object> AUTH_MAP = Map.of(
			FOAF.NAME, JsonPath.compile("$.authorName.value"),
			FOAF.MEMBER, JsonPath.compile("$.authorAffiliation.value"),
			ADMS.IDENTIFIER, JsonPath.compile("$.authorIdentifier.value"),
			ADMS.SCHEMA_AGENCY, JsonPath.compile("$.authorIdentifierScheme.value")
		);

	private final static Map<IRI,Object> CONTRIB_MAP = Map.of(
			FOAF.NAME, JsonPath.compile("$.contributorName.value"),
			FOAF.MEMBER, JsonPath.compile("$.contributorAffiliation.value"),
			ADMS.IDENTIFIER, JsonPath.compile("$.contributorIdentifier.value"),
			ADMS.SCHEMA_AGENCY, JsonPath.compile("$.contributorIdentifierScheme.value")
		);

	private final static Map<IRI,Object> DIST_MAP = Map.of(
			DCTERMS.IDENTIFIER, JsonPath.compile("$.dataFile.id"),
			DCTERMS.TITLE, JsonPath.compile("$.dataFile.filename"),
			DCTERMS.DESCRIPTION,JsonPath.compile("$.directoryLabel"),
			DCAT.MEDIA_TYPE, JsonPath.compile("$.dataFile.contentType"),
			DCAT.BYTE_SIZE, JsonPath.compile("$.dataFile.filesize"),
			DCTERMS.CREATED, JsonPath.compile("$.dataFile.creationDate")
		);
	
	private final static Map<IRI,Object> CONTACT_MAP = Map.of(
			VCARD4.FN, JsonPath.compile("$.datasetContactName.value"),
			VCARD4.HAS_ORGANIZATION_NAME,JsonPath.compile("$.datasetContactAffiliation.value"),
			VCARD4.HAS_EMAIL , JsonPath.compile("$.datasetContactEmail.value")
		);

	@Override
	public ReadContext getPaginated(int start, int results) throws IOException {
		String url = getBase().toString() + API_LIST;
		if (start > 0) {
			url += "&start=" + start + "&per_page=" + results;
		}
		LOG.info("Get {}", url);

		try (InputStream pis = new URL(url).openStream()) {
			return JsonPath.using(conf).parse(pis);
		}
	}

	/**
	 * Add links to detailed datasets to a list (so they can be fetched one by one)
	 * 
	 * @param list
	 * @param items 
	 */
	private void addItems(List<URL> list, List<String> items) {
		String base = getBase().toString();
		
		if (items != null && !items.isEmpty()) {
			items.forEach(e -> {
				try {
					URL u = new URL(base + API_DATASET + e);
					list.add(u);
				} catch(MalformedURLException mue) {
					LOG.warn("Could not create URL for {}", e);
				}
			});
		}
	}

	/**
	 * Get total number of results, based on the info in the JSON response
	 * 
	 * @param root JSON root
	 * @return number of results or zero
	 */
	private int getResultsTotal(ReadContext ctx) {
		Integer count = ctx.read("$.data.total_count");
		if (count != null && count <= 0) {
			LOG.warn("No results found");
			return 0;
		}
		LOG.info("Found {} results", count);
		return count;
	}

	/**
	 * Get total number of results, based on the info in the JSON response
	 * 
	 * @param root JSON root
	 * @return number of results or zero
	 */
	private int getResultsPage(ReadContext ctx) {
		Integer count = ctx.read("$.data.count_in_response");
		if (count != null && count <= 0) {
			LOG.warn("No results counter found");
			return 0;
		}
		LOG.info("Found {} results on page", count);
		return count;
	}

	/**
	 * 
	 * @return
	 * @throws IOException 
	 */
	protected List<URL> scrapeList() throws IOException {
		List<URL> list = new ArrayList<>();

		for(int pos = 0, total = Integer.MAX_VALUE; pos < total; ) {
			ReadContext ctx = getPaginated(pos, RESULTS_PAGE);
			if (total == Integer.MAX_VALUE) {
				total = getResultsTotal(ctx);
			}
			int cnt = getResultsPage(ctx);
			if (cnt == 0) {
				// break to avoid infinite loop
				break;
			}
			pos += cnt;
			List<String> items = ctx.read("$.data.items[*].global_id");
			addItems(list, items);
		}
		return list;
	}

	/**
	 * 
	 * @param url
	 * @return
	 * @throws IOException 
	 */
	private ReadContext scrapeDataset(URL url) throws IOException {
		LOG.info("Get {}", url);
		try (InputStream ins = url.openStream()) {
			return JsonPath.using(conf).parse(ins);
		}
	}

	/**
	 * Add persons (contacts, authors...)
	 * @param store
	 * @param jsonObj
	 * @param path
	 * @param pathId
	 * @param subject
	 * @param predicate
	 * @param map 
	 */
	private void addPersons(Storage store, ReadContext jsonObj, JsonPath path, JsonPath pathId, 
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
			IRI contactSubj = makePersonIRI(hash(idPath));
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
	
//		addCitation(store, jsonObj, AUTH_PATH, AUTH_ID_PATH, datasetSubj);

		addPersons(store, jsonObj, CONTACT_PATH, CONTACT_ID_PATH, datasetSubj, DCAT.CONTACT_POINT, CONTACT_MAP);
		addPersons(store, jsonObj, AUTH_PATH, AUTH_ID_PATH, datasetSubj, DCTERMS.CREATOR, AUTH_MAP);
		addPersons(store, jsonObj, CONTRIB_PATH, CONTRIB_ID_PATH, datasetSubj, DCTERMS.CONTRIBUTOR, CONTRIB_MAP);

		JSONArray s = jsonObj.read(START_PATH);
		String startDate = (!s.isEmpty()) ? s.get(0).toString() : null;
		JSONArray e = jsonObj.read(END_PATH);
		String endDate = (!e.isEmpty()) ? e.get(0).toString() : null;
		if (startDate != null && endDate != null) {
			generateTemporal(store, datasetSubj, startDate, endDate);
		}

		JSONArray files = jsonObj.read(DIST_PATH);
		// download files / distribution
		for(Object f: files) {
			ReadContext node = JsonPath.using(conf).parse(f);
			String idPath = null;
			Object p = node.read(DIST_ID_PATH);
			if (p != null && !p.toString().isBlank()) {
				idPath = p.toString();
			} else {
				p = node.read(DIST_ID_PATH2);
				if (p != null) {
					idPath = p.toString().replace("file://", "");
				}
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

	/**
	 * Map JSON to DCAT(-ish) series of triples
	 * 
	 * @param store
	 * @param jsonObj 
	 * @throws java.net.MalformedURLException
	 */
	public void generateDcat(Storage store, ReadContext jsonObj) throws MalformedURLException {
		mapDataset(store, jsonObj);		
		generateCatalog(store);
	}

	protected void scrapeCat(Cache cache) throws IOException {
		String lang = getDefaultLang();
		List<URL> scrapeList = scrapeList();
		for(URL url: scrapeList) {
			sleep();
			try {
				ReadContext ctx = scrapeDataset(url);
				cache.storePage(url, lang, new Page(url, ctx.jsonString()));
			} catch (IOException ioe) {
				LOG.error("Could not scrape {}: {}", url, ioe.getMessage());
			}
		}
	}

	@Override
	protected void generateDcat(Cache cache, Storage store) throws IOException {
		String lang = getDefaultLang();
		Set<URL> urls = cache.retrievePageList();
		for(URL url: urls) {
			Page page = cache.retrievePage(url).get(lang);
			generateDcat(store, parse(page.getContent()));
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
	protected DataverseJson(Properties prop) throws IOException {
		super(prop);
	}
}
