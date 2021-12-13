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
package be.fedict.dcat.scrapers;


import be.fedict.dcat.helpers.Storage;
import static be.fedict.dcat.scrapers.BasicScraperJson.conf;
import com.jayway.jsonpath.JsonPath;
import com.jayway.jsonpath.ReadContext;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import net.minidev.json.JSONArray;
import net.minidev.json.JSONObject;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.util.Values;
import org.eclipse.rdf4j.model.vocabulary.DCAT;
import org.eclipse.rdf4j.model.vocabulary.DCTERMS;
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
	private final String API_LIST = "/api/search?q=*&type=dataset";
	private final String API_DATASET = "/api/datasets/:persistentId/?persistentId=";

	@Override
	public ReadContext getPaginated(int start, int results) throws IOException {
		String url = getBase().toString() + API_LIST;
		if (start > 0) {
			url += "&start=" + start + "&per_page=" + results;
		}
		logger.info("Get {}", url);

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
					logger.warn("Could not create URL for {}", e);
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
			logger.warn("No results found");
			return 0;
		}
		logger.info("Found {} results", count);
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
			logger.warn("No results counter found");
			return 0;
		}
		logger.info("Found {} results on page", count);
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
		logger.info("Get {}", url);
		try (InputStream ins = url.openStream()) {
			return JsonPath.using(conf).parse(ins);
		}
	}

	/**
	 * Map JSON fields to DCAT properties
	 * 
	 * @param store
	 * @param jsonObj JSON object
	 * @param datasetIdPath
	 * @param datasetMap datasetMap property map
	 * @param distPath path to distribution
	 * @param distMap distribution property map
	 * @param distIdPathAlt
	 * @param distIdPath
	 * @param contactPath
	 * @param contactMap
	 * @throws java.net.MalformedURLException
	 */
	protected void map(Storage store, ReadContext jsonObj, JsonPath datasetIdPath, Map<IRI,Object> datasetMap, 
			JsonPath distPath, JsonPath distIdPath, JsonPath distIdPathAlt, Map<IRI,Object> distMap,
			JsonPath contactPath, Map<IRI,Object> contactMap) throws MalformedURLException {

		IRI datasetSubj =  makeDatasetIRI(jsonObj.read(datasetIdPath).toString());
		store.add(datasetSubj, RDF.TYPE, DCAT.DATASET);
		add(store, datasetSubj, jsonObj, datasetMap);
	
		JSONArray files = jsonObj.read(distPath);

		for(Object f: files) {
			ReadContext node = JsonPath.using(conf).parse(f);
			String idPath = null;
			Object p = jsonObj.read(distIdPath);
			if (p != null) {
				idPath = p.toString();
			} else {
				p = jsonObj.read(distIdPathAlt);
				if (p != null) {
					idPath = p.toString().replace("file://", "");
				}
			}
			if (idPath == null) {
				logger.warn("No dist ID for {}", node.jsonString());
				break;
			}
			IRI distSubj = makeDistIRI(idPath);
			store.add(datasetSubj, DCAT.HAS_DISTRIBUTION, distSubj);
			store.add(distSubj, RDF.TYPE, DCAT.DATASET);
			add(store, distSubj, node, distMap);
		};

		JSONArray obj = jsonObj.read(contactPath);
		ReadContext node = JsonPath.using(conf).parse(obj);
	}

	/**
	 * Generate DCAT from JSON
	 * 
	 * @param store
	 * @param jsonObj JSON object
	 * @param datasetIdPath
	 * @param datasetMap datasetMap property map
	 * @param distPath path to distribution
	 * @param distMap distribution property map
	 * @param distIdPath
	 * @param contactPath
	 * @param contactMap
	 * @throws java.net.MalformedURLException
	 */
	protected void generateDcat(Storage store, ReadContext jsonObj, JsonPath datasetIdPath, 
			Map<IRI,Object> datasetMap, 
			JsonPath distPath, JsonPath distIdPath, Map<IRI,Object> distMap,
			JsonPath contactPath, Map<IRI,Object> contactMap) throws MalformedURLException {

		URL datasetSubj = makeDatasetURL(jsonObj.read(datasetIdPath).toString());
		add(store, Values.iri(datasetSubj.toString()), jsonObj, datasetMap);
	
		JSONArray files = jsonObj.read(distPath);
		
		for(Object f : files) {
			ReadContext node = JsonPath.using(conf).parse(f);
			URL distSubj = makeDistURL(jsonObj.read(distIdPath).toString());
			add(store, Values.iri(distSubj.toString()), node, distMap);
		};

		JSONObject obj = jsonObj.read(contactPath);
		ReadContext node = JsonPath.using(conf).parse(obj);
	}
	
	/**
	 * Map JSON to DCAT(-ish) series of triples
	 * 
	 * @param jsonObj 
	 * @throws java.net.MalformedURLException
	 */
	public void generateDcat(Storage store, ReadContext jsonObj) throws MalformedURLException {
		JsonPath datasetIdPath = JsonPath.compile("$.data.latestVersion.datasetPersistentId");
		
		Map<IRI,Object> datasetMap = Map.of(
			DCTERMS.TITLE, 
				JsonPath.compile("$.data.latestVersion.metadataBlocks.citation.fields[?(@.typeName=='title')].value"),
			DCTERMS.DESCRIPTION, 
				JsonPath.compile("$.data.latestVersion.metadataBlocks.citation.fields[?(@.typeName=='dsDescription')].value[*].dsDescriptionValue.value"),
			DCTERMS.CREATED, JsonPath.compile("$.data.latestVersion.createTime"),
			DCTERMS.MODIFIED, JsonPath.compile("$.data.latestVersion.lastUpdateTime"),
			DCTERMS.LICENSE, JsonPath.compile("$.data.latestVersion.license"),
			DCTERMS.RIGHTS, JsonPath.compile("$.data.latestVersion.termsOfUse"),
			DCAT.THEME, 
				JsonPath.compile("$.data.latestVersion.metadataBlocks.citation.fields[?(@.typeName=='subject')].value[*]"),
			DCAT.KEYWORD, 
				JsonPath.compile("$.data.latestVersion.metadataBlocks.citation.fields[?(@.typeName=='keyword')].value[*].keywordValue.value"),
			DCTERMS.LANGUAGE, 
				JsonPath.compile("$.data.latestVersion.metadataBlocks.citation.fields[?(@.typeName=='language')].value[*]"),
			DCTERMS.SPATIAL, 
				JsonPath.compile("$.data.latestVersion.metadataBlocks.geospatial.fields[?(@.typeName=='geographicCoverage')].value[*].value")
		);

		JsonPath distPath = JsonPath.compile("$.data.latestVersion.files[*]");
		JsonPath distIdPath = JsonPath.compile("$.dataFile.persistentId");
		JsonPath distIdPathAlt = JsonPath.compile("$.dataFile.storageIdentifier");

		Map<IRI,Object> distMap = Map.of(
			DCTERMS.TITLE, JsonPath.compile("$.dataFile.filename"),
			DCTERMS.FILE_FORMAT, JsonPath.compile("$.dataFile.contentType"),
			DCAT.BYTE_SIZE, JsonPath.compile("$.dataFile.filesize"),
			DCTERMS.CREATED, JsonPath.compile("$.data.creationDate")
		);

		JsonPath contactPath = 
			JsonPath.compile("$.data.latestVersion.metadataBlocks.citation.fields[?(@.typeName=='datasetContact')].value[*]");

		Map<IRI,Object> contactMap = Map.of(
			VCARD4.FN, JsonPath.compile("$.datasetContactName.value"),
			VCARD4.ORGANIZATION_NAME, JsonPath.compile("$.datasetContactAffiliation.value")
		);

		map(store, jsonObj, datasetIdPath, datasetMap, distPath, distIdPath, distIdPathAlt, 
			distMap, contactPath, contactMap);
		
		generateCatalog(store);
	}

	@Override
	public void scrape() throws IOException {
		String lang = getDefaultLang();
		Cache cache = getCache();

		List<URL> scrapeList = scrapeList();
		for(URL url: scrapeList) {
			sleep();
			ReadContext ctx = scrapeDataset(url);
			cache.storePage(url, lang, new Page(url, ctx.jsonString()));
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

	/**
	 * Constructor
	 * 
	 * @param prop 
	 */
	protected DataverseJson(Properties prop) throws IOException {
		super(prop);
	}
}
