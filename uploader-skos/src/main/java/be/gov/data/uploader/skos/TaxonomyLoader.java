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
package be.gov.data.uploader.skos;

import jakarta.json.Json;
import jakarta.json.JsonObject;
import jakarta.json.JsonValue;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Base64;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
//import javax.json.Json;
//import javax.json.JsonObject;
import org.apache.http.HttpHeaders;
import org.apache.http.HttpResponse;
import org.apache.http.client.fluent.Request;
import org.apache.http.entity.ContentType;
import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.model.vocabulary.SKOS;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.Rio;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Bart.Hanssens
 */
public class TaxonomyLoader {
	private final static Logger LOG = LoggerFactory.getLogger(TaxonomyLoader.class);
	
	/**
	 * Build the (mandatory) name, depending on whether or not the thesaurus is language-agnostic (or not)
	 * 
	 * @param term term to get the name(s) from
	 * @return string (for language-agnostic) or object with an array per language
	 */
	private JsonValue buildName(Term term) {
		Map<String, String> values = term.values();
		
		if (values.size() == 1 && values.containsKey("und")) {
			return Json.createValue(term.values().get("und"));
		}

		return Json.createArrayBuilder(values.entrySet().stream()
			.map(e -> Json.createObjectBuilder()
				.add("langcode", e.getKey())
				.add("value", e.getValue()).build()).collect(Collectors.toSet())).build();
	}
	
	/**
	 * Build a taxonomy term JSON object for Drupal, which may or may not be translated
	 * 
	 * @param taxonomy Drupal internal name of the taxonomy
	 * @param term term
	 * @return JSON object for Drupal
	 */
	private JsonObject buildTerm(String taxonomy, Term term) {
		return Json.createObjectBuilder()
			.add("data", Json.createObjectBuilder()
					.add("type", "taxonomy_term--" + taxonomy)
					.add("attributes", Json.createObjectBuilder()
						.add("field_uri", Json.createObjectBuilder()
							.add("uri", term.subject().toString())
							.add("title", ""))
						.add("name", buildName(term)
					)
			)).build();
	}
	
	/**
	 * Post a term to a Drupal 9 website
	 * 
	 * @param website domain name
	 * @param taxonomy internal Drupal nam of the taxonomy
	 * @param user user name
	 * @param pass password
	 * @param term taxonomy term
	 * @throws IOException 
	 */
	public void postTerm(String website, String taxonomy, String user, String pass, Term term) throws IOException {
		JsonObject obj = buildTerm(taxonomy, term);
		LOG.info(obj.toString());
				
		String auth = Base64.getEncoder().encodeToString((user + ":" + pass).getBytes());
		Request req = Request.Post(website + "/en/jsonapi/taxonomy_term/" + taxonomy)
			.setHeader(HttpHeaders.AUTHORIZATION, "Basic " + auth)
			.bodyString(obj.toString(), ContentType.create("application/vnd.api+json"));

		HttpResponse resp = req.execute().returnResponse();
		LOG.info(resp.toString());
	}

	/**
	 * Load taxonomy from SKOS file
	 * 
	 * @param file SKOS file in Turtle format
	 * @return RDF model
	 * @throws IOException 
	 */
	private static Model loadSkos(File file) throws IOException {
		try(FileInputStream fis = new FileInputStream(file)) {
			return Rio.parse(fis, RDFFormat.TURTLE, (Resource) null);
		}
	}

	/**
	 * Parse the SKOS file into a set of taxonomy terms
	 * 
	 * @param file SKOS file
	 * @return set of terms
	 * @throws IOException 
	 */
	public Set<Term> parse(File file) throws IOException {
		LOG.info("Parsing file {}", file.toString());
		Model m = loadSkos(file);
		
		Set<Resource> uris = m.filter(null, SKOS.HAS_TOP_CONCEPT, null).subjects();
		LOG.info("Found {} root terms", uris.size());
		
		if (uris.isEmpty()) {
			uris = m.filter(null, RDF.TYPE, SKOS.CONCEPT).subjects();
			LOG.info("Assuming flat list, found {} terms", uris.size());
		}
		
		return uris.stream().map(u -> {
			Set<Value> values = m.filter(u, SKOS.PREF_LABEL, null).objects();
			Map<String, String> prefs = values.stream().map(Literal.class::cast)
				.collect(Collectors.toMap(l -> l.getLanguage().orElse("und"), l -> l.getLabel()));
			return new Term(u, prefs, null);
		}).collect(Collectors.toSet());
	}

	
	/**
	 * Constructor
	 * 
	 */
	public TaxonomyLoader() {
	}
}
