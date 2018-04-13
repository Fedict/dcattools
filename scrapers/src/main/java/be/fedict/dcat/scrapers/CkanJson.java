/*
 * Copyright (c) 2015, Bart Hanssens <bart.hanssens@fedict.be>
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
import be.fedict.dcat.helpers.Page;
import be.fedict.dcat.vocab.MDR_LANG;

import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.ParseException;
import java.util.Map;
import java.util.regex.Pattern;

import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.json.JsonReader;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.vocabulary.DCAT;
import org.eclipse.rdf4j.model.vocabulary.DCTERMS;
import org.eclipse.rdf4j.model.vocabulary.FOAF;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.model.vocabulary.VCARD4;

import org.eclipse.rdf4j.repository.RepositoryException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Abstract scraper for CKAN portals, using the JSON API.
 *
 * @author Bart Hanssens <bart.hanssens@fedict.be>
 */
public abstract class CkanJson extends Ckan {

	private final Logger logger = LoggerFactory.getLogger(CkanJson.class);

	// CKAN JSON fields   
	public final static String ID = "id";
	public final static String AUTHOR = "author";
	public final static String AUTHOR_EML = "author_email";
	public final static String CREATED = "created";
	public final static String FORMAT = "format";
	public final static String LICENSE_ID = "license_id";
	public final static String LICENSE_URL = "license_url";
	public final static String MAINT = "maintainer";
	public final static String MAINT_EML = "maintainer_email";
	public final static String META_CREATED = "metadata_created";
	public final static String META_MODIFIED = "metadata_modified";
	public final static String MODIFIED = "last_modified";
	public final static String NAME = "name";
	public final static String NOTES = "notes";
	public final static String ORGANIZATION = "organization";
	public final static String IS_ORG = "is_organization";
	public final static String RESOURCES = "resources";
	public final static String TAGS = "tags";
	public final static String TITLE = "title";
	public final static String URL = "url";

	public final static String EXTRA = "extras";

	public final static String KEY = "key";
	public final static String VALUE = "value";

	public final static Pattern YEAR_PAT
			= Pattern.compile(".*((18|19|20)[0-9]{2}-(19|20)[0-9]{2}).*");

	/**
	 * Make an URL for retrieving JSON of CKAN Package (DCAT Dataset)
	 *
	 * @param id
	 * @return URL
	 * @throws java.net.MalformedURLException
	 */
	@Override
	protected URL ckanDatasetURL(String id) throws MalformedURLException {
		return new URL(getBase(), Ckan.API_PKG + id);
	}

	/**
	 * Get URL of a CKAN page.
	 *
	 * @param id
	 * @return URL
	 * @throws MalformedURLException
	 */
	protected URL ckanPageURL(String id) throws MalformedURLException {
		return new URL(getBase(), Ckan.DATASET + id);
	}

	/**
	 * Parse a CKAN string and store it in the RDF store.
	 *
	 * @param store RDF store
	 * @param uri RDF subject URI
	 * @param obj JsonObject
	 * @param field CKAN field name
	 * @param property RDF property
	 * @param lang language
	 * @throws RepositoryException
	 */
	protected void parseString(Storage store, IRI uri, JsonObject obj,
			String field, IRI property, String lang) throws RepositoryException {
		String s = obj.getString(field, "");
		if (!s.isEmpty()) {
			if (lang != null) {
				store.add(uri, property, s, lang);
			} else {
				store.add(uri, property, s);
			}
		}
	}

	/**
	 * Parse a CKAN temporal and store it in the RDF store.
	 *
	 * @param store RDF store
	 * @param uri RDF subject URI
	 * @param obj JsonObject
	 * @param field CKAN field name
	 * @param property RDF property
	 * @throws RepositoryException
	 * @throws MalformedURLException
	 */
	protected void parseTemporal(Storage store, IRI uri, JsonObject obj, String field, IRI property)
			throws RepositoryException, MalformedURLException {
		String s = obj.getString(field, "");
		generateTemporal(store, uri, s, YEAR_PAT, "-");
	}

	/**
	 * Parse a CKAN string and store it in the RDF store
	 *
	 * @param store RDF store
	 * @param uri RDF subject URI
	 * @param obj JsonObject
	 * @param field CKAN field name
	 * @param property RDF property
	 * @throws RepositoryException
	 * @throws MalformedURLException
	 */
	protected void parseURI(Storage store, IRI uri, JsonObject obj, String field,
			IRI property) throws RepositoryException, MalformedURLException {
		String s = obj.getString(field, "");
		if (!s.isEmpty()) {
			s = s.replace(" ", "%20").replace("\\", "%5C");
			URL url = new URL(s);
			store.add(uri, property, url);
		}
	}

	/**
	 * Parse a CKAN date and store it in the RDF store
	 *
	 * @param store RDF store
	 * @param uri RDF subject URI
	 * @param obj JsonObject
	 * @param field CKAN field name
	 * @param property RDF property
	 * @throws RepositoryException
	 */
	protected void parseDate(Storage store, IRI uri, JsonObject obj,
			String field, IRI property) throws RepositoryException {
		String s = obj.getString(field, "");
		if (!s.isEmpty()) {
			try {
				store.add(uri, property, DATEFMT.parse(s));
			} catch (ParseException ex) {
				logger.warn("Could not parse date {}", s, ex);
			}
		}
	}

	/**
	 * Parse a CKAN contact and store it in the RDF store
	 *
	 * @param store RDF store
	 * @param uri RDF subject URI
	 * @param name contact name
	 * @param email contact
	 * @throws RepositoryException
	 */
	protected void parseContact(Storage store, IRI uri, String name, String email)
			throws RepositoryException {
		String v = "";
		try {
			v = makeOrgURL(makeHashId(name + email)).toString();
		} catch (MalformedURLException e) {
			logger.error("Could not generate hash url", e);
		}

		if (!name.isEmpty() || !email.isEmpty()) {
			IRI vcard = store.getURI(v);
			store.add(uri, DCAT.CONTACT_POINT, vcard);
			store.add(vcard, RDF.TYPE, VCARD4.ORGANIZATION);
			if (!name.isEmpty()) {
				store.add(vcard, VCARD4.HAS_FN, name);
			}
			if (!email.isEmpty()) {
				store.add(vcard, VCARD4.HAS_EMAIL, store.getURI("mailto:" + email));
			}
		}
	}

	/**
	 * Parse a CKAN contact and store it in the RDF store
	 *
	 * @param store RDF store
	 * @param uri RDF subject URI
	 * @param obj JsonObject
	 * @param field CKAN field name
	 * @param field2 CKAN field email
	 * @param property RDF property
	 * @throws RepositoryException
	 */
	protected void parseContact(Storage store, IRI uri, JsonObject obj,
			String field, String field2, IRI property) throws RepositoryException {
		String name = obj.getString(field, "");
		String email = obj.getString(field2, "");

		// Check if this is actually an escaped JSON array
		if (name.startsWith("[{")) {
			name = name.replace("\\", "");

			JsonReader json = Json.createReader(new StringReader(name));
			JsonArray arr = json.readArray();
			for (JsonObject o : arr.getValuesAs(JsonObject.class)) {
				parseContact(store, uri, o, CkanJson.NAME, field2, property);
			}
			return;
		}
		parseContact(store, uri, name, email);
	}

	/**
	 * Parse CKAN dataset in JSON format.
	 *
	 * @param store RDF store
	 * @param uri RDF subject
	 * @param lang language
	 * @param json JSON object with CKAN data
	 * @throws RepositoryException
	 * @throws MalformedURLException
	 */
	protected void ckanGeneral(Storage store, IRI uri, JsonObject json, String lang)
			throws RepositoryException, MalformedURLException {
		parseString(store, uri, json, CkanJson.ID, DCTERMS.IDENTIFIER, null);
		parseString(store, uri, json, CkanJson.TITLE, DCTERMS.TITLE, lang);
		parseString(store, uri, json, CkanJson.NOTES, DCTERMS.DESCRIPTION, lang);

		parseDate(store, uri, json, CkanJson.META_CREATED, DCTERMS.ISSUED);
		parseDate(store, uri, json, CkanJson.META_MODIFIED, DCTERMS.MODIFIED);

		parseContact(store, uri, json, CkanJson.AUTHOR, CkanJson.AUTHOR_EML, DCAT.CONTACT_POINT);
		parseContact(store, uri, json, CkanJson.MAINT, CkanJson.MAINT_EML, DCAT.CONTACT_POINT);
	}

	/**
	 * Parse CKAN tags in JSON format.
	 *
	 * @param store RDF store
	 * @param uri RDF subject
	 * @param json JSON object with CkanJson data
	 * @param lang language
	 * @throws RepositoryException
	 */
	protected void ckanTags(Storage store, IRI uri, JsonObject json, String lang)
			throws RepositoryException {
		JsonArray arr = json.getJsonArray(CkanJson.TAGS);

		for (JsonObject obj : arr.getValuesAs(JsonObject.class)) {
			parseString(store, uri, obj, CkanJson.NAME, DCAT.KEYWORD, lang);
		}
	}

	/**
	 * Parse CKAN resources.
	 *
	 * @param store RDF store
	 * @param dataset dataset
	 * @param json JSON
	 * @param lang language code
	 * @throws RepositoryException
	 * @throws MalformedURLException
	 */
	protected void ckanResources(Storage store, IRI dataset, JsonObject json, String lang)
			throws RepositoryException, MalformedURLException {

		/* CKAN page / access page */
		URL access = ckanPageURL(json.getString(CkanJson.ID, ""));

		JsonArray arr = json.getJsonArray(CkanJson.RESOURCES);

		for (JsonObject obj : arr.getValuesAs(JsonObject.class)) {
			String id = obj.getString(CkanJson.ID, "");
			IRI dist = store.getURI(makeDistURL(id).toString());
			logger.debug("Generating distribution {}", dist.toString());

			store.add(dataset, DCAT.HAS_DISTRIBUTION, dist);
			store.add(dist, RDF.TYPE, DCAT.DISTRIBUTION);

			parseString(store, dist, obj, CkanJson.ID, DCTERMS.IDENTIFIER, null);
			parseString(store, dist, obj, CkanJson.NAME, DCTERMS.TITLE, lang);
			parseDate(store, dist, obj, CkanJson.CREATED, DCTERMS.CREATED);
			parseDate(store, dist, obj, CkanJson.MODIFIED, DCTERMS.MODIFIED);
			parseString(store, dist, obj, CkanJson.FORMAT, DCAT.MEDIA_TYPE, null);
			parseURI(store, dist, obj, CkanJson.URL, DCAT.DOWNLOAD_URL);

			// License from dataset must be on the distribution
			parseString(store, dist, json, CkanJson.LICENSE_ID, DCTERMS.LICENSE, null);
			parseURI(store, dist, json, CkanJson.LICENSE_URL, DCTERMS.RIGHTS);

			store.add(dist, DCAT.ACCESS_URL, access);
			store.add(dist, DCTERMS.LANGUAGE, MDR_LANG.MAP.get(lang));
		}
	}

	/**
	 * Parse CKAN organization
	 *
	 * @param store
	 * @param uri
	 * @param json
	 * @param lang language code
	 * @throws RepositoryException
	 * @throws MalformedURLException
	 */
	protected void ckanOrganization(Storage store, IRI uri, JsonObject json, String lang)
			throws RepositoryException, MalformedURLException {
		if (!json.isNull(CkanJson.ORGANIZATION)) {
			JsonObject obj = json.getJsonObject(CkanJson.ORGANIZATION);

			if (obj.getBoolean(CkanJson.IS_ORG)) {
				String s = obj.getString(CkanJson.ID, "");
				IRI org = store.getURI(makeOrgURL(s).toString());
				store.add(uri, DCTERMS.PUBLISHER, org);
				store.add(org, RDF.TYPE, FOAF.ORGANIZATION);

				parseString(store, org, obj, CkanJson.NAME, FOAF.NAME, lang);
			}
		}
	}

	/**
	 * Parse CKAN extra fields.
	 *
	 * @param store RDF store
	 * @param uri
	 * @param json
	 * @param lang language code
	 * @throws RepositoryException
	 * @throws MalformedURLException
	 */
	protected abstract void ckanExtras(Storage store, IRI uri, JsonObject json, String lang)
			throws RepositoryException, MalformedURLException;

	/**
	 * Generate DCAT Dataset
	 *
	 * @param store RDF store
	 * @param id
	 * @param page full JSON object
	 * @throws MalformedURLException
	 * @throws RepositoryException
	 */
	@Override
	protected void generateDataset(Storage store, String id, Map<String, Page> page)
			throws MalformedURLException, RepositoryException {
		String lang = getDefaultLang();

		Page p = page.getOrDefault("", new Page());
		JsonReader reader = Json.createReader(new StringReader(p.getContent()));
		JsonObject obj = reader.readObject();

		String ckanid = obj.getString(CkanJson.ID, "");
		IRI dataset = store.getURI(makeDatasetURL(ckanid).toString());
		logger.info("Generating dataset {}", dataset.toString());

		store.add(dataset, RDF.TYPE, DCAT.DATASET);
		store.add(dataset, DCTERMS.LANGUAGE, MDR_LANG.MAP.get(lang));

		/* Parse different sections of CKAN JSON */
		ckanGeneral(store, dataset, obj, lang);
		ckanTags(store, dataset, obj, lang);
		ckanResources(store, dataset, obj, lang);
		ckanOrganization(store, dataset, obj, lang);
		ckanExtras(store, dataset, obj, lang);
	}

	/**
	 * Get a CKAN package (DCAT Dataset).
	 *
	 * @param url
	 * @return JsonObject containing CKAN Package or NULL
	 * @throws IOException
	 */
	protected JsonObject scrapePackage(URL url) throws IOException {
		JsonObject obj = makeJsonRequest(url);
		if (obj.getBoolean(CkanJson.SUCCESS)) {
			return obj.getJsonObject(CkanJson.RESULT);
		}
		return null;
	}

	/**
	 * Get JSON body of a page
	 *
	 * @param url
	 * @return string version of the page
	 * @throws IOException
	 */
	@Override
	protected String getPage(URL url) throws IOException {
		JsonObject obj = scrapePackage(url);
		return obj.toString();
	}

	/**
	 * CKAN scraper.
	 *
	 * @param caching local cache file
	 * @param storage local triple store file
	 * @param base URL of the CKAN site
	 */
	public CkanJson(File caching, File storage, URL base) {
		super(caching, storage, base);
	}
}
