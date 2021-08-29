/*
 * Copyright (c) 2018, FPS BOSA DG DT
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
import be.fedict.dcat.vocab.MDR_LANG;

import java.io.IOException;
import java.io.StringReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.DocumentFactory;
import org.dom4j.Node;
import org.dom4j.io.SAXReader;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.vocabulary.DCAT;
import org.eclipse.rdf4j.model.vocabulary.DCTERMS;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.model.vocabulary.VCARD4;
import org.eclipse.rdf4j.repository.RepositoryException;

/**
 * Abstract scraper for the GeonetRDF v3 portal software with DCAT export.
 *
 * @see http://geonetwork-opensource.org/
 *
 * @author Bart Hanssens <bart.hanssens@fedict.be>
 */
public abstract class GeonetGmd extends Geonet {

	public final static String GMD = "http://www.isotc211.org/2005/gmd";
	public final static String API = "/eng/csw?service=CSW&version=2.0.2";
	public final static int MAX_RECORDS = 200;
	public final static String API_RECORDS = API
		+ "&request=GetRecords&resultType=results"
		+ "&outputSchema=" + GMD
		+ "&elementSetName=full&typeNames=gmd:MD_Metadata"
		+ "&maxRecords=" + MAX_RECORDS;
	public final static String POSITION = "&startPosition=";

	private final static SAXReader sax;
	private final static Map<String, String> NS = new HashMap<>();

	static {
		NS.put("csw", "http://www.opengis.net/cat/csw/2.0.2");
		NS.put("gmd", "http://www.isotc211.org/2005/gmd");
		NS.put("gmx", "http://www.isotc211.org/2005/gmx");
		NS.put("gml", "http://www.opengis.net/gml");
		NS.put("gco", "http://www.isotc211.org/2005/gco");
		NS.put("srv", "http://www.isotc211.org/2005/srv");
		NS.put("xlink", "http://www.w3.org/1999/xlink");

		DocumentFactory factory = DocumentFactory.getInstance();
		factory.setXPathNamespaceURIs(NS);
		sax = new SAXReader();
		sax.setDocumentFactory(factory);
	}

	public final static String NUM_REC = "csw:GetRecordsResponse/csw:SearchResults/@numberOfRecordsMatched";

	public final static String XP_DATASETS = "//gmd:MD_Metadata";

	public final static String XP_ID = "gmd:fileIdentifier/gco:CharacterString";
	public final static String XP_TSTAMP = "gmd:dateStamp/gco:DateTime";
	public final static String XP_META = "gmd:identificationInfo/gmd:MD_DataIdentification";
	public final static String XP_KEYWORDS = "gmd:descriptiveKeywords/gmd:MD_Keywords/gmd:keyword";
	public final static String XP_LICENSE = "gmd:resourceConstraints/gmd:MD_LegalConstraints/gmd:otherConstraints";
	public final static String XP_LICENSE2 = "gmd:resourceConstraints/gmd:MD_Constraints/gmd:useLimitation";

	public final static String XP_TYPE = "gmd:hierarchyLevel/gmd:MD_ScopeCode/@codeListValue";

	public final static String XP_TITLE = "gmd:citation/gmd:CI_Citation/gmd:title";
	public final static String XP_DESC = "gmd:abstract";
	public final static String XP_PURP = "gmd:purpose";
	public final static String XP_ANCHOR = "gmx:Anchor";
	public final static String XP_CHAR = "gco:CharacterString";

	public final static String XP_STR = "gco:CharacterString";
	public final static String XP_STRLNG = "gmd:PT_FreeText/gmd:textGroup/gmd:LocalisedCharacterString";

	public final static String XP_CONTACT = "gmd:contact/gmd:CI_ResponsibleParty";
	public final static String XP_ORG_NAME = "gmd:organisationName";
	public final static String XP_INDIVIDUAL = "gmd:individualName";
	public final static String XP_EMAIL = "gmd:contactInfo/gmd:CI_Contact/gmd:address/gmd:CI_Address/gmd:electronicMailAddress/gco:CharacterString";

	public final static String XP_TEMPORAL = "gmd:extent/gmd:EX_Extent/gmd:temporalElement";
	public final static String XP_TEMP_EXT = "gmd:EX_TemporalExtent/gmd:extent/gml:TimePeriod/";
	public final static String XP_TEMP_BEGIN = XP_TEMP_EXT + "gml:beginPosition";
	public final static String XP_TEMP_END = XP_TEMP_EXT + "gml:endPosition";

	public final static String XP_QUAL = "gmd:dataQualityInfo/gmd:DQ_DataQuality";
	public final static String XP_QUAL_LIN = XP_QUAL + "/gmd:lineage/gmd:LI_Lineage/gmd:statement";
	public final static String XP_QUAL_TYPE = XP_QUAL + "/gmd:scope/gmd:DQ_Scope/gmd:level/gmd:MD_ScopeCode/@codeListValue";

	public final static String XP_DISTS = "gmd:distributionInfo/gmd:MD_Distribution";
	public final static String XP_TRANSF = "/gmd:transferOptions/gmd:MD_DigitalTransferOptions/gmd:onLine/gmd:CI_OnlineResource";

	public final static String XP_DISTS2 = "gmd:distributionInfo/gmd:MD_Distribution/gmd:distributor/gmd:MD_Distributor";
	public final static String XP_TRANSF2 = "/*/gmd:MD_DigitalTransferOptions/gmd:onLine/gmd:CI_OnlineResource";

	public final static String XP_FMT = "gmd:distributionFormat/gmd:MD_Format/gmd:name/*";
	public final static String XP_FMT2 = "../../../../*/gmd:MD_Format/gmd:name/gco:CharacterString";
	public final static String XP_MIME = "gmd:name/gmx:MimeFileType";

	public final static String XP_PROTO = "gmd:protocol/gco:CharacterString";

	public final static String XP_DIST_URL = "gmd:linkage/gmd:URL";
	public final static String XP_DIST_NAME = "gmd:name";
	public final static String XP_DIST_DESC = "gmd:description";

	public final static String XP_MAINT = "gmd:resourceMaintenance/gmd:MD_MaintenanceInformation";
	public final static String XP_FREQ = XP_MAINT + "/gmd:maintenanceAndUpdateFrequency/gmd:MD_MaintenanceFrequencyCode/@codeListValue";

	public final static String SERV_TYPE = "srv:serviceType/gco:LocalName";

	public final static String INSPIRE_TYPE = "http://inspire.ec.europa.eu/metadatacodelist/ResourceType/";

	public final static DateFormat DATEFMT = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");

	/**
	 * Parse a temporal and store it in the RDF store.
	 *
	 * @param store RDF store
	 * @param uri RDF subject URI
	 * @param node
	 * @throws RepositoryException
	 * @throws MalformedURLException
	 */
	protected void parseTemporal(Storage store, IRI uri, Node node)
		throws RepositoryException, MalformedURLException {
		String start = node.valueOf(XP_TEMP_BEGIN);
		String end = node.valueOf(XP_TEMP_END);

		generateTemporal(store, uri, start, end);
	}

	/**
	 * Parse a contact and store it in the RDF store
	 *
	 * @param store RDF store
	 * @param uri RDF subject URI
	 * @param contact contact DOM node
	 * @throws RepositoryException
	 */
	protected void parseContact(Storage store, IRI uri, Node contact) throws RepositoryException {
		String v = "";

		Node name = contact.selectSingleNode(XP_ORG_NAME);
		Node name2 = contact.selectSingleNode(XP_INDIVIDUAL);

		String email = contact.valueOf(XP_EMAIL);

		try {
			v = makeOrgURL(makeHashId(name + email)).toString();
		} catch (MalformedURLException e) {
			logger.error("Could not generate hash url", e);
		}

		if (name != null || name2 != null || !email.isEmpty()) {
			IRI vcard = store.getURI(v);
			store.add(uri, DCAT.CONTACT_POINT, vcard);
			store.add(vcard, RDF.TYPE, VCARD4.ORGANIZATION);

			boolean found = false;
			for (String lang : getAllLangs()) {
				found |= parseMulti(store, vcard, name, VCARD4.FN, lang);
			}
			if (!found) {
				for (String lang : getAllLangs()) {
					parseMulti(store, vcard, name2, VCARD4.FN, lang);
				}
			}
			if (!email.isEmpty()) {
				store.add(vcard, VCARD4.HAS_EMAIL, store.getURI("mailto:" + email));
			}
		}
	}

	/**
	 * Map language code to geonet language string
	 *
	 * @param lang
	 * @return
	 */
	protected String mapLanguage(String lang) {
		return lang.toUpperCase();
	}

	/**
	 * Parse and store multilingual string
	 *
	 * @param store RDF store
	 * @param uri IRI of the dataset
	 * @param node multilingual DOM node
	 * @param property RDF property
	 * @param lang language code
	 * @return boolean
	 * @throws RepositoryException
	 */
	protected boolean parseMulti(Storage store, IRI uri, Node node, IRI property, String lang)
		throws RepositoryException {
		if (node == null) {
			return false;
		}
		String txten = node.valueOf(XP_STR);
		String txt = node.valueOf(XP_STRLNG + "[@locale='#" + mapLanguage(lang) + "']");

		if (txt == null || txt.isEmpty()) {
			store.add(uri, property, txten, "en");
			return false;
		}
		store.add(uri, property, txt, lang);
		return true;
	}

	/**
	 * Generate distribution
	 *
	 * @param store
	 * @param dataset
	 * @param node
	 * @param format
	 * @param license
	 * @throws MalformedURLException
	 */
	protected void generateDist(Storage store, IRI dataset, Node node, String format, String license)
		throws MalformedURLException {
		String url = node.valueOf(XP_DIST_URL);
		if (url == null || url.isEmpty() || url.equals("undefined")) {
			logger.warn("No url for distribution");
			return;
		}

		String id = makeHashId(dataset.toString()) + "/" + makeHashId(url);
		IRI dist = store.getURI(makeDistURL(id).toString());
		logger.debug("Generating distribution {}", dist.toString());

		store.add(dataset, DCAT.HAS_DISTRIBUTION, dist);
		store.add(dist, RDF.TYPE, DCAT.DISTRIBUTION);
		if (format != null) {
			store.add(dist, DCTERMS.FORMAT, format);
		}
		if (license != null) {
			store.add(dist, DCTERMS.LICENSE, license);
		}
		try {
			IRI iri = store.getURI(url);
			store.add(dist, DCAT.DOWNLOAD_URL, store.getURI(url));
		} catch (IllegalArgumentException e) {
			logger.debug("Cannot create download URL for {}", url);
		}

		Node title = node.selectSingleNode(XP_DIST_NAME);
		Node desc = node.selectSingleNode(XP_DIST_DESC);

		for (String lang : getAllLangs()) {
			parseMulti(store, dist, title, DCTERMS.TITLE, lang);
			parseMulti(store, dist, desc, DCTERMS.DESCRIPTION, lang);
		}
	}

	/**
	 * Generate DCAT dataset
	 *
	 * @param dataset
	 * @param id
	 * @param store
	 * @param node
	 * @throws MalformedURLException
	 */
	protected void generateDataset(IRI dataset, String id, Storage store, Node node)
		throws MalformedURLException {
		logger.info("Generating dataset {}", dataset.toString());

		Node metadata = node.selectSingleNode(XP_META);
		if (metadata == null) {
			logger.warn("No metadata for {}", id);
			return;
		}

		// try to filter out non-datasets
		String dtype = node.valueOf(XP_QUAL_TYPE);
		if (dtype == null || dtype.isEmpty()) {
			dtype = metadata.valueOf(XP_TYPE);
		}
		if (dtype != null && !dtype.isEmpty() && !dtype.equals("dataset")) {
			logger.warn("Not a dataset: {} is {}", id, dtype);
			return;
		}

		store.add(dataset, DCTERMS.TYPE, store.getURI(INSPIRE_TYPE + "dataset"));
		store.add(dataset, RDF.TYPE, DCAT.DATASET);
		store.add(dataset, DCTERMS.IDENTIFIER, id);

		String date = node.valueOf(XP_TSTAMP);
		if (date != null && !date.isEmpty()) {
			try {
				store.add(dataset, DCTERMS.MODIFIED, DATEFMT.parse(date));
			} catch (ParseException ex) {
				logger.warn("Could not parse date {}", date, ex);
			}
		}

		List<Node> keywords = metadata.selectNodes(XP_KEYWORDS);

		Node title = metadata.selectSingleNode(XP_TITLE);
		Node desc = metadata.selectSingleNode(XP_DESC);

		boolean hasTitle = false;
		for (String lang : getAllLangs()) {
			if (parseMulti(store, dataset, title, DCTERMS.TITLE, lang)) {
				store.add(dataset, DCTERMS.LANGUAGE, MDR_LANG.MAP.get(lang));
			}
			parseMulti(store, dataset, desc, DCTERMS.DESCRIPTION, lang);
			for (Node keyword : keywords) {
				parseMulti(store, dataset, keyword, DCAT.KEYWORD, lang);
			}
		}

		Node range = metadata.selectSingleNode(XP_TEMPORAL);
		if (range != null) {
			parseTemporal(store, dataset, range);
		}

		String freq = metadata.valueOf(XP_FREQ);
		if (freq != null) {
			store.add(dataset, DCTERMS.ACCRUAL_PERIODICITY, freq);
		}

		Node contact = node.selectSingleNode(XP_CONTACT);
		if (contact != null) {
			parseContact(store, dataset, contact);
		}

		// Distributions can be defined on several (hierarchical) levels
		List<Node> dists = node.selectNodes(XP_DISTS2 + XP_TRANSF2);
		// check higher level if no distributions could be found
		if (dists == null || dists.isEmpty()) {
			logger.warn("Checking for dists on higher level");
			dists = node.selectNodes(XP_DISTS + XP_TRANSF);
		}

		if (dists == null || dists.isEmpty()) {
			logger.warn("No dists for {}", id);
			return;
		}

		List<Node> licenses = metadata.selectNodes(XP_LICENSE);
		List<Node> licenses2 = metadata.selectNodes(XP_LICENSE2);
		if (licenses.isEmpty()) {
			licenses = licenses2;
		} else {
			if (!licenses2.isEmpty()) {
				licenses.addAll(licenses2);
			}
		}

		String license = null;
		for (Node n : licenses) {
			String anchor = n.valueOf(XP_ANCHOR);
			if (anchor != null && !anchor.isEmpty()) {
				license = anchor;
				break;
			}
			if (license == null || !license.isEmpty()) {
				license = n.valueOf(XP_CHAR);
			}
		}

		for (Node dist : dists) {
			// check proto first, in case of a OGC service
			Node fmt = dist.selectSingleNode(XP_PROTO);
			if (fmt == null || fmt.getText().startsWith("http") || fmt.getText().startsWith("WWW")) {
				fmt = dist.selectSingleNode(XP_MIME);
				if (fmt == null) {
					fmt = dist.selectSingleNode(XP_FMT2);
				}
			}
			String str = null;
			if (fmt != null) {
				str = fmt.getText();
			}
			generateDist(store, dataset, dist, str, license);
		}
	}

	/**
	 * Generate DCAT file
	 *
	 * @param cache
	 * @param store
	 * @throws RepositoryException
	 * @throws MalformedURLException
	 */
	@Override
	public void generateDcat(Cache cache, Storage store)
		throws RepositoryException, MalformedURLException {
		Set<URL> urls = cache.retrievePageList();

		SAXReader sax = new SAXReader();

		try {
			for (URL url : urls) {
				System.err.println(url);
				Map<String, Page> map = cache.retrievePage(url);
				String xml = map.get("all").getContent();
				Document doc = sax.read(new StringReader(xml));

				List<Node> datasets = doc.selectNodes(XP_DATASETS);
				for (Node dataset : datasets) {
					String id = dataset.valueOf(XP_ID);
					IRI iri = store.getURI(makeDatasetURL(id).toString());
					generateDataset(iri, id, store, dataset);
				}
			}
		} catch (DocumentException ex) {
			logger.error("Error parsing XML");
			throw new RepositoryException(ex);
		}

		generateCatalog(store);
	}

	/**
	 * Get total number of results from XML response
	 *
	 * @param doc
	 * @return number of records
	 */
	private int getNumRecords(Document doc) {
		Node rec = doc.selectSingleNode(NUM_REC);
		if (rec != null) {
			String n = rec.getText();
			logger.info(n + " records found");
			return Integer.valueOf(n);
		}
		return MAX_RECORDS;
	}

	/**
	 * Scrape DCAT catalog.
	 *
	 * @param cache
	 * @throws IOException
	 */
	protected void scrapeCat(Cache cache) throws IOException {
		try {
			for (int pos = 1, recs = MAX_RECORDS; pos < recs; pos += MAX_RECORDS) {
				URL url = new URL(getBase() + API_RECORDS + POSITION + pos);
				String xml = makeRequest(url);

				Document doc = sax.read(new StringReader(xml));
				if (pos == 1) {
					recs = getNumRecords(doc);
				}
				cache.storePage(url, "all", new Page(url, xml));
			}
		} catch (DocumentException ex) {
			logger.error("Error parsing XML");
			throw new RepositoryException(ex);
		}
	}

	/**
	 * Scrape DCAT catalog.
	 *
	 * @throws IOException
	 */
	@Override
	public void scrape() throws IOException {
		logger.info("Start scraping");
		Cache cache = getCache();

		Set<URL> urls = cache.retrievePageList();
		if (urls.isEmpty()) {
			scrapeCat(cache);
		}
		logger.info("Done scraping");
	}

	/**
	 * Constructor
	 *
	 * @param prop
	 * @throws java.io.IOException
	 */
	protected GeonetGmd(Properties prop) throws IOException {
		super(prop);
	}
}
