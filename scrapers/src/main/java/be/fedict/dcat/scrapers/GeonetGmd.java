/*
 * Copyright (c) 2018, Bart Hanssens <bart.hanssens@fedict.be>
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

import java.io.File;
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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * Abstract scraper for the GeonetRDF v3 portal software with DCAT export.
 *
 * @see http://geonetwork-opensource.org/
 *
 * @author Bart Hanssens <bart.hanssens@fedict.be>
 */
public abstract class GeonetGmd extends Geonet {

	private final Logger logger = LoggerFactory.getLogger(GeonetGmd.class);
	
	public final static String GMD = "http://www.isotc211.org/2005/gmd";
	public final static String API = "/eng/csw?service=CSW&version=2.0.2";
	public final static String API_RECORDS = API
			+ "&request=GetRecords&resultType=results"
			+ "&outputSchema=" + GMD
			+ "&elementSetName=full&typeNames=gmd:MD_Metadata"
			+ "&maxRecords=150";
	public final static String OFFSET = "startPosition";

	private final static Map<String,String> NS = new HashMap<>();
	static {
		NS.put("gmd", "http://www.isotc211.org/2005/gmd");
		NS.put("gml", "http://www.opengis.net/gml");
		NS.put("gco", "http://www.isotc211.org/2005/gco");
		NS.put("srv", "http://www.isotc211.org/2005/srv");
	}
		
	public final static String XP_DATASETS = "//gmd:MD_Metadata";
	
	public final static String XP_ID = "gmd:fileIdentifier/gco:CharacterString";
	public final static String XP_TSTAMP = "gmd:dateStamp/gco:DateTime";
	public final static String XP_META = "gmd:identificationInfo/gmd:MD_DataIdentification";
	public final static String XP_KEYWORDS = "gmd:descriptiveKeywords/gmd:MD_Keywords/gmd:keyword";
	
	public final static String XP_TITLE = "gmd:citation/gmd:CI_Citation/gmd:title";
	public final static String XP_DESC = "gmd:abstract";
	public final static String XP_PURP = "gmd:purpose";
	
	public final static String XP_STR = "gco:CharacterString";
	public final static String XP_STRLNG = "gmd:PT_FreeText/gmd:textGroup/gmd:LocalisedCharacterString";
	
	public final static String XP_CONTACT = "gmd:contact/gmd:CI_ResponsibleParty";
	public final static String XP_ORG_NAME = "gmd:organisationName/gco:CharacterString";
	public final static String XP_EMAIL = "gmd:contactInfo/gmd:CI_Contact/gmd:address/gmd:CI_Address/gmd:electronicMailAddress/gco:CharacterString";
	
	public final static String XP_TEMPORAL = "gmd:extent/gmd:EX_Extent/gmd:temporalElement";
	public final static String XP_TEMP_EXT = "gmd:EX_TemporalExtent/gmd:extent/gml:TimePeriod/";
	public final static String XP_TEMP_BEGIN = XP_TEMP_EXT + "gml:beginPosition";
	public final static String XP_TEMP_END = XP_TEMP_EXT + "gml:endPosition";

	public final static String XP_QUAL = "gmd:dataQualityInfo/gmd:DQ_DataQuality";
	public final static String XP_QUAL_LIN = XP_QUAL + "/gmd:lineage/gmd:LI_Lineage/gmd:statement";
	public final static String XP_QUAL_TYPE = XP_QUAL + "/gmd:scope/gmd:DQ_Scope/gmd:level/gmd:MD_ScopeCode/@codeListValue";
	
	public final static String XP_DISTS = "gmd:distributionInfo/gmd:MD_Distribution";
	public final static String XP_TRANSF = XP_DISTS + "/gmd:transferOptions/gmd:MD_DigitalTransferOptions/gmd:onLine/gmd:CI_OnlineResource";
	public final static String XP_FORMAT = XP_DISTS + "/gmd:distributionFormat/gmd:MD_Format/gmd:name";
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
		
		String name = contact.valueOf(XP_ORG_NAME);
		String email = contact.valueOf(XP_EMAIL);
		
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
	 * Parse and store multilingual string
	 * 
	 * @param store RDF store
	 * @param uri IRI of the dataset
	 * @param node multilingual DOM node
	 * @param property RDF property
	 * @param lang language code
	 * @throws RepositoryException 
	 */
	protected void parseMulti(Storage store, IRI uri, Node node, IRI property, String lang) 
			throws RepositoryException {
		if (node == null) {
			return;
		}
		String txt = node.valueOf(XP_STRLNG + "[@locale='#" + lang.toUpperCase() +"']");

		if (txt == null || txt.isEmpty()) {
			return;
		}	
		if (!lang.equals("en") && txt.equals(node.valueOf(XP_STR))) {
			return;
		}
		store.add(uri, property, txt, lang);
	}

	/**
	 * Generate distribution
	 * 
	 * @param store
	 * @param dataset
	 * @param node
	 * @throws MalformedURLException 
	 */
	protected void generateDist(Storage store, IRI dataset, Node node) 
												throws MalformedURLException {
		String url = node.valueOf(XP_DIST_URL);
		if (url == null || url.isEmpty() || url.equals("undefined")) {
			logger.debug("No url for distribution");
			return;
		}
			
		String id = makeHashId(dataset.toString()) + "/" + makeHashId(url);
        IRI dist = store.getURI(makeDistURL(id).toString());
        logger.debug("Generating distribution {}", dist.toString());
		
		store.add(dataset, DCAT.HAS_DISTRIBUTION, dist);
		store.add(dist, RDF.TYPE, DCAT.DISTRIBUTION);
		store.add(dist, DCAT.ACCESS_URL, store.getURI(url));

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
	
		String dtype = node.valueOf(XP_QUAL_TYPE);
		if (dtype != null) {
			if (!dtype.equals("dataset")) {
				logger.warn("Not a dataset: {} is {}", id, dtype);
				return;
			}
			store.add(dataset, DCTERMS.TYPE, store.getURI(INSPIRE_TYPE + dtype));
		}
		
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
		
		for (String lang : getAllLangs()) {
			store.add(dataset, DCTERMS.LANGUAGE, MDR_LANG.MAP.get(lang));

			parseMulti(store, dataset, title, DCTERMS.TITLE, lang);
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
		
		List<Node> dists = node.selectNodes(XP_TRANSF);
		for (Node dist: dists) {
			generateDist(store, dataset, dist);
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
		Map<String, Page> map = cache.retrievePage(getBase());
		String xml = map.get("all").getContent();

		SAXReader sax = new SAXReader();

		try {
			DocumentFactory.getInstance().setXPathNamespaceURIs(NS);
			Document doc = sax.read(new StringReader(xml));
			
			List<Node> datasets = doc.selectNodes(XP_DATASETS);
			for (Node dataset: datasets) {
				String id = dataset.valueOf(XP_ID);
				IRI iri = store.getURI(makeDatasetURL(id).toString());
				generateDataset(iri, id, store, dataset);
			}
		} catch (DocumentException ex) {
			logger.error("Error parsing XML");
			throw new RepositoryException(ex);
		}
	
		generateCatalog(store);
	}

	/**
	 * Scrape DCAT catalog.
	 *
	 * @param cache
	 * @throws IOException
	 */
	protected void scrapeCat(Cache cache) throws IOException {
		URL front = getBase();
		URL url = new URL(getBase() + GeonetGmd.API_RECORDS);
		System.err.println(url);
		String content = makeRequest(url);
		cache.storePage(front, "all", new Page(url, content));
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

		Map<String, Page> front = cache.retrievePage(getBase());
		if (front.keySet().isEmpty()) {
			scrapeCat(cache);
		}
		logger.info("Done scraping");
	}

	/**
	 * Constructor
	 *
	 * @param caching DB cache file
	 * @param storage SDB file to be used as triple store backend
	 * @param base base URL
	 */
	public GeonetGmd(File caching, File storage, URL base) {
		super(caching, storage, base);
	}
}
