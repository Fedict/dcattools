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
package be.gov.data.drupal;

import be.gov.data.drupal.dao.Term;
import jakarta.json.Json;
import jakarta.json.JsonArray;
import jakarta.json.JsonObject;
import jakarta.json.JsonObjectBuilder;
import jakarta.json.JsonReader;
import jakarta.json.JsonString;
import jakarta.json.JsonValue;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.apache.http.HttpHeaders;
import org.apache.http.HttpResponse;
import org.apache.http.client.fluent.Request;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.util.Values;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.model.vocabulary.SKOS;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.Rio;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Load and retrieve taxonomy terms to/from Drupal 9
 * 
 * @author Bart Hanssens
 */
public class TaxonomyLoader extends AbstractLoader {
	private final static Logger LOG = LoggerFactory.getLogger(TaxonomyLoader.class);
	
	/**
	 * Build the (mandatory) name, depending on whether or not the thesaurus is language-agnostic (or not)
	 * 
	 * @param term term to get the name(s) from
	 * @return JSON string (for language-agnostic) or JSON object with an array per language
	 */
	private JsonValue buildName(Term term) {
		Map<String, String> values = term.values();
		
		if (values.size() == 1 && values.containsKey("und")) {
			return Json.createValue(term.values().get("und"));
		}

		return Json.createValue(term.values().get("en"));
		/*
		return Json.createArrayBuilder(values.entrySet().stream()
			.map(e -> Json.createObjectBuilder()
				.add("value", e.getValue())
				.add("langcode", e.getKey())
			.build())
			.collect(Collectors.toSet()))
			.build(); */
	}

	/**
	 * Build the (mandatory) name, depending on whether or not the thesaurus is language-agnostic (or not)
	 * 
	 * @param json json value to get the name(s) from
	 * @return map of the names, using "und" for undefined / language-agnostic 
	 */
	private Map<String,String> buildName(JsonValue json) {
		if (json instanceof JsonString js) {
			return Map.of("und", js.getString());
		}
		if (json instanceof JsonArray arr) {
			return arr.stream()
				.map(JsonObject.class::cast)
				.collect(Collectors.toMap(o -> o.getString("langcode"), o -> o.getString("value")));
		}
		return Collections.emptyMap();
	}

	/**
	 * Get UUID of parent (if any) and build relationship object
	 * 
	 * @param taxonomy Drupal internal name of the taxonomy
	 * @param term term
	 * @return (potentially empty) relationship object
	 */
	private JsonObjectBuilder buildParent(String taxonomy, Term term) {
		if (term.parent() == null) {
			return Json.createObjectBuilder();
		}

		String uuid = null;
		try {
			Term parent = getTerm(taxonomy, term.parent());
			uuid = parent.drupalID().toString();
		} catch (IOException ioe) {
			LOG.error("Parent not found", ioe);
		}

		return Json.createObjectBuilder()
					.add("relationships", Json.createObjectBuilder()
						.add("parent", Json.createObjectBuilder()
							.add("data", Json.createArrayBuilder()
								.add(Json.createObjectBuilder()
									.add("type", "taxonomy_term--" + taxonomy)
									.add("id", uuid)
								)
							)
						)
					);
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
						.add("name", buildName(term))
					)
					.addAll(buildParent(taxonomy, term))
			).build();
	}
	
	/**
	 * Post a term to a Drupal 9 website
	 * 
	 * @param taxonomy internal Drupal nam of the taxonomy
	 * @param term taxonomy term
	 * @return true if the term has been created
	 * @throws IOException 
	 */
	public boolean postTerm(String taxonomy, Term term) throws IOException {
		JsonObject obj = buildTerm(taxonomy, term);
		HttpResponse resp = postRequest("/en/jsonapi/taxonomy_term/" + taxonomy, obj);
	
		return resp.getStatusLine().getStatusCode() < 400;
	}

	/**
	 * Parse JSON object into a taxonomy term
	 * 
	 * @param obj json object
	 * @return 
	 */
	private Term parseTerm(JsonObject obj) {
		UUID uuid = UUID.fromString(obj.getString("id"));
				
		JsonObject attributes = obj.getJsonObject("attributes");
		String uri = attributes.getJsonObject("field_uri").getJsonString("uri").getString();
		IRI iri = Values.iri(uri);
		Map<String,String> names = buildName(attributes.get("name"));
		
		return new Term(iri, names, null, uuid);
	}

	/**
	 * Get a term with a specific IRI from a Drupal 9 website 
	 * 
	 * @param taxonomy
	 * @param iri 
	 * @return taxonomy term or null
	 * @throws IOException 
	 */
	public Term getTerm(String taxonomy, IRI iri) throws IOException {
		Request req = Request.Get(getWebsite() + "/en/jsonapi/taxonomy_term/" + taxonomy 
										+ "?filter[field_uri.uri]=" + iri.stringValue());
		
		HttpResponse resp = req.execute().returnResponse();
		LOG.info(resp.toString());

		if (resp.getStatusLine().getStatusCode() >= 400) {
			LOG.error("Error in HTTP status");
			return null;
		}


		try(InputStream is = resp.getEntity().getContent();
			JsonReader reader = Json.createReader(is)) {

			if (reader == null) {
				LOG.error("Could not parse JSON");
				return null;
			}
		
			JsonArray arr = (JsonArray) reader.readObject().get("data");
			// there should be only one
			return parseTerm(arr.getJsonObject(0));
		}
	}

	/**
	 * Get a term with a specific IRI from a Drupal 9 website 
	 * 
	 * @param taxonomy
	 * @return taxonomy term or null
	 * @throws IOException 
	 */
	public Set<Term> getAllTerms(String taxonomy) throws IOException {
		Request req = Request.Get(getWebsite() + "/en/jsonapi/taxonomy_term/" + taxonomy)
			.addHeader(HttpHeaders.ACCEPT, "application/vnd.api+json");
		
		HttpResponse resp = req.execute().returnResponse();

		if (resp.getStatusLine().getStatusCode() >= 400) {
			LOG.error("Error in HTTP status");
			return null;
		}

		try(InputStream is = new BufferedInputStream(resp.getEntity().getContent());
			JsonReader reader = Json.createReader(is)) {

			if (reader == null) {
				LOG.error("Could not read JSON");
				return null;
			}

			JsonArray arr = (JsonArray) reader.readObject().get("data");

			return arr.stream().filter(JsonObject.class::isInstance)
							.map(JsonObject.class::cast)
							.map(o -> parseTerm(o))
							.collect(Collectors.toSet());
		}
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
	 * Parse a single term
	 * 
	 * @param m full RDF model
	 * @param subj subject IRI
	 * @return term
	 */
	private Term parseTerm(Model m, Resource subj) {
		Set<Value> values = m.filter(subj, SKOS.PREF_LABEL, null).objects();
		Map<String, String> prefs = values.stream().map(Literal.class::cast)
				.collect(Collectors.toMap(l -> l.getLanguage().orElse("und"), l -> l.getLabel()));
		IRI parent = (IRI) m.filter(subj, SKOS.BROADER, null).objects().stream().findFirst().orElse(null);
		return new Term((IRI) subj, prefs, parent, null);
	}

	/**
	 * Parse the SKOS file into a set of taxonomy terms
	 * 
	 * @param file SKOS file
	 * @return set of terms
	 * @throws IOException 
	 */
	public List<Term> parse(File file) throws IOException {
		LOG.info("Parsing file {}", file.toString());
		Model m = loadSkos(file);
		
		Set<Resource> uris = m.filter(null, RDF.TYPE, SKOS.CONCEPT).subjects();
		LOG.info("Found {} terms", uris.size());

		// add revers relations, so we can start at top and add narrower terms afterwards
		for(Statement stmt: m.filter(null, SKOS.BROADER, null)) {
			m.add((IRI) stmt.getObject(), SKOS.NARROWER, stmt.getSubject());
		}
		
		// get the top terms first, i.e all terms without a parent/broader relation
		List<Term> terms = uris.stream()
			.filter(u ->  m.filter(u, SKOS.BROADER, null).isEmpty())
			.map(u -> parseTerm(m, u))
			.collect(Collectors.toList());
	
		LOG.info("Found {} top terms", terms.size());

		// if there are more terms than only top terms, it is a tree-like taxonomy with parents/children
		if (uris.size() > terms.size()) {
			List<Term> parents = new ArrayList<>(terms);
			List<Term> children;
		
			do {	
				children = parents.stream()
					.flatMap(t -> m.filter(t.subject(), SKOS.NARROWER, null).stream())
					.map(s -> parseTerm(m, (IRI) s.getObject()))
					.collect(Collectors.toList());
				terms.addAll(children);
				parents = new ArrayList<>(children);
			} while (!children.isEmpty());
		}
		return terms;
	}

	
	/**
	 * Constructor
	 * 
	 * @param website base url of the website
	 * @param user user name
	 * @param pass password
	 */
	public TaxonomyLoader(String website, String user, String pass) {
		super(website, user, pass);
	}
}
