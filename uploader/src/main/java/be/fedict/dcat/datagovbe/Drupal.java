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
package be.fedict.dcat.datagovbe;

import be.fedict.dcat.helpers.Storage;
import be.fedict.dcat.vocab.DATAGOVBE;
import be.fedict.dcat.vocab.MDR_LANG;
import be.fedict.dcat.vocab.SCHEMA;

import com.google.common.collect.ListMultimap;

import java.io.IOException;

import java.net.URL;

import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;

import java.text.ParseException;
import java.text.SimpleDateFormat;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.json.Json;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import javax.net.ssl.SSLContext;

import org.apache.http.Header;
import org.apache.http.HttpHeaders;
import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;
import org.apache.http.client.fluent.Executor;
import org.apache.http.client.fluent.Request;
import org.apache.http.conn.ssl.TrustSelfSignedStrategy;
import org.apache.http.entity.ContentType;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.client.LaxRedirectStrategy;
import org.apache.http.ssl.SSLContexts;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.util.Values;
import org.eclipse.rdf4j.model.vocabulary.DCAT;
import org.eclipse.rdf4j.model.vocabulary.DCTERMS;
import org.eclipse.rdf4j.model.vocabulary.FOAF;
import org.eclipse.rdf4j.model.vocabulary.VCARD4;
import org.eclipse.rdf4j.repository.RepositoryException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Drupal REST service class.
 *
 * @author Bart Hanssens <bart.hanssens@fedict.be>
 */
public class Drupal {

	private final static Logger logger = LoggerFactory.getLogger(Drupal.class);

	public final static String PROP_PREFIX = "be.fedict.datagovbe7";

	public final static String NODE = "/node";
	public final static String TOKEN = "/restws/session/token";
	public final static String X_TOKEN = "X-CSRF-Token";

	public final static String POST = "Post";
	public final static String PUT = "Put";

	// Drupal fields
	public final static String TITLE = "title";
	public final static String BODY = "body";
	public final static String LANGUAGE = "language";
	public final static String URL = "url";
	public final static String AUTHOR = "author";
	public final static String MODIFIED = "changed_date";
	public final static String FLD_CAT = "field_category";
	public final static String FLD_CONDITIONS = "field_conditions";
	public final static String FLD_DETAILS = "field_details";
	public final static String FLD_FORMAT = "field_file_type";
	public final static String FLD_FREQ = "field_frequency";
	public final static String FLD_GEO = "field_geo_coverage";
	public final static String FLD_ID = "field_id";
	public final static String FLD_KEYWORDS = "field_keywords";
	public final static String FLD_LICENSE = "field_license";
	public final static String FLD_LINKS = "field_links";
	public final static String FLD_MAIL = "field_contact";
	public final static String FLD_ORG = "field_organization";
	public final static String FLD_PUBLISHER = "field_publisher";
	public final static String FLD_UPSTAMP = "field_upstamp";
	public final static String FLD_TIME = "field_time";

	public final static String ID = "id";
	public final static String TYPE = "type";
	public final static String TYPE_DATA = "dataset";

	public final static String FORMAT = "format";
	public final static String FORMAT_HTML = "filtered_html";
	public final static String SOURCE = "source";
	public final static String SUMMARY = "summary";
	public final static String VALUE = "value";

	public final static String TAXO_PREFIX = "http://data.gov.be/en/taxonomy";

	public final static Pattern SHORTLINK
		= Pattern.compile("/([0-9]+)>; rel=\"shortlink\"");

	public final static SimpleDateFormat ISODATE = new SimpleDateFormat("yyyy-MM-dd");

	private final static Map<String,IRI> langMap = Map.of(
		"nl", Values.iri("http://publications.europa.eu/resource/authority/language/NLD"),
		"fr", Values.iri("http://publications.europa.eu/resource/authority/language/FRA"),
		"de", Values.iri("http://publications.europa.eu/resource/authority/language/DEU"),
		"en", Values.iri("http://publications.europa.eu/resource/authority/language/ENG")
	);

	public final static int LEN_TITLE = 128;
	public final static int LEN_KEYWORDS = 255;
	public final static int LEN_LINK = 255;

	private final String[] langs;
	private final URL url;
	private String userid;

	private Executor exec;
	private HttpHost proxy = null;
	private HttpHost host = null;
	private String token = null;
	private Storage store;

	/**
	 * Return shorter version of string, with trailing ellipsis (...)
	 *
	 * @param s string
	 * @param len maximum length
	 * @return
	 */
	private static String ellipsis(String s, int len) {
		int cut = s.lastIndexOf(' ', len - 3);
		if (cut < 0) {
			cut = 125;
		}
		return s.substring(0, cut) + "...";
	}

	/**
	 * Strip HTML tags from string
	 *
	 * @param s
	 * @return
	 */
	private static String stripTags(String s) {
		return s.replaceAll("<[bB][rR] ?/?>|</[pP]>|</[dD][iI][vV]>", "\n")
			.replaceAll("<[hH].?>|<[pP]>|<[dD][iI][vV]>", "").trim();
	}

	/**
	 * Prepare a POST or PUT action.
	 *
	 * @param method POST or PUT
	 * @param relpath relative path
	 * @return
	 */
	private Request prepare(String method, String relpath) {
		String u = this.url.toString() + relpath;

		Request r = method.equals(Drupal.POST) ? Request.Post(u) : Request.Put(u);

		r.setHeader(HttpHeaders.ACCEPT, ContentType.APPLICATION_JSON.getMimeType())
			.setHeader(Drupal.X_TOKEN, token);

		if (proxy != null) {
			r.viaProxy(proxy);
		}
		return r;
	}

	/**
	 * Create a JsonArrayBuilder with a list of Drupal URL fields
	 *
	 * @param l list of URLs (as string)
	 * @return JsonArrayBuilder containing URL field JSON objects
	 */
	private static JsonArrayBuilder urlArrayJson(Collection<String> l) {
		JsonArrayBuilder builder = Json.createArrayBuilder();
		for (String s : l) {
			builder.add(Json.createObjectBuilder().add(Drupal.URL, s));
		}
		return builder;
	}

	/**
	 * Create a JsonArrayBuilder with a list of Drupal text fields
	 *
	 * @param l list of URLs (as string)
	 * @return JsonArrayBuilder containing text fields JSON objects
	 */
	private static JsonArrayBuilder fieldArrayJson(List<String> l) {
		JsonArrayBuilder builder = Json.createArrayBuilder();
		for (String s : l) {
			builder.add(Json.createObjectBuilder().add(Drupal.VALUE, s));
		}
		return builder;
	}

	/**
	 * Get Drupal taxonomy terms as JSON Array
	 *
	 * @param terms
	 * @return JsonArrayBuilder
	 */
	private static JsonArrayBuilder arrayTermsJson(Collection<String> terms) {
		JsonArrayBuilder arr = Json.createArrayBuilder();

		for (String term : terms) {
			if (term.startsWith(Drupal.TAXO_PREFIX)) {
				String id = term.substring(term.lastIndexOf('/') + 1);
				arr.add(Json.createObjectBuilder().add(Drupal.ID, id).build());
			}
		}
		return arr;
	}

	/**
	 * Get Drupal taxonomy terms from RDF as JSON Array
	 *
	 * @param map
	 * @param property
	 * @return JsonArrayBuilder
	 */
	private static JsonArrayBuilder arrayTermsJson(
		Map<Resource, ListMultimap<String, String>> map, IRI property) {
		return arrayTermsJson(Storage.getMany(map, property, ""));
	}

	/**
	 * Check if a dataset / distribution is available in a certain language
	 *
	 * @param map
	 * @param lang
	 * @return boolean
	 */
	private static boolean hasLang(Map<Resource, ListMultimap<String, String>> map, String lang) {
		List<String> datalangs = Storage.getMany(map, DCTERMS.LANGUAGE, "");

		// If there is no language info, assume info is language-neutral
		if (datalangs.isEmpty()) {
			return true;
		}

		for (String datalang : datalangs) {
			if (MDR_LANG.MAP.get(lang).toString().equals(datalang)) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Check if dataset with ID already exists on drupal site.
	 *
	 * @param id
	 * @param lang
	 * @return
	 */
	private String checkExists(String id, String lang) {
		String node = "";

		String u = this.url.toString() + "/" + lang + "/" + Drupal.TYPE_DATA + "/" 
			+ id.replace(".", "-").replace(":", "-").replace(" ", "%20");

		Request r = Request.Head(u);
		if (proxy != null) {
			r.viaProxy(proxy);
		}

		try {
			HttpResponse resp = exec.execute(r).returnResponse();
			Header header = resp.getFirstHeader("Link");
			if (header != null) {
				Matcher m = SHORTLINK.matcher(header.getValue());
				if (m.find()) {
					node = m.group(1);
					logger.info("Dataset {} exists, node {}", id, node);
				}
			}
		} catch (IOException ex) {
			logger.error("Exception getting dataset {}", id);
		}

		return node;
	}

	/**
	 * Check if node or translations exist on Drupal site.
	 *
	 * @param builder
	 * @param id
	 * @param lang
	 * @return
	 */
	private String checkExistsTrans(JsonObjectBuilder builder, String id, String lang) {
		String node = checkExists(id, lang);

		// Exists in another language ?
		if (node.isEmpty()) {
			String lowlang = "";
			int lownode = Integer.MAX_VALUE;

			for (String otherlang : langs) {
				if (!otherlang.equals(lang)) {
					String othernode = checkExists(id, otherlang);
					if (!othernode.isEmpty()) {
						int n = Integer.parseInt(othernode);
						// Node with lowest ID is source
						if (n < lownode) {
							lownode = n;
							lowlang = otherlang;
						}
					}
				}
			}
			if (!lowlang.isEmpty()) {
				builder.add(Drupal.SOURCE,
					Json.createObjectBuilder().add(lowlang, lownode));
			}
		}
		return node;
	}

	/**
	 * Get contact contact address or contact form
	 *
	 * @param org organization
	 * @return contact address / contact form URL or empty string
	 * @throw RepositoryException
	 */
	private String getOrgContact(String org) throws RepositoryException {
		// Get DCAT contactpoints
		String str = "";
		Map<Resource, ListMultimap<String, String>> map
			= store.queryProperties(store.getURI(org));
		String contact = Storage.getOne(map, VCARD4.HAS_EMAIL, "");
		if (contact.startsWith("mailto:")) {
			str = contact.substring(7);
		}
		if (str.isEmpty()) {
			str = Storage.getOne(map, VCARD4.HAS_URL, "");
		}
		return str;
	}

	/**
	 * Get contact contact address
	 *
	 * @param org organization
	 * @param language code
	 * @return organisation name or empty string
	 * @throw RepositoryException
	 */
	private String getOrgName(String org, String lang) throws RepositoryException {
		// Get DCAT contactpoints
		Map<Resource, ListMultimap<String, String>> map
			= store.queryProperties(store.getURI(org));
		String name = Storage.getOne(map, VCARD4.FN, lang);
		if (name.isEmpty()) {
			name = Storage.getOne(map, VCARD4.HAS_FN, lang);
		}
		if (name.isEmpty()) {
			// check undefined language
			name = Storage.getOne(map, VCARD4.FN, "");
		}
		if (name.isEmpty()) {
			// check undefined language
			name = Storage.getOne(map, VCARD4.HAS_FN, "");
		}
		if (name.isEmpty()) {
			name = Storage.getOne(map, FOAF.NAME, lang);
		}
		if (name.isEmpty()) {
			name = Storage.getOne(map, FOAF.NAME, "");
		}
		return name;
	}

	/**
	 * Get modification date
	 *
	 * @param dataset
	 * @return
	 */
	private Date getModif(Map<Resource, ListMultimap<String, String>> dataset) {
		Date modif = new Date();
		String m = Storage.getOne(dataset, DCTERMS.MODIFIED, "");
		if (m.isEmpty()) {
			m = Storage.getOne(dataset, DCTERMS.ISSUED, "");
		}
		if (!m.isEmpty() && (m.length() >= 10)) {
			try {
				modif = Drupal.ISODATE.parse(m.substring(0, 10));
			} catch (ParseException ex) {
				logger.error("Exception parsing {} as date", m);
			}
		}
		return modif;
	}

	/**
	 * Get list of contact contact addresses
	 *
	 * @param dataset
	 * @return list of contact addresses
	 * @throws RepositoryException
	 */
	private List<String> getDatasetMails(Map<Resource, ListMultimap<String, String>> dataset)
		throws RepositoryException {
		ArrayList<String> arr = new ArrayList<>();
		List<String> orgs = Storage.getMany(dataset, DCAT.CONTACT_POINT, "");
		for (String org : orgs) {
			String email = getOrgContact(org);
			if (!email.isEmpty() && !arr.contains(email)) {
				arr.add(email);
			}
		}
		return arr;
	}

	/**
	 * Get list of organization names
	 *
	 * @param dataset
	 * @param lang language code
	 * @return list of contact addresses
	 * @throws RepositoryException
	 */
	private List<String> getDatasetOrgs(Map<Resource, ListMultimap<String, String>> dataset,
		String lang) throws RepositoryException {
		ArrayList<String> arr = new ArrayList<>();
		
		List<String> orgs = Storage.getMany(dataset, DCTERMS.CREATOR, "");
		if (orgs == null || orgs.isEmpty()) {
			orgs = Storage.getMany(dataset, DCAT.CONTACT_POINT, "");
		}
		
		for (String org : orgs) {
			String name = getOrgName(org, lang);
			if (!name.isEmpty() && !arr.contains(name)) {
				arr.add(name);
			}
		}
		return arr;
	}

	/**
	 * Get a list of keywords
	 *
	 * @param dataset
	 * @param lang language
	 * @return comma-separatedlist of keywords
	 * @throws RepositoryException
	 */
	private String getKeywords(Map<Resource, ListMultimap<String, String>> dataset,
		String lang) throws RepositoryException {
		StringBuilder b = new StringBuilder();

		List<String> words = Storage.getMany(dataset, DCAT.KEYWORD, lang);
		for (String word : words) {
			if (!word.isEmpty()) {
				b.append(word.trim()).append(", ");
			}
		}
		return (b.length() > 2) ? b.substring(0, b.length() - 2) : "";
	}

	/**
	 * Get publisher
	 *
	 * @param dataset
	 * @return
	 * @throws RepositoryException
	 */
	private Map<Resource, ListMultimap<String, String>>
		getPublisher(Map<Resource, ListMultimap<String, String>> dataset)
		throws RepositoryException {
		Map<Resource, ListMultimap<String, String>> m = new HashMap<>();

		String publ = Storage.getOne(dataset, DCTERMS.PUBLISHER, "");
		if (!publ.isEmpty()) {
			m = store.queryProperties(store.getURI(publ));
		}
		return m;
	}

	/**
	 * Get temporal coverage as string
	 *
	 * @param dataset
	 * @return string
	 * @throws RepositoryException
	 */
	private String getTime(Map<Resource, ListMultimap<String, String>> dataset)
		throws RepositoryException {
		Map<Resource, ListMultimap<String, String>> m;

		String time = Storage.getOne(dataset, DCTERMS.TEMPORAL, "");
		if (!time.isEmpty()) {
			m = store.queryProperties(store.getURI(time));
			String start = Storage.getOne(m, SCHEMA.START_DATE, "");
			if (start.isEmpty()) {
				start = Storage.getOne(m, DCAT.START_DATE, "");
			}
			String end = Storage.getOne(m, SCHEMA.END_DATE, "");
			if (end.isEmpty()) {
				end = Storage.getOne(m, DCAT.END_DATE, "");
			}
			time = start + " / " + end;
		}
		return time;
	}

	/**
	 * Add a dataset to Drupal form
	 *
	 * @param builder
	 * @param dataset
	 * @param lang
	 * @throws RepositoryException
	 */
	private void addDataset(JsonObjectBuilder builder,
		Map<Resource, ListMultimap<String, String>> dataset, String lang)
		throws RepositoryException {
		String id = Storage.getOne(dataset, DCTERMS.IDENTIFIER, "");
		String title = stripTags(Storage.getOne(dataset, DCTERMS.TITLE, lang));
		if (title.isEmpty()) {
			logger.warn("Title for {} empty", id);
		}
		// Just copy the title if description is empty
		String desc = Storage.getOne(dataset, DCTERMS.DESCRIPTION, lang);
		desc = (desc.isEmpty()) ? title : stripTags(desc);

		// Max size for Drupal title
		if (title.length() > Drupal.LEN_TITLE) {
			logger.warn("Title {} for {} too long", title, id);
			title = ellipsis(title, Drupal.LEN_TITLE);
		}

		Date modif = getModif(dataset);

		String keywords = getKeywords(dataset, lang);
		// Max size for Drupal keywords
		if (keywords.length() > Drupal.LEN_KEYWORDS) {
			logger.warn("Keywords {} too long", keywords);
			keywords = ellipsis(keywords, Drupal.LEN_KEYWORDS);
		}

		Map<Resource, ListMultimap<String, String>> publ = getPublisher(dataset);
		JsonArrayBuilder emails = fieldArrayJson(getDatasetMails(dataset));
		JsonArrayBuilder orgs = fieldArrayJson(getDatasetOrgs(dataset, lang));

		builder.add(Drupal.TYPE, Drupal.TYPE_DATA)
			.add(Drupal.LANGUAGE, lang)
			.add(Drupal.AUTHOR, Json.createObjectBuilder().add(Drupal.ID, userid))
			.add(Drupal.TITLE, title)
			.add(Drupal.BODY, Json.createObjectBuilder()
				.add(Drupal.VALUE, desc)
				.add(Drupal.SUMMARY, "")
				.add(Drupal.FORMAT, Drupal.FORMAT_HTML))
			.add(Drupal.FLD_UPSTAMP, modif.getTime() / 1000L)
			.add(Drupal.FLD_FREQ, arrayTermsJson(dataset, DATAGOVBE.FREQ))
			.add(Drupal.FLD_CAT, arrayTermsJson(dataset, DATAGOVBE.THEME))
			.add(Drupal.FLD_GEO, arrayTermsJson(dataset, DATAGOVBE.SPATIAL))
			.add(Drupal.FLD_PUBLISHER, arrayTermsJson(publ, DATAGOVBE.ORG))
			.add(Drupal.FLD_ORG, orgs)
			.add(Drupal.FLD_MAIL, emails)
			.add(Drupal.FLD_KEYWORDS, keywords)
			.add(Drupal.FLD_ID, id.replace(":", "-").replace(".", "-"));

		String fromtill = getTime(dataset);
		if (fromtill.isEmpty()) {
			builder.addNull(Drupal.FLD_TIME);
		} else {
			builder.add(Drupal.FLD_TIME, fromtill);
		}
	}

	/**
	 * Get download link
	 *
	 * @param dist
	 * @param property
	 * @param store
	 * @param lang
	 * @return
	 */
	private static String getLink(Map<Resource, ListMultimap<String, String>> dist, IRI property, 
									Storage store, String lang) {
		String l = Storage.getOne(dist, property, "");
		// don't add empty links or generated "well-known" skolemized links
		if (l.isEmpty() || l.contains(".well-known/genid")) {
			return "";
		}
		Map<Resource, ListMultimap<String, String>> linkmap = store.queryProperties(store.getURI(l));
		List<String> langs = Storage.getMany(linkmap, DCTERMS.LANGUAGE, "");
					
		String link = l.replace(" ", "%20");
		if (link.length() > Drupal.LEN_LINK) {
			logger.warn("Download URL too long ({}): {} ", l.length(), l);
		}
		return link;
	}

	/**
	 * Add DCAT datasets
	 *
	 * @param builder
	 * @param uris
	 * @param lang
	 * @param landings
	 * @throws RepositoryException
	 */
	private void addDists(JsonObjectBuilder builder, List<String> uris, String lang, List<String> landings)
		throws RepositoryException {
		HashSet<String> accesses = new HashSet<>();
		HashSet<String> downloads = new HashSet<>();
		HashSet<String> rights = new HashSet<>();
		HashSet<String> types = new HashSet<>();

		if (!landings.isEmpty()) {
			accesses.addAll(landings);
		}

		for (String uri : uris) {
			Map<Resource, ListMultimap<String, String>> dist
				= store.queryProperties(store.getURI(uri));
			if (hasLang(dist, lang) || hasLang(dist, "en")) {
				// Data.gov.be displays this information as fields on dataset
				// not on distribution.
				if (landings.isEmpty()) {
					accesses.add(getLink(dist, DCAT.ACCESS_URL, store, lang));
				}
				downloads.add(getLink(dist, DCAT.DOWNLOAD_URL, store, lang));
				rights.add(getLink(dist, DCTERMS.RIGHTS, store, lang));
				types.add(Storage.getOne(dist, DATAGOVBE.MEDIA_TYPE, ""));

				builder.add(Drupal.FLD_LICENSE, arrayTermsJson(dist, DATAGOVBE.LICENSE));
			} else {
				logger.warn("No download or other info for {}", uri);
			}
		}

		// remove duplicate and empty links
		downloads.removeAll(accesses);
		downloads.remove("");

		rights.removeAll(accesses);
		rights.remove("");

		types.remove("");
		accesses.remove("");

		builder.add(Drupal.FLD_DETAILS, urlArrayJson(accesses));

		if (!types.isEmpty()) {
			builder.add(Drupal.FLD_FORMAT, arrayTermsJson(types));
		}
		if (!downloads.isEmpty()) {
			builder.add(Drupal.FLD_LINKS, urlArrayJson(downloads));
		}
		if (!rights.isEmpty()) {
			builder.add(Drupal.FLD_CONDITIONS, urlArrayJson(rights));
		}
	}

	/**
	 * Add a dataset to the Drupal website.
	 *
	 * @param uri identifier of the dataset
	 * @throws RepositoryException
	 */
	private void add(IRI uri) throws RepositoryException {
		Map<Resource, ListMultimap<String, String>> dataset = store.queryProperties(uri);

		if (dataset.isEmpty()) {
			logger.warn("Empty dataset for {}", uri.stringValue());
			return;
		}

		for (String lang : langs) {
			if (!hasLang(dataset, lang)) {
				continue;
			}

			JsonObjectBuilder builder = Json.createObjectBuilder();

			addDataset(builder, dataset, lang);

			List<String> landingPages = Storage.getMany(dataset, DCAT.LANDING_PAGE, "");

			// Get DCAT distributions
			List<String> dists = Storage.getMany(dataset, DCAT.HAS_DISTRIBUTION, "");
			addDists(builder, dists, lang, landingPages);

			// Add new or update existing dataset ?
			String id = Storage.getOne(dataset, DCTERMS.IDENTIFIER, "");
			String node = checkExistsTrans(builder, id, lang);

			// Build the JSON array
			JsonObject obj = builder.build();
			logger.debug(obj.toString());

			Request r = node.isEmpty()
				? prepare(Drupal.POST, Drupal.NODE)
				: prepare(Drupal.PUT, Drupal.NODE + "/" + node);
			r.bodyString(obj.toString(), ContentType.APPLICATION_JSON);

			try {
				StatusLine status = exec.authPreemptive(host).execute(r)
					.returnResponse().getStatusLine();
				logger.info(status.toString());
			} catch (IOException ex) {
				logger.error("Could not update {}", uri.toString(), ex);
			}
		}
	}

	/**
	 * Set username and password.
	 *
	 * @param user username
	 * @param password password
	 * @param userid drupal user ID
	 */
	public void setUserPassID(String user, String password, String userid) {
		exec = exec.clearAuth().clearCookies()
			.auth(host, user, password);
		this.userid = userid;
	}

	/**
	 * Set HTTP proxy.
	 *
	 * @param proxy proxy server
	 * @param port proxy port
	 */
	public void setProxy(String proxy, int port) {
		if (proxy == null || proxy.isEmpty()) {
			this.proxy = null;
		} else {
			this.proxy = new HttpHost(proxy, port);
		}
	}

	/**
	 * Get CSRF Token
	 *
	 * @throws IOException
	 */
	private void getCSRFToken() throws IOException {
		Request r = Request.Get(this.url.toString() + Drupal.TOKEN)
			.setHeader(HttpHeaders.CACHE_CONTROL, "no-cache");

		if (proxy != null) {
			r.viaProxy(proxy);
		}
		token = exec.authPreemptive(host)
			.execute(r)
			.returnContent().asString();
		logger.debug("CSRF Token is {}", token);
	}

	/**
	 * Update site
	 *
	 * @param skip skip number of datasets
	 * @throws IOException
	 * @throws RepositoryException
	 */
	public void update(int skip) throws IOException, RepositoryException {
		List<IRI> datasets = store.query(DCAT.DATASET);

		logger.info("Updating {} datasets...", Integer.toString(datasets.size()));

		getCSRFToken();

		int i = 0;
		for (IRI d : datasets) {
			if (i >= skip) {
				add(d);
			}
			if (++i % 100 == 0) {
				logger.info("Updated {}", Integer.toString(i));
			}
		}
		logger.info("Done updating datasets");
	}

	/**
	 * Drupal REST Service.
	 *
	 * @param url service endpoint
	 * @param langs languages
	 * @param store
	 */
	public Drupal(URL url, String[] langs, Storage store) {
		this.url = url;
		this.langs = langs;
		this.store = store;
		this.host = new HttpHost(url.getHost(), -1, url.getProtocol());

		Executor e = null;
		try {
			/* Self signed certificates are OK */
			SSLContext ctx = SSLContexts.custom()
				.loadTrustMaterial(new TrustSelfSignedStrategy()).build();

			/* Allow redirect on POST */
			CloseableHttpClient client = HttpClients.custom()
				.setRedirectStrategy(new LaxRedirectStrategy())
				.setSSLContext(ctx)
				.disableContentCompression()
				.build();
			e = Executor.newInstance(client);
		} catch (NoSuchAlgorithmException ex) {
			logger.error("Algo error", ex);
		} catch (KeyStoreException | KeyManagementException ex) {
			logger.error("Store exception", ex);
		}
		this.exec = e;
	}
}
