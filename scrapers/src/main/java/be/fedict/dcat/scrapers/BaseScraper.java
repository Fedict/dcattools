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
import be.fedict.dcat.helpers.Cache;
import be.fedict.dcat.helpers.Fetcher;
import be.fedict.dcat.vocab.DATAGOVBE;
import be.fedict.dcat.vocab.MDR_LANG;
import be.fedict.dcat.vocab.SCHEMA;

import com.google.common.hash.HashFunction;
import com.google.common.hash.Hashing;

import java.io.File;
import java.io.IOException;
import java.io.Writer;

import java.net.MalformedURLException;
import java.net.URL;

import java.text.SimpleDateFormat;

import java.util.Date;
import java.util.List;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.vocabulary.DCAT;
import org.eclipse.rdf4j.model.vocabulary.DCTERMS;
import org.eclipse.rdf4j.model.vocabulary.FOAF;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.model.vocabulary.XSD;
import org.eclipse.rdf4j.repository.RepositoryException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * BaseScraper scraper class
 *
 * @author Bart Hanssens
 */
public abstract class BaseScraper extends Fetcher implements Scraper {

	protected final Logger logger = LoggerFactory.getLogger(getClass());

	public final static String PROP_PREFIX = "be.fedict.dcat.scrapers";

	private Cache cache = null;
	private Storage store = null;
	private URL base = null;

	private String defLang = "";
	private String[] allLangs = {};

	private String name = "";

	private final static HashFunction HASHER = Hashing.sha1();

	public final static String TODAY = new SimpleDateFormat("yyyy-MM-dd").format(new Date());

	/**
	 * Get cache
	 *
	 * @return local cache file
	 */
	public final Cache getCache() {
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
	public final void setDefaultLang(String lang) {
		this.defLang = lang;
	}

	/**
	 * Get default language
	 *
	 * @return language language code
	 */
	public final String getDefaultLang() {
		return defLang;
	}

	/**
	 * Set all languages
	 *
	 * @param langs array of language codes
	 */
	public final void setAllLangs(String langs[]) {
		this.allLangs = langs;
	}

	/**
	 * Get all languages
	 *
	 * @return array of language codes
	 */
	public final String[] getAllLangs() {
		return allLangs;
	}

	/**
	 * Set name
	 *
	 * @param name name
	 */
	public final void setName(String name) {
		this.name = name;
	}

	/**
	 * Get name
	 *
	 * @return name
	 */
	public final String getName() {
		return name;
	}

	/**
	 * Get lowercase file extension
	 *
	 * @param href
	 * @return file extension or empty string
	 */
	public final String getFileExt(String href) {
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
	 * Make a hashed ID based upon a string.
	 *
	 * @param s
	 * @return
	 */
	public final String makeHashId(String s) {
		return HASHER.hashBytes(s.getBytes()).toString();
	}

	/**
	 * Return an absolute URL
	 *
	 * @param rel relative URL
	 * @return
	 * @throws MalformedURLException
	 */
	public final URL makeAbsURL(String rel) throws MalformedURLException {
		// Check if URL is already absolute
		if (rel.startsWith("http:") || rel.startsWith("https:")) {
			return new URL(rel);
		}
		return new URL(getBase().getProtocol(), getBase().getHost(), rel);
	}

	/**
	 * Make an URL for a DCAT Catalog
	 *
	 * @return URL
	 * @throws MalformedURLException
	 */
	public URL makeCatalogURL() throws MalformedURLException {
		return new URL(DATAGOVBE.PREFIX_URI_CAT + "/" + getName());
	}

	/**
	 * Make an URL for a DCAT Dataset
	 *
	 * @param id
	 * @return URL
	 * @throws MalformedURLException
	 */
	public final URL makeDatasetURL(String id) throws MalformedURLException {
		return new URL(DATAGOVBE.PREFIX_URI_DATASET + "/" + getName() + "/"
			+ id.replace(".", "-").replace(":", "-"));
	}

	/**
	 * Make an URL for a DCAT Distribution
	 *
	 * @param id
	 * @return URL
	 * @throws java.net.MalformedURLException
	 */
	public final URL makeDistURL(String id) throws MalformedURLException {
		return new URL(DATAGOVBE.PREFIX_URI_DIST + "/" + getName() + "/"
			+ id.replace(".", "-").replace(":", "-"));
	}

	/**
	 * Make an URL for an organization
	 *
	 * @param id
	 * @return URL
	 * @throws java.net.MalformedURLException
	 */
	public final URL makeOrgURL(String id) throws MalformedURLException {
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
	public URL makeTemporalURL(String start, String end) throws MalformedURLException {
		String s[] = start.split("T");
		String e[] = end.split("T");

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
	public void generateTemporal(Storage store, IRI dataset, String str, Pattern p, String sep)
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
	public void generateTemporal(Storage store, IRI dataset, String start, String end)
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

	@Override
	public final void scrape(File cache) throws IOException {
		this.cache = new Cache(cache);
		scrape();
	}

	/**
	 * Fetch all metadata from repository / site
	 *
	 * @throws java.io.IOException
	 */	
	public abstract void scrape() throws IOException;

	/**
	 * Extra DCAT catalog info
	 *
	 * @param store RDF store
	 * @param catalog catalog URI
	 * @throws RepositoryException
	 */
	public void generateCatalogInfo(Storage store, IRI catalog)
		throws RepositoryException {
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
	public void generateCatalog(Storage store)
		throws RepositoryException, MalformedURLException {
		IRI catalog = store.getURI(makeCatalogURL().toString());
		store.add(catalog, RDF.TYPE, DCAT.CATALOG);

		List<IRI> uris = store.query(DCAT.DATASET);
		for (IRI u : uris) {
			store.add(catalog, DCAT.HAS_DATASET, u);
		}
		generateCatalogInfo(store, catalog);
	}

	/**
	 * Generate DCAT from cache and write it to the RDF store
	 *
	 * @param cache cache
	 * @param store RDF store
	 * @throws RepositoryException
	 * @throws MalformedURLException
	 */
	public abstract void generateDcat(Cache cache, Storage store)
		throws RepositoryException, MalformedURLException;


	@Override
	public void writeDcat(Writer out) throws RepositoryException, MalformedURLException {
		store.startup();

		generateDcat(cache, store);

		cache.shutdown();
		store.write(out);
		store.shutdown();
	}

	/**
	 * Get required property
	 *
	 * @param prop properties
	 * @param name unprefixed property
	 * @return value of the property
	 * @throws IOException if property is empty
	 */
	public final String getRequiredProperty(Properties prop, String name) throws IOException {
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
	public final String getProperty(Properties prop, String name) {
		String value = prop.getProperty(BaseScraper.PROP_PREFIX + "." + name);
		if (value == null) {
			logger.warn("No property {}", name);
		}
		return value;
	}

	/**
	 * Constructor
	 * 
	 * @param prop
	 * @throws IOException 
	 */
	protected BaseScraper(Properties prop) throws IOException {
		this.base = new URL(getRequiredProperty(prop, "url"));
		this.defLang = getRequiredProperty(prop, "deflanguage");
		this.allLangs = getRequiredProperty(prop, "languages").split(",");

		String delay = prop.getProperty(BaseScraper.PROP_PREFIX + ".http.delay", "500");
		setDelay(Integer.valueOf(delay));
	}
}
