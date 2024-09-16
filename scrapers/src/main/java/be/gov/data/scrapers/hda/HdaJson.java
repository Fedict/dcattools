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
package be.gov.data.scrapers.hda;

import be.gov.data.scrapers.Cache;
import be.gov.data.scrapers.Page;
import be.gov.data.helpers.Storage;
import be.gov.data.scrapers.BasicScraperJson;

import com.jayway.jsonpath.JsonPath;
import com.jayway.jsonpath.ReadContext;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;
import java.util.Map;
import static java.util.Map.entry;
import java.util.Properties;
import java.util.Set;

import net.minidev.json.JSONArray;

import org.apache.http.client.CookieStore;
import org.apache.http.client.config.CookieSpecs;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.BasicCookieStore;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.vocabulary.DCAT;
import org.eclipse.rdf4j.model.vocabulary.DCTERMS;
import org.eclipse.rdf4j.model.vocabulary.OWL;
import org.eclipse.rdf4j.model.vocabulary.RDF;

/**
 * Scraper for Health Data Agency catalog, which is a custom JSON metadata format
 *
 * @see https://catalog.hda.belgium.be/
 * @author Bart Hanssens
 */
public class HdaJson extends BasicScraperJson  {
	private final static String API_LIST = "/openapi/v2/entity/dataset?systemMetadata=false&aspects=datasetProperties&query=%2A";

	private final static String API_AUTH = "/authenticate?redirect_uri=%2F";

	/**
	 * Get scrollId to scroll to next page
	 * 
	 * @param root JSON root
	 * @return number of results or zero
	 */
	private String getScrollId(ReadContext ctx) {
		String scrollId = ctx.read("$.scrollId");
		if (scrollId == null || scrollId.isEmpty()) {
			return null;
		}
		return scrollId;
	}

	/**
	 * Map JSON fields to DCAT properties
	 * 
	 * @param store
	 * @param jsonObj JSON object
	 * @param datasetIdPath
	 * @param datasetMap datasetMap property map
	 * @throws java.net.MalformedURLException
	 */
	protected void mapDataset(Storage store, ReadContext jsonObj, 
			JsonPath datasetIdPath, Map<IRI,Object> datasetMap) throws MalformedURLException {
		String hashID = hash(jsonObj.read(datasetIdPath).toString());
		IRI datasetSubj =  makeDatasetIRI(hashID);

		store.add(datasetSubj, RDF.TYPE, DCAT.DATASET);
		add(store, datasetSubj, jsonObj, datasetMap);
	}

	/**
	 * Map JSON to DCAT(-ish) series of triples
	 * 
	 * @param store
	 * @param jsonObj 
	 * @throws java.net.MalformedURLException
	 */
	private void generateDcat(Storage store, ReadContext jsonObj) throws MalformedURLException {
		JsonPath datasetIdPath = JsonPath.compile("$.urn");

		Map<IRI,Object> datasetMap = Map.ofEntries(
			entry(DCTERMS.IDENTIFIER, 
				JsonPath.compile("$.datasetProperties.value.customProperties.Identifier")),
			entry(DCTERMS.TITLE,
				JsonPath.compile("$.datasetKey.value.name")),
			entry(DCTERMS.DESCRIPTION, 
				List.of(
					JsonPath.compile("$.datasetProperties.value.customProperties.['Description (en)']"),
					JsonPath.compile("$.datasetProperties.value.customProperties.['Description (nl)']"),
					JsonPath.compile("$.datasetProperties.value.customProperties.['Description (fr)']"))
				),
			entry(DCTERMS.ISSUED, 
				JsonPath.compile("$.datasetProperties.value.customProperties.Issued")),
			entry(DCTERMS.MODIFIED, 
				JsonPath.compile("$.datasetProperties.value.customProperties.Modified")),
			entry(DCTERMS.ACCRUAL_PERIODICITY, 
				JsonPath.compile("$.datasetProperties.value.customProperties.['Accrual Periodicity']")),
			entry(DCTERMS.LANGUAGE, 
				JsonPath.compile("$.datasetProperties.value.customProperties.Language")),
			entry(DCTERMS.TEMPORAL, 
				JsonPath.compile("$.datasetProperties.value.customProperties.Temporal")),
			entry(DCTERMS.SPATIAL, 
				JsonPath.compile("$.datasetProperties.value.customProperties.Spatial")),
			entry(DCAT.KEYWORD, 
				JsonPath.compile("$.datasetProperties.value.customProperties.Keyword")),
			entry(DCTERMS.SUBJECT, 
				List.of(
					JsonPath.compile("$.datasetProperties.value.customProperties.Data_Domain"),
					JsonPath.compile("$.datasetProperties.value.customProperties.['Health Category']"))
				),
			entry(DCAT.THEME, 
				JsonPath.compile("$.datasetProperties.value.customProperties.Theme")),
			entry(DCTERMS.ACCESS_RIGHTS, 
				JsonPath.compile("$.datasetProperties.value.customProperties.['Access Rights']")),
			entry(DCTERMS.PUBLISHER, 
				JsonPath.compile("$.datasetProperties.value.customProperties.Publisher")),
			entry(DCTERMS.CREATOR, 
				JsonPath.compile("$.datasetProperties.value.customProperties.Creator")),
			entry(DCAT.ACCESS_URL, 
				JsonPath.compile("$.datasetProperties.value.customProperties.Page")),
			entry(DCAT.LANDING_PAGE, 
				JsonPath.compile("$.datasetProperties.value.customProperties.['Landing Page']")),
			entry(OWL.VERSIONINFO,
				JsonPath.compile("$.datasetProperties.value.customProperties.Version"))
		
		);

		mapDataset(store, jsonObj, datasetIdPath, datasetMap);
		
		generateCatalog(store);
	}

	@Override
	public void scrape() throws IOException {
		String lang = getDefaultLang();
		Cache cache = getCache();

		Set<URL> urls = cache.retrievePageList();
		if (!urls.isEmpty()) {
			return;
		}
	
		CookieStore cookieStore = new BasicCookieStore();
		try (CloseableHttpClient httpclient = HttpClients.custom()
								.setDefaultRequestConfig(RequestConfig.custom()
								.setCookieSpec(CookieSpecs.STANDARD).build())
								.setDefaultCookieStore(cookieStore).build()) {
			/* obtain cookie to avoid HTTP 401 */
			CloseableHttpResponse auth = httpclient.execute(new HttpGet(getBase().toString() + API_AUTH));
			auth.close();
			
			String scrollId = null;
			int count = 50;
			
			do {
				String u = getBase().toString() + API_LIST + "&count=" + count;
				if (scrollId != null) {
					u += "&scrollId=" + scrollId;
				}
				LOG.info("Get {}", u);
				
				HttpGet httpGet = new HttpGet(u);
				try (CloseableHttpResponse response = httpclient.execute(httpGet)) {
					URL url = new URL(u);
					ReadContext ctx = JsonPath.using(conf).parse(response.getEntity().getContent());
					cache.storePage(url, lang, new Page(url, ctx.jsonString()));
					scrollId = getScrollId(ctx);
				}
				sleep();
			} while (scrollId != null);
		}
	}

	@Override
	protected void generateDcat(Cache cache, Storage store) throws IOException {
		JsonPath path = JsonPath.compile("$.entities");

		String lang = getDefaultLang();

		Set<URL> urls = cache.retrievePageList();

		for(URL url: urls) {
			Page page = cache.retrievePage(url).get(lang);
			ReadContext ctx = parse(page.getContent());
			JSONArray entities = ctx.read(path);
			if (entities != null) {
				for (Object obj: entities) {
					generateDcat(store, JsonPath.using(conf).parse(obj));
				}
			}
		}
	}

	/**
	 * Constructor
	 * 
	 * @param prop 
	 */
	public HdaJson(Properties prop) throws IOException {
		super(prop);
		setName("hda");
	}
}
