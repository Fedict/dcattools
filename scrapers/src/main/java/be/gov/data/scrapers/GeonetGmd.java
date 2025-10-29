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
package be.gov.data.scrapers;

import be.gov.data.helpers.Storage;
import be.gov.data.dcat.vocab.MDR_LANG;

import java.io.IOException;
import java.io.StringReader;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
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
import org.eclipse.rdf4j.model.vocabulary.FOAF;
import org.eclipse.rdf4j.model.vocabulary.GEO;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.model.vocabulary.SKOS;
import org.eclipse.rdf4j.model.vocabulary.VCARD4;
import org.eclipse.rdf4j.model.vocabulary.XSD;
import org.eclipse.rdf4j.repository.RepositoryException;

/**
 * Abstract scraper for the GeonetRDF v3 portal software with DCAT export.
 *
 * @see http://geonetwork-opensource.org/
 *
 * @author Bart Hanssens
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

	private final SAXReader sax;
	private final static Map<String, String> NS = new HashMap<>();

	static {
		NS.put("csw", "http://www.opengis.net/cat/csw/2.0.2");
		NS.put("gmd", "http://www.isotc211.org/2005/gmd");
		NS.put("gmx", "http://www.isotc211.org/2005/gmx");
		NS.put("gml", "http://www.opengis.net/gml");
		NS.put("gml32", "http://www.opengis.net/gml/3.2");
		NS.put("gco", "http://www.isotc211.org/2005/gco");
		NS.put("srv", "http://www.isotc211.org/2005/srv");
		NS.put("xlink", "http://www.w3.org/1999/xlink");
	}

	public final static String NUM_REC = "csw:GetRecordsResponse/csw:SearchResults/@numberOfRecordsMatched";

	public final static String XP_DATASETS = "//gmd:MD_Metadata";

	public final static String XP_ID = "gmd:fileIdentifier/gco:CharacterString";
	public final static String XP_DSTAMP = "gmd:dateStamp/gco:Date";
	public final static String XP_TSTAMP = "gmd:dateStamp/gco:DateTime";
	public final static String XP_META = "gmd:identificationInfo/gmd:MD_DataIdentification";
	public final static String XP_META_SERV = "gmd:identificationInfo/srv:SV_ServiceIdentification";
	
	public final static String XP_KEYWORDS = "gmd:descriptiveKeywords/gmd:MD_Keywords/gmd:keyword";
	public final static String XP_KEYWORD_URI = "gmx:Anchor/@xlink:href";
	
	public final static String XP_TOPIC = "gmd:topicCategory/gmd:MD_TopicCategoryCode";
	public final static String XP_THESAURUS = "../gmd:thesaurusName/gmd:CI_Citation/gmd:title/gco:CharacterString";
	public final static String XP_THESAURUS2 = "../gmd:thesaurusName/gmd:CI_Citation/gmd:title/gmd:PT_FreeText/gmd:textGroup/gmd:LocalisedCharacterString";
	
	public final static String XP_LICENSE = "gmd:resourceConstraints/gmd:MD_LegalConstraints/gmd:otherConstraints";
	public final static String XP_LICENSE2 = "gmd:resourceConstraints/gmd:MD_Constraints/gmd:useLimitation";
	public final static String XP_LICENSE3 = "gmd:resourceConstraints/gmd:MD_LegalConstraints/gmd:useLimitation";
	
	public final static String XP_TYPE = "gmd:hierarchyLevel/gmd:MD_ScopeCode/@codeListValue";

	public final static String XP_TITLE = "gmd:citation/gmd:CI_Citation/gmd:title";
	public final static String XP_DESC = "gmd:abstract";
	public final static String XP_LANG = "gmd:language/gmd:LanguageCode/@codeListValue";
	public final static String XP_PURP = "gmd:purpose";
	public final static String XP_ANCHOR = "gmx:Anchor";
	public final static String XP_CHAR = "gco:CharacterString";

	public final static String XP_STR = "gco:CharacterString";
	public final static String XP_STRLNG = "gmd:PT_FreeText/gmd:textGroup/gmd:LocalisedCharacterString";

	public final static String XP_AUTHOR = ".//gmd:CI_ResponsibleParty/gmd:role/gmd:CI_RoleCode[@codeListValue='author']/../../gmd:individualName";
	public final static String XP_AUTHOR_ORG = ".//gmd:CI_ResponsibleParty/gmd:role/gmd:CI_RoleCode[@codeListValue='author']/../../gmd:organisationName";

	public final static String XP_CONTACT = "gmd:contact/gmd:CI_ResponsibleParty";
	public final static String XP_ORG_NAME = "gmd:organisationName";
	public final static String XP_INDIVIDUAL = "gmd:individualName";
	public final static String XP_EMAIL = "gmd:contactInfo/gmd:CI_Contact/gmd:address/gmd:CI_Address/gmd:electronicMailAddress/gco:CharacterString";

	public final static String XP_BBOX = "gmd:extent/gmd:EX_Extent/gmd:geographicElement/gmd:EX_GeographicBoundingBox";
	public final static String XP_BBOX_W = "gmd:westBoundLongitude/gco:Decimal";
	public final static String XP_BBOX_E = "gmd:eastBoundLongitude/gco:Decimal";
	public final static String XP_BBOX_S = "gmd:southBoundLatitude/gco:Decimal";
	public final static String XP_BBOX_N = "gmd:northBoundLatitude/gco:Decimal";
			
	public final static String XP_TEMPORAL = "gmd:extent/gmd:EX_Extent/gmd:temporalElement/gmd:EX_TemporalExtent/gmd:extent";
	public final static String XP_TEMP_BEGIN = "gml:TimePeriod/gml:beginPosition";
	public final static String XP_TEMP_END = "gml:TimePeriod/gml:endPosition";
	public final static String XP_TEMP32_BEGIN = "gml32:TimePeriod/gml32:beginPosition";
	public final static String XP_TEMP32_END = "gml32:TimePeriod/gml32:endPosition";
	
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
	public final static String XP_BYTESIZE = "../../gmd:transferSize/gco:Real";
	
	public final static String XP_PROTO = "gmd:protocol/gco:CharacterString";

	public final static String XP_DIST_URL = "gmd:linkage/gmd:URL";
	public final static String XP_DIST_NAME = "gmd:name";
	public final static String XP_DIST_DESC = "gmd:description";

	public final static String XP_MAINT = "gmd:resourceMaintenance/gmd:MD_MaintenanceInformation";
	public final static String XP_FREQ = XP_MAINT + "/gmd:maintenanceAndUpdateFrequency/gmd:MD_MaintenanceFrequencyCode/@codeListValue";

	public final static String XP_HREF = "./@xlink:href";
	public final static String SERV_TYPE = "srv:serviceType/gco:LocalName";

	public final static String INSPIRE_TYPE = "http://inspire.ec.europa.eu/metadatacodelist/ResourceType/";

	public final static DateFormat DATETIME_FMT = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
	public final static DateFormat DATE_FMT = new SimpleDateFormat("yyyy-MM-dd");		

	
	/**
	 * Parse and store multilingual string
	 *
	 * @param store RDF store
	 * @param uri IRI of the dataset
	 * @param node multilingual DOM node
	 * @param property RDF property
	 * @throws RepositoryException
	 */
	protected void parseMulti(Storage store, IRI uri, Node node, IRI property, boolean split) throws RepositoryException {
		if (node == null) {
			return;
		}
		
		String txten = null;
		for (String lang : getAllLangs()) {
			String txt = node.valueOf(XP_STRLNG + "[@locale='#" + lang.toUpperCase() + "']");
			if (txt != null && !txt.isEmpty()) {
				if (lang.equals("en")) {
					txten = txt;
				}
				if (split) {
					String[] words = txt.split(",");
					for (String word: words) {
						store.add(uri, property, word.strip(), lang);
					}
				} else {
					store.add(uri, property, txt.strip(), lang);
				}
			}
		}
		String txt = node.valueOf(XP_STR);
		if (txt != null && !txt.equals(txten)) {
			if (split) {
				String[] words = txt.split(",");
				for (String word: words) {
					store.add(uri, property, word.strip());
				}
			} else {
				store.add(uri, property, txt.strip());
			}
		}
	}

	/**
	 * Parse language of a dataset
	 * 
	 * @param store
	 * @param iri
	 * @param langs 
	 */
	protected void parseLanguage(Storage store, IRI iri, List<Node> langs) {
		if (langs == null || langs.isEmpty()) {
			LOG.warn("No language for dataset {}", iri);
			return;
		}
		
		for (Node lang: langs) {
			IRI lc = MDR_LANG.MAP.get(lang.getStringValue().substring(0, 2));
			if (lc != null) {
				store.add(iri, DCTERMS.LANGUAGE, lc);
			} else {
				LOG.error("No IRI for language {}", lang.getStringValue());
			}
		}
	}
	
	/**
	 * Parse date or datetime stamp
	 * 
	 * @param store
	 * @param node
	 * @param iri
	 * @throws RepositoryException 
	 */
	protected void parseStamp(Storage store, IRI iri, Node node) throws RepositoryException {	
		String stamp = node.valueOf(XP_TSTAMP);
		if (stamp != null && !stamp.isEmpty()) {
			try {
				store.add(iri, DCTERMS.MODIFIED, DATETIME_FMT.parse(stamp));
			} catch (ParseException ex) {
				LOG.warn("Could not parse datetime {}", stamp, ex);
			}
		} else {
			stamp = node.valueOf(XP_DSTAMP);
			if (stamp != null && !stamp.isEmpty()) {
				try {
					store.add(iri, DCTERMS.MODIFIED, DATE_FMT.parse(stamp));
				} catch (ParseException ex) {
					LOG.warn("Could not parse date {}", stamp, ex);
				}
			}
		}
	}

	/**
	 * Parse a bounding box and store it in the RDF store.
	 *
	 * @param store RDF store
	 * @param iri RDF subject URI
	 * @param node
	 * @throws RepositoryException
	 */
	protected void parseBBox(Storage store, IRI iri, Node node) throws RepositoryException {
		String n = node.valueOf(XP_BBOX_N);
		String w = node.valueOf(XP_BBOX_W);
		String s = node.valueOf(XP_BBOX_S);
		String e = node.valueOf(XP_BBOX_E);
		
		if(w != null && !w.isEmpty() && e != null && !e.isEmpty() &&
			s != null && !s.isEmpty() && n != null && !n.isEmpty()) {
			// round to two decimals
			n = String.valueOf(Math.ceil(Double.parseDouble(n) * 100) / 100);
			w = String.valueOf(Math.floor(Double.parseDouble(w) * 100) / 100);
			s = String.valueOf(Math.floor(Double.parseDouble(s) * 100) / 100);
			e = String.valueOf(Math.ceil(Double.parseDouble(e) * 100) / 100);
			
			String bbox = "POLYGON(" + w + " " + s + ", " + e + " " + s + ", " +
										e + " " + n + ", " + w + " " + n + ", " +
										w + " " + s + ")";
			IRI spatial = makeBboxIRI(n, e, s, w);
			store.add(iri, DCTERMS.SPATIAL, spatial);
			store.add(spatial, DCAT.BBOX, bbox, GEO.WKT_LITERAL);
		}
	}

	/**
	 * Parse a temporal and store it in the RDF store.
	 *
	 * @param store RDF store
	 * @param uri RDF subject URI
	 * @param node
	 * @throws RepositoryException
	 */
	protected void parseTemporal(Storage store, IRI uri, Node node) throws RepositoryException {
		String start = node.valueOf(XP_TEMP_BEGIN);
		if (start == null || start.isEmpty()) {
			start = node.valueOf(XP_TEMP32_BEGIN);
		}
		String end = node.valueOf(XP_TEMP_END);
		if (end == null || end.isEmpty()) {
			end = node.valueOf(XP_TEMP32_END);
		}
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
		Node org = contact.selectSingleNode(XP_ORG_NAME);
		Node person = contact.selectSingleNode(XP_INDIVIDUAL);

		String email = contact.valueOf(XP_EMAIL);

		if (org != null || person != null || !email.isEmpty()) {
			IRI vcard = makeOrgIRI(hash(org + email));
			store.add(uri, DCAT.CONTACT_POINT, vcard);
			store.add(vcard, RDF.TYPE, VCARD4.KIND);
			if (org != null) {
				store.add(vcard, RDF.TYPE, VCARD4.ORGANIZATION);
			}

			parseMulti(store, vcard, org, VCARD4.FN, false);
			if (org == null) {
				parseMulti(store, vcard, person, VCARD4.FN, false);
			}
			if (!email.isEmpty()) {
				store.add(vcard, VCARD4.HAS_EMAIL, store.getURI("mailto:" + email));
			}
		}
	}
	
	/**
	 * Parse contacts to get individual authors
	 * 
	 * @param store
	 * @param iri
	 * @param contacts 
	 */
	protected void parseAuthorPersons(Storage store, IRI iri, List<Node> contacts) {
		for(Node c: contacts) {
			Node node = c.selectSingleNode(XP_ANCHOR);
			String id = null;
			
			if (node == null) {
				node = c.selectSingleNode(XP_CHAR);
			} else {
				// check DOI handle
				id = node.valueOf(XP_HREF);
			}
			if (node != null) {
				String name = node.getText();
				IRI person = makePersonIRI(hash(name));
				store.add(iri, DCTERMS.CREATOR, person);
				store.add(person, RDF.TYPE, FOAF.PERSON);
				store.add(person, FOAF.NAME, name);
				if (id != null && !id.isEmpty()){
					store.add(person, DCTERMS.IDENTIFIER, id);
				}
			}
		}
	}
	
	/**
	 * Parse contacts to get organization authors
	 * 
	 * @param store
	 * @param iri
	 * @param contacts 
	 */
	protected void parseAuthorOrgs(Storage store, IRI iri, List<Node> contacts) {
		for(Node c: contacts) {
			Node node = c.selectSingleNode(XP_CHAR);
			if (node != null) {
				String name = node.getText();
				if (name != null && !name.isBlank()) {
					IRI org = makeOrgIRI(hash(name));
					store.add(iri, DCTERMS.CREATOR, org);
					store.add(org, RDF.TYPE, FOAF.ORGANIZATION);
					parseMulti(store, org, node, FOAF.NAME, false);
				}
			}
		}
	}

	/**
	 * Parse keyword from thesaurus to DCAT theme or subject
	 * 
	 * @param store
	 * @param dataset
	 * @param thesaurus
	 * @param keywords 
	 */
	protected void parseTheme(Storage store, IRI dataset, Node thesaurus, Node keyword) {
		if (thesaurus.getText().equals("Theme")) {
			// assume it is a DCAT theme
			parseMulti(store, dataset, keyword, DCAT.THEME, true);
			return;
		}
		Node subj = keyword.selectSingleNode(XP_KEYWORD_URI);
		if (subj != null && !subj.getStringValue().isBlank()) {
			try {
				IRI skos = makeIRI(subj.getStringValue());
				store.add(dataset, DCTERMS.SUBJECT, skos);
				store.add(skos, RDF.TYPE, SKOS.CONCEPT);
				parseMulti(store, skos, subj, SKOS.PREF_LABEL, true);
			} catch (IllegalArgumentException ioe){
				LOG.warn("Invalid URI {}", subj.getStringValue());
			}
		}
	}

	/**
	 * Generate distribution
	 *
	 * @param store
	 * @param dataset
	 * @param node
	 * @param format
	 * @param license
	 */
	protected void generateDist(Storage store, IRI dataset, Node node, String format, List<String> license) {
		String url = node.valueOf(XP_DIST_URL);
		if (url == null || url.isEmpty() || url.equals("undefined")) {
			LOG.warn("No url for distribution");
			return;
		}

		String id = hash(dataset.toString()) + "/" + hash(url);
		IRI dist = makeDistIRI(id);
		LOG.debug("Generating distribution {}", dist.toString());

		store.add(dataset, DCAT.HAS_DISTRIBUTION, dist);
		store.add(dist, RDF.TYPE, DCAT.DISTRIBUTION);
		if (format != null) {
			store.add(dist, DCTERMS.FORMAT, format);
		}
		String size = node.valueOf(XP_BYTESIZE);
		if (size != null && !size.isEmpty()) {
			try {
				store.add(dist, DCAT.BYTE_SIZE, size, XSD.DOUBLE);
			} catch (NumberFormatException nfe) {
				LOG.error("Could not convert {} to byte size", size);
			}
		}
		
		if (license != null) {
			for (String l: license) {
				store.add(dist, DCTERMS.LICENSE, l);
			}
		}
		try {
			store.add(dist, DCAT.DOWNLOAD_URL, store.getURI(url));
		} catch (IllegalArgumentException e) {
			LOG.debug("Cannot create download URL for {}", url);
		}

		Node title = node.selectSingleNode(XP_DIST_NAME);
		Node desc = node.selectSingleNode(XP_DIST_DESC);

		parseMulti(store, dist, title, DCTERMS.TITLE, false);
		parseMulti(store, dist, desc, DCTERMS.DESCRIPTION, false);
	}

	/**
	 * Generate DCAT dataset
	 *
	 * @param dataset
	 * @param id
	 * @param store
	 * @param node
	 */
	protected void generateDataset(IRI dataset, String id, Storage store, Node node) {
		LOG.info("Generating dataset {}", dataset.toString());

		Node metadata = node.selectSingleNode(XP_META);
		if (metadata == null) {
			metadata = node.selectSingleNode(XP_META_SERV);
		}
		if (metadata == null) {
			LOG.error("No metadata for {}", id);
			return;
		}

		// try to  determine type
		String dtype = node.valueOf(XP_QUAL_TYPE);
		if (dtype == null || dtype.isEmpty()) {
			dtype = node.valueOf(XP_TYPE);
		}
		if (dtype == null || dtype.isEmpty()) {
			LOG.warn("Empty type for {}, assuming dataset", id);
			dtype = "dataset";
		}
		switch(dtype) {
			case "dataset" -> store.add(dataset, RDF.TYPE, DCAT.DATASET);
			case "series" -> store.add(dataset, RDF.TYPE, DCAT.DATASET); // fix this
			case "service" -> store.add(dataset, RDF.TYPE, DCAT.DATA_SERVICE);
			default ->  {
							LOG.warn("Unknown type: {} is {}", id, dtype); 
								return; }
		}
		store.add(dataset, DCTERMS.IDENTIFIER, id);

		// modified
		parseStamp(store, dataset, node);

		Node title = metadata.selectSingleNode(XP_TITLE);
		parseMulti(store, dataset, title, DCTERMS.TITLE, false);
		
		Node desc = metadata.selectSingleNode(XP_DESC);
		parseMulti(store, dataset, desc, DCTERMS.DESCRIPTION, false);

		List<Node> langs = metadata.selectNodes(XP_LANG);
		parseLanguage(store, dataset, langs);

		List<Node> keywords = metadata.selectNodes(XP_KEYWORDS);
		
		for (Node keyword : keywords) {
			Node thesaurus = keyword.selectSingleNode(XP_THESAURUS);
			if (thesaurus == null) {
				thesaurus = keyword.selectSingleNode(XP_THESAURUS2);
			}
			if (thesaurus == null) {
				// full text keyword
				parseMulti(store, dataset, keyword, DCAT.KEYWORD, true);
			} else {
				parseTheme(store, dataset, thesaurus, keyword);
			}
		}
		
		// themes
		List<Node> topics = metadata.selectNodes(XP_TOPIC);
		for (Node topic: topics) {
			store.add(dataset, DCAT.THEME, topic.getText());
		}

		// geo bounding box
		Node geo = metadata.selectSingleNode(XP_BBOX);
		if (geo != null) {
			parseBBox(store, dataset, geo);
		}
	
		// time range
		Node range = metadata.selectSingleNode(XP_TEMPORAL);
		if (range != null) {
			parseTemporal(store, dataset, range);
		}

		// frequency
		String freq = metadata.valueOf(XP_FREQ);
		if (freq != null) {
			store.add(dataset, DCTERMS.ACCRUAL_PERIODICITY, freq);
		}

		// contact point
		Node contact = node.selectSingleNode(XP_CONTACT);
		if (contact != null) {
			parseContact(store, dataset, contact);
		}
		
		// authors
		List<Node> persons = node.selectNodes(XP_AUTHOR);
		if (persons != null && !persons.isEmpty()) {
			parseAuthorPersons(store, dataset, persons);
		}
		List<Node> orgs = node.selectNodes(XP_AUTHOR_ORG);
		if (orgs != null && !orgs.isEmpty()) {
			parseAuthorOrgs(store, dataset, orgs);
		}

		// Distributions can be defined on several (hierarchical) levels
		List<Node> dists = node.selectNodes(XP_DISTS2 + XP_TRANSF2);
		// check higher level if no distributions could be found
		if (dists == null || dists.isEmpty()) {
			LOG.warn("Checking for dists on higher level");
			dists = node.selectNodes(XP_DISTS + XP_TRANSF);
		}

		if (dists == null || dists.isEmpty()) {
			LOG.warn("No dists for {}", id);
			return;
		}

		// License / rights can be listed in different nodes
		List<Node> lic = new ArrayList<>();
		lic.addAll(metadata.selectNodes(XP_LICENSE));
		lic.addAll(metadata.selectNodes(XP_LICENSE2));
		lic.addAll(metadata.selectNodes(XP_LICENSE3));

		List<String> licenses = new ArrayList<>();
		for (Node n : lic) {
			String anchor = n.valueOf(XP_ANCHOR);
			if (anchor != null && !anchor.isEmpty()) {
				licenses.add(anchor);
			}
			String str = n.valueOf(XP_CHAR);
			if (str != null && !str.isEmpty()) {
				licenses.add(str);
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
			generateDist(store, dataset, dist, str, licenses);
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

		try {
			for (URL url : urls) {
				Map<String, Page> map = cache.retrievePage(url);
				String xml = map.get("all").getContent();
				Document doc = sax.read(new StringReader(xml));

				List<Node> datasets = doc.selectNodes(XP_DATASETS);
				for (Node dataset : datasets) {
					String id = dataset.valueOf(XP_ID);
					IRI iri = makeDatasetIRI(id);
					generateDataset(iri, id, store, dataset);
				}
			}
		} catch (DocumentException ex) {
			LOG.error("Error parsing XML");
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
			LOG.info(n + " records found");
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
		String prevhash = "";
				
		for (int pos = 1, recs = MAX_RECORDS; pos < recs; pos += MAX_RECORDS) {
			URL url = new URL(getBase() + API_RECORDS + POSITION + pos);
			String xml = makeRequest(url);
			
			prevhash = detectLoop(prevhash, xml);

			try {
				Document doc = sax.read(new StringReader(xml));
				if (pos == 1) {
					recs = getNumRecords(doc);
				}
				cache.storePage(url, "all", new Page(url, xml));
			} catch (DocumentException ex) {
				LOG.error("Error parsing XML " + url);
			}
		}			
	}

	/**
	 * Scrape DCAT catalog.
	 *
	 * @throws IOException
	 */
	@Override
	public void scrape() throws IOException {
		LOG.info("Start scraping");
		Cache cache = getCache();

		Set<URL> urls = cache.retrievePageList();
		if (urls.isEmpty()) {
			scrapeCat(cache);
		}
		LOG.info("Done scraping");
	}

	/**
	 * Constructor
	 *
	 * @param prop
	 * @throws java.io.IOException
	 */
	protected GeonetGmd(Properties prop) throws IOException {
		super(prop);
		
		DocumentFactory factory = DocumentFactory.getInstance();
		factory.setXPathNamespaceURIs(NS);
		sax = new SAXReader();
		sax.setDocumentFactory(factory);
	}
}
