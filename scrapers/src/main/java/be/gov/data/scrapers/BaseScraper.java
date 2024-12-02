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
package be.gov.data.scrapers;

import be.gov.data.helpers.Storage;
import be.gov.data.helpers.Fetcher;
import be.gov.data.dcat.vocab.DATAGOVBE;
import be.gov.data.dcat.vocab.MDR_LANG;

import com.google.common.hash.HashFunction;
import com.google.common.hash.Hashing;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Writer;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
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
	protected final Logger LOG = LoggerFactory.getLogger(getClass());

	protected final static String PROP_PREFIX = "be.gov.data.scrapers";
	protected final static String PKG_PREFIX = "/be/gov/data/scrapers";
	
	private final String dataDir;
	private final Cache cache;
	private final Storage store;
	private final URL base;

	private String defLang = "";
	private String[] allLangs = {};

	private String name = "";
	private boolean raw = false;
	
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
	 * Get datadir
	 *
	 * @return data directory
	 */
	protected String getDataDir() {
		return dataDir;
	}

	/**
	 * Set raw output mode
	 * 
	 * @param raw 
	 */
	@Override
	public void setRawOutput(boolean raw) {
		this.raw = raw;
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
			LOG.warn("No property {}", name);
		}
		return value;
	}

	/**
	 * Make a hash, mostly used for creating a hash ID based upon a string.
	 *
	 * @param s
	 * @return
	 */
	protected String hash(String s) {
		return HASHER.hashBytes(s.getBytes(StandardCharsets.UTF_8)).toString();
	}

	/**
	 * Check if hash of new page equals text of previous page
	 * 
	 * @param prevhash previous hash
	 * @param page text of the new page
	 * @return hash of new page
	 * @throws IOException when hashes are the same
	 */
	protected String detectLoop(String prevhash, String page) throws IOException {
		String newhash = hash(page);
		if (newhash.equals(prevhash)) {	
			throw new IOException("Page loop detected");
		}
		return newhash;
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
	 * Make IRI 
	 * 
	 * @param prefix
	 * @param id
	 * @return 
	 */
	private IRI makeIRI(String prefix, String id) {
		return Values.iri(prefix + "/" + getName() + "/" + id.replace(".", "-").replace(":", "-"));
	}

	/**
	 * Make an IRI for a DCAT Dataset
	 *
	 * @param id
	 * @return URL
	 */
	protected IRI makeDatasetIRI(String id) {
		return makeIRI(DATAGOVBE.PREFIX_URI_DATASET, id);
	}

	/**
	 * Make an IRI for a DCAT bounding box
	 *
	 * @param n
	 * @param e
	 * @param s
	 * @param w
	 * @return URL
	 */
	protected IRI makeBboxIRI(String n, String e, String s, String w) {
		String id = n.replace(".", "_") + "-" + e.replace(".", "_") 
			+ "-" + s.replace(".", "_") + "-" + w.replace(".", "_");
		return makeIRI(DATAGOVBE.PREFIX_URI_GEO, id);
	}

	/**
	 * Make an IRI for a DCAT Distribution
	 *
	 * @param id
	 * @return URL
	 */
	protected IRI makeDistIRI(String id) {
		return makeIRI(DATAGOVBE.PREFIX_URI_DIST, id);
	}

	/**
	 * Make an IRI for an organization
	 *
	 * @param id
	 * @return IRI
	 */
	protected IRI makeOrgIRI(String id){
		return makeIRI(DATAGOVBE.PREFIX_URI_ORG, id);
	}

	/**
	 * Make an IRI for a person
	 *
	 * @param id
	 * @return IRI
	 */
	protected IRI makePersonIRI(String id){
		return makeIRI(DATAGOVBE.PREFIX_URI_PERSON, id);
	}

	/**
	 * Make an IRI for a date
	 *
	 * @param start start date string
	 * @param end end date string
	 * @return URL
	 */
	protected IRI makeTemporalURL(String start, String end) {
		String[] s = start.split("T");
		String[] e = end.split("T");

		return Values.iri(DATAGOVBE.PREFIX_URI_TEMPORAL + "/" + s[0] + "_" + e[0]);
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

		parseTemporal(store, dataset, split[0], split[1]);
	}

	/**
	 * Generate temporal triples
	 *
	 * @param store triple store
	 * @param dataset URI
	 * @param start start date
	 * @param end end date
	 */
	protected void parseTemporal(Storage store, IRI dataset, String start, String end) {
		String s = start.trim();

		if (s.isEmpty()) {
			LOG.warn("Empty start date");
			return;
		}
		String e = end.trim();

		// Assume start of year / end of year when only YYYY is given 
		switch (s.length()) {
			case 4 -> s += "-01-01";
			case 6 -> s = s.substring(0, 4) + "-" + s.substring(4) + "-01";
			case 7 -> s += "-01";
			default -> s = s.replace("/", "-");
		}

		switch (e.length()) {
			case 0 -> e = TODAY;
			case 4 -> e += "-12-31";
			case 6 -> e = e.substring(0, 4) + "-" + e.substring(4);
			case 7 -> e += "-28";
			default -> e = e.replace("/", "-");
		}

		IRI u = store.getURI(makeTemporalURL(s, e).toString());
		store.add(dataset, DCTERMS.TEMPORAL, u);
		store.add(u,  RDF.TYPE, DCTERMS.PERIOD_OF_TIME);
		store.add(u, DCAT.START_DATE, s, (s.length() == 10) ? XSD.DATE : XSD.DATETIME);
		store.add(u, DCAT.END_DATE, e, (e.length() == 10) ? XSD.DATE : XSD.DATETIME);
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
		LOG.info("Load script list from {}", file);
		try(InputStream is = BaseScraper.class.getResourceAsStream(fname);
			BufferedReader r = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
			scripts = r.lines().filter(s -> !s.startsWith("#"))		// remove comments
								.filter(s -> !s.isBlank())			// remove empty lines
								.map(s -> PKG_PREFIX + "/" + s)			// add prefix
								.collect(Collectors.toList());
		}
		LOG.info("Found {} scripts", scripts.size());

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
				LOG.info("Loading data from {}", script);
				try(InputStream is = BaseScraper.class.getResourceAsStream(script)) {
					store.add(is, RDFFormat.TURTLE);
				}
			} else if (script.endsWith("qry")) {
				LOG.info("Execute query {}", script);
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
		if (raw == false) {
			enhance("scripts.txt");
		}
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
		List<IRI> datasets = store.query(DCAT.DATASET);
		for (IRI u : datasets) {
			store.add(catalog, DCAT.HAS_DATASET, u);
		}
		List<IRI> services = store.query(DCAT.DATA_SERVICE);
		for (IRI u : services) {
			store.add(catalog, DCAT.HAS_SERVICE, u);
		}
		generateCatalogInfo(store, catalog);
	}

		
	@Override
	public final void writeDcat(Writer out) throws RepositoryException, MalformedURLException {
		store.write(out);
	}

	/**
	 * Write DCAT to a local file
	 * 
	 * @throws IOException 
	 */
	public void writeDcat() throws IOException {
		// output file
		String outfile = String.join(File.separator, dataDir, getName() + ".nt");
		LOG.info("Writing end results to {}", outfile);
		try (BufferedWriter bw = Files.newBufferedWriter(Paths.get(outfile), StandardCharsets.UTF_8)) {
			writeDcat(bw);
		}
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
		this.dataDir = getRequiredProperty(prop, "datadir");
		this.cache = new Cache(new File(getRequiredProperty(prop, "cache")));

		String delay = prop.getProperty(BaseScraper.PROP_PREFIX + ".http.delay", "500");
		setDelay(Integer.parseInt(delay));
		
		store.startup();
	}
}
