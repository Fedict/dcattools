/*
 * Copyright (c) 2015, FPS BOSA DG DT
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
import be.fedict.dcat.helpers.Fetcher;
import be.fedict.dcat.vocab.DATAGOVBE;
import be.fedict.dcat.vocab.MDR_LANG;
import be.fedict.dcat.vocab.SCHEMA;

import com.google.common.hash.HashFunction;
import com.google.common.hash.Hashing;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Writer;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.util.Values;
import org.eclipse.rdf4j.model.vocabulary.DCAT;
import org.eclipse.rdf4j.model.vocabulary.DCTERMS;
import org.eclipse.rdf4j.model.vocabulary.FOAF;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.model.vocabulary.XSD;
import org.eclipse.rdf4j.repository.RepositoryException;
import org.eclipse.rdf4j.rio.RDFFormat;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * BaseScraper scraper class
 *
 * @author Bart Hanssens
 */
public abstract class BaseScraper extends Fetcher implements Scraper, AutoCloseable {
	protected final static Logger logger = LoggerFactory.getLogger(getClass());

	protected final static String PROP_PREFIX = "be.fedict.dcat.scrapers";
	protected final static String PKG_PREFIX = "/be/fedict/dcat/scrapers";
	
	private Cache cache = null;
	private Storage store = null;
	private URL base = null;

	private String defLang = "";
	private String[] allLangs = {};

	private String name = "";

	private final static HashFunction HASHER = Hashing.sha1();

	protected final static String TODAY = new SimpleDateFormat("yyyy-MM-dd").format(new Date());

	/**
	 * Get cache
	 *
	 * @return local cache file
	 */
	protected Cache getCache() {
		return cache;
	}

	/**
	 * Get base URL
	 *
	 * @return base URL
	 */
	public URL getBase() {
		return base;
	}

	/**
	 * Set default language
	 *
	 * @param lang language code
	 */
	public void setDefaultLang(String lang) {
		this.defLang = lang;
	}

	/**
	 * Get default language
	 *
	 * @return language language code
	 */
	public String getDefaultLang() {
		return defLang;
	}

	/**
	 * Set all languages
	 *
	 * @param langs array of language codes
	 */
	public void setAllLangs(String[] langs) {
		this.allLangs = langs;
	}

	/**
	 * Get all languages
	 *
	 * @return array of language codes
	 */
	public String[] getAllLangs() {
		return allLangs;
	}

	/**
	 * Set name
	 *
	 * @param name name
	 */
	public void setName(String name) {
		this.name = name;
	}

	/**
	 * Get name
	 *
	 * @return name
	 */
	public String getName() {
		return name;
	}

	/**
	 * Get lowercase file extension
	 *
	 * @param href
	 * @return file extension or empty string
	 */
	protected String getFileExt(String href) {
		String ext = "";
		int dot = href.lastIndexOf('.');
		if (dot > 0) {
			ext = href.substring(dot + 1);
			int q = ext.lastIndexOf('?');
			if (q > 0) {
				ext = ext.substring(0, q);
			}
			// Wasn't a file in the first place
			if (ext.contains("/")) {
				ext = "";
			}
		}
		return ext.toLowerCase();
	}

	/**
	 * Get required property
	 *
	 * @param prop properties
	 * @param name unprefixed property
	 * @return value of the property
	 * @throws IOException if property is empty
	 */
	protected final String getRequiredProperty(Properties prop, String name) throws IOException {
		String value = prop.getProperty(BaseScraper.PROP_PREFIX + "." + name, "");
		if (value.isEmpty()) {
			throw new IOException("Property missing: " + name);
		}
		return value;
	}

	/**
	 * Get optional property
	 *
	 * @param prop properties
	 * @param name unprefixed property
	 * @return property value
	 */
	protected final String getProperty(Properties prop, String name) {
		String value = prop.getProperty(BaseScraper.PROP_PREFIX + "." + name);
		if (value == null) {
			logger.warn("No property {}", name);
		}
		return value;
	}

	/**
	 * Make a hashed ID based upon a string.
	 *
	 * @param s
	 * @return
	 */
	protected String makeHashId(String s) {
		return HASHER.hashBytes(s.getBytes(StandardCharsets.UTF_8)).toString();
	}

	/**
	 * Make an URL for a DCAT Catalog
	 *
	 * @return URL
	 * @throws MalformedURLException
	 */
	protected URL makeCatalogURL() throws MalformedURLException {
		return new URL(DATAGOVBE.PREFIX_URI_CAT + "/" + getName());
	}

	/**
	 * Make an URL for a DCAT Dataset
	 *
	 * @param id
	 * @return URL
	 * @throws MalformedURLException
	 */
	protected URL makeDatasetURL(String id) throws MalformedURLException {
		return new URL(DATAGOVBE.PREFIX_URI_DATASET + "/" + getName() + "/"
			+ id.replace(".", "-").replace(":", "-"));
	}
	
	/**
	 * Make an IRI for a DCAT Dataset
	 *
	 * @param id
	 * @return URL
	 * @throws MalformedURLException
	 */
	protected IRI makeDatasetIRI(String id) throws MalformedURLException {
		return Values.iri(DATAGOVBE.PREFIX_URI_DATASET + "/" + getName() + "/"
			+ id.replace(".", "-").replace(":", "-"));
	}
	
	/**
	 * Make an URL for a DCAT Distribution
	 *
	 * @param id
	 * @return URL
	 * @throws java.net.MalformedURLException
	 */
	protected URL makeDistURL(String id) throws MalformedURLException {
		return new URL(DATAGOVBE.PREFIX_URI_DIST + "/" + getName() + "/"
			+ id.replace(".", "-").replace(":", "-"));
	}
	/**
	 * Make an IRI for a DCAT Distribution
	 *
	 * @param id
	 * @return URL
	 */
	protected IRI makeDistIRI(String id) {
		return Values.iri(DATAGOVBE.PREFIX_URI_DIST + "/" + getName() + "/"
			+ id.replace(".", "-").replace(":", "-"));
	}

	/**
	 * Make an URL for an organization
	 *
	 * @param id
	 * @return URL
	 * @throws java.net.MalformedURLException
	 */
	protected URL makeOrgURL(String id) throws MalformedURLException {
		return new URL(DATAGOVBE.PREFIX_URI_ORG + "/" + getName() + "/" + id);
	}

	/**
	 * Make an URL for a date
	 *
	 * @param start start date string
	 * @param end end date string
	 * @return URL
	 * @throws java.net.MalformedURLException
	 */
	protected URL makeTemporalURL(String start, String end) throws MalformedURLException {
		String[] s = start.split("T");
		String[] e = end.split("T");

		return new URL(DATAGOVBE.PREFIX_URI_TEMPORAL + "/" + s[0] + "_" + e[0]);
	}

	/**
	 * Generate temporal triples
	 *
	 * @param store triple store
	 * @param dataset URI
	 * @param str string to parse
	 * @param p pattern
	 * @param sep date separator
	 * @throws MalformedURLException
	 */
	protected void generateTemporal(Storage store, IRI dataset, String str, Pattern p, String sep)
		throws MalformedURLException {
		Matcher m = p.matcher(str);
		if (!m.matches()) {
			return;
		}
		String span = m.group(1);
		String[] split = span.split(sep);

		generateTemporal(store, dataset, split[0], split[1]);
	}

	/**
	 * Generate temporal triples
	 *
	 * @param store triple store
	 * @param dataset URI
	 * @param start start date
	 * @param end end date
	 * @throws MalformedURLException
	 */
	protected void generateTemporal(Storage store, IRI dataset, String start, String end)
		throws MalformedURLException {
		String s = start.trim();

		if (s.isEmpty()) {
			logger.warn("Empty start date");
			return;
		}
		String e = end.trim();

		// Assume start of year / end of year when only YYYY is given 
		switch (s.length()) {
			case 4:
				s += "-01-01";
				break;
			case 6:
				s = s.substring(0, 4) + "-" + s.substring(4) + "-01";
				break;
			default:
				s = s.replace("/", "-");
		}

		switch (e.length()) {
			case 0:
				e = TODAY;
				break;
			case 4:
				e += "-12-31";
				break;
			case 6:
				e = e.substring(0, 4) + "-" + e.substring(4);
				break;
			default:
				e = e.replace("/", "-");
		}

		IRI u = store.getURI(makeTemporalURL(s, e).toString());
		store.add(dataset, DCTERMS.TEMPORAL, u);
		store.add(u, SCHEMA.START_DATE, s, (s.length() == 10) ? XSD.DATE : XSD.DATETIME);
		store.add(u, SCHEMA.END_DATE, e, (e.length() == 10) ? XSD.DATE : XSD.DATETIME);
	}

	/**
	 * Load script file names from resources scripts.txt file
	 * 
	 * @param file name of the file with the list of scrips
	 * @return list of file names
	 */
	private List<String> loadScripts(String file) throws IOException {
		List<String> scripts;

		String fname = PKG_PREFIX + "/" + getName() + "/" + file;
		logger.info("Load script list from {}", file);
		try(InputStream is = BaseScraper.class.getResourceAsStream(fname);
			BufferedReader r = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
			scripts = r.lines().filter(s -> !s.startsWith("#"))		// remove comments
								.filter(s -> !s.isBlank())			// remove empty lines
								.map(s -> PKG_PREFIX + "/" + s)			// add prefix
								.collect(Collectors.toList());
		}
		logger.info("Found {} scripts", scripts.size());

		return scripts;
	}

	/**
	 * Load additional data and run additional scripts, if any
	 * 
	 * @param file name of the script file
	 * @throws IOException 
	 */
	protected void enhance(String file) throws IOException {
		List<String> scripts = loadScripts(file);
		
		for (String script: scripts) {
			if (script.endsWith("ttl")) {
				logger.info("Loading data from {}", script);
				try(InputStream is = BaseScraper.class.getResourceAsStream(script)) {
					store.add(is, RDFFormat.TURTLE);
				}
			} else if (script.endsWith("qry")) {
				logger.info("Execute query {}", script);
				try(InputStream is = BaseScraper.class.getResourceAsStream(script);
					BufferedReader r = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
					String sparql = r.lines().filter(s -> !s.startsWith("#"))
											.collect(Collectors.joining(System.lineSeparator()));
					store.queryUpdate(sparql);
				}
			}
		}
	}

	/**
	 * Generate DCAT from cache and write it to the RDF store
	 *
	 * @param cache cache
	 * @param store RDF store
	 * @throws IOException
	 */
	protected abstract void generateDcat(Cache cache, Storage store) throws IOException;

	/**
	 * Generate DCAT-AP from cache
	 *
	 * @throws IOException
	 */
	protected void generateDcat() throws IOException {
		generateDcat(cache, store);
		enhance("scripts.txt");
	}

	/**
	 * Extra DCAT catalog info
	 *
	 * @param store RDF store
	 * @param catalog catalog URI
	 * @throws RepositoryException
	 */
	public void generateCatalogInfo(Storage store, IRI catalog) throws RepositoryException {
		store.add(catalog, DCTERMS.TITLE, "DCAT Catalog for " + getName(), "en");
		store.add(catalog, DCTERMS.DESCRIPTION, "Converted by BOSA DG DT converter", "en");
		store.add(catalog, DCTERMS.MODIFIED, new Date());
		store.add(catalog, DCTERMS.LICENSE, DATAGOVBE.LICENSE_CC0);
		store.add(catalog, FOAF.HOMEPAGE, getBase());

		String[] langs = getAllLangs();
		for (String lang : langs) {
			store.add(catalog, DCTERMS.LANGUAGE, MDR_LANG.MAP.get(lang));
		}
	}

	/**
	 * Generate DCAT Catalog.
	 *
	 * @param store RDF store
	 * @throws RepositoryException
	 * @throws MalformedURLException
	 */
	public void generateCatalog(Storage store) throws RepositoryException, MalformedURLException {
		IRI catalog = store.getURI(makeCatalogURL().toString());
		store.add(catalog, RDF.TYPE, DCAT.CATALOG);

		List<IRI> uris = store.query(DCAT.DATASET);
		for (IRI u : uris) {
			store.add(catalog, DCAT.HAS_DATASET, u);
		}
		generateCatalogInfo(store, catalog);
	}

		
	@Override
	public void writeDcat(Writer out) throws RepositoryException, MalformedURLException {
		store.write(out);

	}

	@Override
	public void close() {
		cache.shutdown();
		store.shutdown();		
	}

	/**
	 * Constructor
	 * 
	 * @param prop
	 * @throws IOException 
	 */
	protected BaseScraper(Properties prop) throws IOException {
		this.base = new URL(getRequiredProperty(prop, "url"));
		this.store = new Storage();
		this.defLang = getRequiredProperty(prop, "deflanguage");
		this.allLangs = getRequiredProperty(prop, "languages").split(",");
		this.cache = new Cache(new File(getRequiredProperty(prop, "cache")));

		String delay = prop.getProperty(BaseScraper.PROP_PREFIX + ".http.delay", "500");
		setDelay(Integer.valueOf(delay));
		
		store.startup();
	}
}
