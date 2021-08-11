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

import be.fedict.dcat.helpers.Cache;
import be.fedict.dcat.helpers.Page;
import be.fedict.dcat.helpers.Storage;

import com.github.jsonldjava.utils.JsonUtils;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.eclipse.rdf4j.repository.RepositoryException;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.RDFParseException;
/**
 * Scraper for FAIR HealthData portal
 * The format returned by the API is almost JSON-LD
 *
 * @author Bart Hanssens
 */
public class DcatFAIR extends Dcat {
	private final static Map<String,String> CONTEXT = new HashMap<>();
	
	static {
		CONTEXT.put("title", "http://purl.org/dc/terms/title");
		CONTEXT.put("identifier", "http://purl.org/dc/terms/identifier");
		CONTEXT.put("dcat:Dataset", "http://www.w3.org/ns/dcat#Dataset");
		CONTEXT.put("dcat:Distribution", "http://www.w3.org/ns/dcat#Distribution");
		CONTEXT.put("vcard:Contact", "http://www.w3.org/2006/vcard/ns#Contact");
		CONTEXT.put("description", "http://purl.org/dc/terms/description");
		CONTEXT.put("accessLevel", "http://purl.org/dc/terms/accessRights");
		CONTEXT.put("issued", "http://purl.org/dc/terms/issued");
		CONTEXT.put("accrualPeriodicity", "http://purl.org/dc/terms/accrualPeriodicity");
		CONTEXT.put("modified", "http://purl.org/dc/terms/modified");
		CONTEXT.put("license", "http://purl.org/dc/terms/license");
		CONTEXT.put("spatial", "http://purl.org/dc/terms/spatial");
		CONTEXT.put("publisher", "http://purl.org/dc/terms/publisher");
		CONTEXT.put("fn", "http://www.w3.org/2006/vcard/ns#fn");
		CONTEXT.put("isPartOf", "http://purl.org/dc/terms/isPartOf");
		CONTEXT.put("contactPoint", "http://www.w3.org/ns/dcat#contactPoint");
		CONTEXT.put("distribution", "http://www.w3.org/ns/dcat#distribution");
		CONTEXT.put("format", "http://purl.org/dc/terms/format");
		CONTEXT.put("mediaType", "http://www.w3.org/ns/dcat#mediaType");
		CONTEXT.put("downloadURL", "http://www.w3.org/ns/dcat#downloadURL");
		CONTEXT.put("keyword", "http://www.w3.org/ns/dcat#keyword");
		CONTEXT.put("theme", "http://www.w3.org/ns/dcat#theme");
		CONTEXT.put("references", "http://purl.org/dc/terms/references");
	}
	private final static String DISTS = "distribution";

	@Override
	public void generateDcat(Cache cache, Storage store) throws RepositoryException, MalformedURLException {
		Map<String, Page> map = cache.retrievePage(getBase());
		String ttl = map.get("all").getContent();

		List<Map> datasets;
		// Change JSON file to JSON-LD
		try (InputStream in = new ByteArrayInputStream(ttl.getBytes(StandardCharsets.UTF_8))) {
			datasets = (List<Map>) JsonUtils.fromInputStream(in);
		} catch (IOException ex) {
			throw new RepositoryException(ex);
		}
		
		for(Map dataset: datasets) {
			dataset.remove("spatial");
			List<Map> dists = (List<Map>) dataset.get(DISTS);
			if (dists != null) {
				for(Map dist: dists) {
					dist.remove("%Ref:downloadURL");
				}
			}
		}
		Map<String,Object> jsonld = new HashMap();
		jsonld.put("@context", CONTEXT);
		jsonld.put("@graph", datasets);
		
		String str = "";
		try {
			str = JsonUtils.toString(jsonld);
		} catch (IOException ex) {
			throw new RepositoryException(ex);
		}
		
		try (InputStream json = new ByteArrayInputStream(str.getBytes(StandardCharsets.UTF_8))) {
			store.add(json, RDFFormat.JSONLD);
		} catch (RDFParseException | IOException ex) {
			throw new RepositoryException(ex);
		}
		generateCatalog(store);
	}
	
	/**
	 * Constructor
	 * 
	 * @param caching
	 * @param storage
	 * @param base 
	 */
	public DcatFAIR(File caching, File storage, URL base) {
		super(caching, storage, base);
		setName("fair");
	}
}
