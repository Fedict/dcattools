/*
 * Copyright (c) 2017, Bart Hanssens <bart.hanssens@fedict.be>
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
import java.net.MalformedURLException;
import java.net.URL;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import javax.xml.xpath.XPathFactory;
import javax.xml.xpath.XPathFactoryConfigurationException;
import net.sf.saxon.lib.NamespaceConstant;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.vocabulary.DCAT;
import org.eclipse.rdf4j.model.vocabulary.DCTERMS;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.model.vocabulary.VCARD4;
import org.eclipse.rdf4j.repository.RepositoryException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.xmlbeam.XBProjector;
import org.xmlbeam.annotation.XBRead;
import org.xmlbeam.config.DefaultXMLFactoriesConfig;
import org.xmlbeam.config.DefaultXMLFactoriesConfig.NamespacePhilosophy;
import org.xmlbeam.config.XMLFactoriesConfig;

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

	public final static DateFormat DATEFMT = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
	
	// XMLBeam "projection" interfaces
	protected interface GmdMultiString {
		@XBRead("./gco:CharacterString")
		public String getString();
		
		@XBRead("./gmd:PT_FreeText/gmd:textGroup/gmd:LocalisedCharacterString[@locale=$PARAM0]")
		public String getString(String lang);
	}
	
	protected interface GmdContact {
		@XBRead("./gmd:organisationName/gco:CharacterString")
		public String getName();

		@XBRead("./gmd:contactInfo/gmd:CI_Contact/gmd:address/gmd:CI_Address/gmd:electronicMailAddress/gco:CharacterString")		
		public String getEmail();		
	}
	
	protected interface GmdLink {
		@XBRead("./gmd:linkage/gmd:URL")
		public String getHref();
		
		@XBRead("./gmd:description")
		public GmdMultiString getDescription();
	}
	
	protected interface GmdDist {
		@XBRead("./gmd:distributionFormat/gmd:MD_Format/gmd:name")
		public GmdMultiString getFormat();
		
		@XBRead("./gmd:transferOptions/gmd:MD_DigitalTransferOptions/gmd:onLine/gmd:CI_OnlineResource")
		public List<GmdLink> getLinks();
	}
	
	protected interface GmdMeta {
		@XBRead("./gmd:citation/gmd:CI_Citation/gmd:title")
		public GmdMultiString getTitle();
		
		@XBRead("./gmd:abstract")
		public GmdMultiString getDescription();
		
		@XBRead("./gmd:descriptiveKeywords/gmd:MD_Keywords/gmd:keyword")
		public List<GmdMultiString> getKeywords();
	}
			
	public interface GmdDuration {
		@XBRead("./gmd:EX_TemporalExtent/gmd:extent/gml:TimePeriod/gml:beginPosition")
		public String getStart();

		@XBRead("./gmd:EX_TemporalExtent/gmd:extent/gml:TimePeriod/gml:endPosition")
		public String getEnd();
	}
		
	protected interface GmdDataset {
		@XBRead("gmd:fileIdentifier/gco:CharacterString")
		public String getID();
		
		@XBRead("gmd:contact/gmd:CI_ResponsibleParty")
		public GmdContact getContact();
		
		@XBRead("gmd:dateStamp/gco:DateTime")
		public String getTimestamp();
		
		@XBRead("gmd:identificationInfo/gmd:MD_DataIdentification")
		public GmdMeta getMeta();
		
		@XBRead("gmd:extent/gmd:EX_Extent/gmd:temporalElement")
		public GmdDuration getDuration();
			
		@XBRead("gmd:distributionInfo/gmd:MD_Distribution")
		public List<GmdDist> getDistributions();
	}
	
	protected interface GmdRoot {
		@XBRead("//gmd:MD_Metadata")
		public List<GmdDataset> getDatasets();
	}
	
	// Saxon XPath parser is 3x faster than the JDK's
	protected final XMLFactoriesConfig xmlcfg = new DefaultXMLFactoriesConfig() {
		@Override
		public XPathFactory createXPathFactory() {
			return new net.sf.saxon.xpath.XPathFactoryImpl();
		}
	};
	
	/**
	 * Parse a contact and store it in the RDF store
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
	 * Parse and store multilingual string
	 * 
	 * @param store RDF store
	 * @param uri IRI of the dataset
	 * @param property RDF property
	 * @param multi multi language structure
	 * @param lang language code
	 * @throws RepositoryException 
	 */
	protected void parseMulti(Storage store, IRI uri, GmdMultiString multi, IRI property, String lang) 
			throws RepositoryException {
		String str = multi.getString();
		String title = multi.getString("#" + lang.toUpperCase());

		if (title != null && (!str.equals(title) || lang.equals("en"))) {
			store.add(uri, property, title, lang);
		}
	}
	
	/**
	 * Generate DCAT dataset
	 * 
	 * @param store 
	 * @param id 
	 * @param meta 
	 * @throws java.net.MalformedURLException 
	 */
	protected void generateDataset(Storage store, String id, GmdDataset meta) 
			throws MalformedURLException {
		IRI dataset = store.getURI(makeDatasetURL(id).toString());
		logger.info("Generating dataset {}", dataset.toString());
		
		store.add(dataset, RDF.TYPE, DCAT.DATASET);
		store.add(dataset, DCTERMS.IDENTIFIER, id);

		String date = meta.getTimestamp();
		if (date != null) {
			try {
				store.add(dataset, DCTERMS.MODIFIED, DATEFMT.parse(date));
			} catch (ParseException ex) {
				logger.warn("Could not parse date {}", date, ex);
			}
		}
		
		GmdMeta metadata = meta.getMeta();	
		if (metadata == null) {
			logger.warn("No metadata for {}", id);
			return;
		}	

		List<GmdMultiString> keywords = metadata.getKeywords();
		GmdMultiString title = metadata.getTitle();
		GmdMultiString desc = metadata.getDescription();

		for (String lang : getAllLangs()) {
			store.add(dataset, DCTERMS.LANGUAGE, MDR_LANG.MAP.get(lang));

			parseMulti(store, dataset, title, DCTERMS.TITLE, lang);
			parseMulti(store, dataset, desc, DCTERMS.DESCRIPTION, lang);
			for (GmdMultiString keyword : keywords) {
				parseMulti(store, dataset, keyword, DCAT.KEYWORD, lang);
			}
		}
		GmdContact contact = meta.getContact();
		if (contact != null) {
			parseContact(store, dataset, contact.getName(), contact.getEmail());
		}
		
		List<GmdDist> dists = meta.getDistributions();
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

		try {
			GmdRoot m = new XBProjector(xmlcfg).projectXMLString(xml, GmdRoot.class);
			for (GmdDataset e: m.getDatasets()) {
				generateDataset(store, e.getID(), e);
			}
		} catch (IOException ex) {
			logger.error("Error projecting XML");
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
