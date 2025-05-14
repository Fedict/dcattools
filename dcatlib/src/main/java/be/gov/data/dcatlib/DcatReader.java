/*
 * Copyright (c) 2023, FPS BOSA
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

package be.gov.data.dcatlib;

import be.gov.data.dcatlib.model.Catalog;
import be.gov.data.dcatlib.model.CatalogRecord;
import be.gov.data.dcatlib.model.DataResource;
import be.gov.data.dcatlib.model.Dataservice;
import be.gov.data.dcatlib.model.Dataset;
import be.gov.data.dcatlib.model.Distribution;
import be.gov.data.dcatlib.model.Organization;
import be.gov.data.dcatlib.model.SkosTerm;
import java.io.BufferedReader;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.eclipse.rdf4j.model.BNode;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.util.Values;
import org.eclipse.rdf4j.model.vocabulary.DCAT;
import org.eclipse.rdf4j.model.vocabulary.DCTERMS;
import org.eclipse.rdf4j.model.vocabulary.FOAF;
import org.eclipse.rdf4j.model.vocabulary.OWL;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.model.vocabulary.SKOS;
import org.eclipse.rdf4j.model.vocabulary.VCARD4;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.Rio;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Read DCAT-AP v2 data into a simplified model for data.gov.be
 * 
 * @author Bart Hanssens
 */
public class DcatReader {
	private final Path file;
	private final InputStream is;
	private final RDFFormat fmt;

	private final static Logger LOG = LoggerFactory.getLogger(DcatReader.class);
	private final static SimpleDateFormat DATETIMEZF_FMT = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSXXX");
	private final static SimpleDateFormat DATETIMEZ_FMT = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX");
	private final static SimpleDateFormat DATETIME_FMT = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
	private final static SimpleDateFormat DATE_FMT = new SimpleDateFormat("yyyy-MM-dd");
	private final static SimpleDateFormat YEARMONTH_FMT = new SimpleDateFormat("yyyy-MM");
	private final static SimpleDateFormat YEAR_FMT = new SimpleDateFormat("yyyy");
	
	private final static IRI DCATAP_LEGISLATION = Values.iri("http://data.europa.eu/r5r/applicableLegislation");
	private final static IRI DCATAP_CATEGORIES = Values.iri("http://data.europa.eu/r5r/hvdCategory");
	
	private final static Map<IRI,String> LANG_MAP = 
		Map.of(Values.iri("http://publications.europa.eu/resource/authority/language/NLD"), "nl",
				Values.iri("http://publications.europa.eu/resource/authority/language/FRA"), "fr",
				Values.iri("http://publications.europa.eu/resource/authority/language/DEU"), "de",
				Values.iri("http://publications.europa.eu/resource/authority/language/ENG"), "en"
			);
	
	private final static IRI ADMS_IDENTIFIER = Values.iri("http://www.w3.org/ns/adms#identifier");

	private Model m;

	/**
	 * Get a single value
	 * 
	 * @param subj subject
	 * @param pred predicate
	 * @return value or null
	 * @throws IOException 
	 */
	private Value getValue(Resource subj, IRI pred) throws IOException {
		Set<Value> objects = m.filter(subj, pred, null).objects();
		if (objects.isEmpty()) {
			return null;
		}
		if (objects.size() > 1) {
			LOG.warn("More than 1 value for " + subj + " " + pred);
		}
		return objects.stream().findFirst().get();
	}

	/**
	 * Get a set of values
	 * 
	 * @param subj subject
	 * @param pred predicate
	 * @return values or null
	 */
	private Set<Value> getValues(Resource subj, IRI pred) {
		return m.filter(subj, pred, null).objects();
	}

	/**
	 * Get a single date
	 * 
	 * @param subj subject
	 * @param pred predicate
	 * @return date or null value
	 * @throws IOException when there are multiple values 
	 */
	private Date getDate(Resource subj, IRI pred) throws IOException {
		Value value = getValue(subj, pred);
		if (value == null) {
			return null;
		}
		String date = value.stringValue();
		try {
			if (date.contains("T")) {
				if (date.contains("+")) {
					if (date.contains(".")) {
						return DATETIMEZF_FMT.parse(date);
					} else {
						return DATETIMEZ_FMT.parse(date);
					}
				} else {
					return DATETIME_FMT.parse(date);	
				}
			}
			if (date.length() > 7) {
				return DATE_FMT.parse(date);
			}
			if (date.length() > 4) {
				return YEARMONTH_FMT.parse(date);
			}
			return YEAR_FMT.parse(date);
		} catch(ParseException pe) {
			throw new IOException(pe);
		}
	}

	/**
	 * Get a single string
	 * 
	 * @param subj subject
	 * @param pred predicate
	 * @return string or null value
	 * @throws IOException when there are multiple values
	 */
	private String getString(Resource subj, IRI pred) throws IOException {
		Value value = getValue(subj, pred);
		return (value != null) ? value.stringValue() : null;
	}

	/**
	 * Get a single IRI or blank node
	 * 
	 * @param subj subject
	 * @param pred predicate
	 * @return IRI/Bnode or null value
	 * @throws IOException when there are multiple values
	 */
	private Resource getResource(Resource subj, IRI pred) throws IOException {
		Value value = getValue(subj, pred);
		if (value == null) {
			return null;
		}
		if (! (value instanceof Resource)) {
			throw new IOException("Not a resource " + value);
		}
		return (Resource) value;
	}

	/**
	 * Get a single IRI
	 * 
	 * @param subj subject
	 * @param pred predicate
	 * @return IRI or null value
	 * @throws IOException when there are multiple values
	 */
	private IRI getIRI(Resource subj, IRI pred) throws IOException {
		Value value = getValue(subj, pred);
		if (value == null) {
			return null;
		}
		if (! (value instanceof IRI)) {
			throw new IOException("Not a IRI  " + value + " for " + pred);
		}
		return (IRI) value;
	}

	/**
	 * Get a set of IRIs
	 * 
	 * @param subj subject
	 * @param pred predicate
	 * @return set of IRIs
	 * @throws IOException when not IRIs
	 */
	private Set<IRI> getIRIs(Resource subj, IRI pred) throws IOException {
		Set<IRI> values = new HashSet<>();
		for(Statement s: m.getStatements(subj, pred, null)) {
			Value value = s.getObject();
			if (! (value instanceof IRI)) {
				throw new IOException("Not a IRI " + value + " for " + pred);
			}
			values.add((IRI) value);
		}
		return values;
	}

	/**
	 * Get a map with a single IRI per language
	 * 
	 * @param subj subject
	 * @param pred predicate
	 * @return map of IRIs per language
	 * @throws IOException when language tag is invalid
	 */
	private Map<String,IRI> getLangIRI(Resource subj, IRI pred) throws IOException {
		Map<String,IRI> map = new HashMap<>();

		// we might have different IRIs per language, but also 1 IRI for multiple languages
		Set<IRI> iris = getIRIs(subj, pred);
		for (IRI iri: iris) {
			Set<IRI> langs = getIRIs(iri, DCTERMS.LANGUAGE);
			// no language defined, so valid for all languages
			if (langs.isEmpty()) {
				if (map.containsKey("")) {
					LOG.warn("Undefined language for " + subj + " " + pred + " already present");
					continue;
				}
				map.put("", iri);
			} else {
				for (IRI lang: langs) {
					String code = LANG_MAP.get(lang);
					if (code == null) {
						throw new IOException("Language " + lang + " not found");
					}
					if (map.containsKey(code)) {
						LOG.warn("Language " + code + " for " + subj + " " + pred + " already present");
					}
					map.put(code, iri);
				}
			}
		}
		return map;
	}

	/**
	 * Get a map with (possibly) multiple IRI per language
	 * 
	 * @param subj subject
	 * @param pred predicate
	 * @return map of IRIs per language
	 * @throws IOException
	 */
	private Map<String,Set<IRI>> getLangIRIs(Resource subj, IRI pred) throws IOException {
		Map<String,Set<IRI>> map = new HashMap<>();

		Set<IRI> iris = getIRIs(subj, pred);
		for (IRI iri: iris) {
			IRI lang = getIRI(iri, DCTERMS.LANGUAGE);
			String code = "";
			if (lang != null) {
				code = LANG_MAP.get(lang);
				if (code == null) {
					throw new IOException("Language " + lang + " for " + subj + " " + pred + " not found");
				}
			}
			Set<IRI> s = map.get(code);
			if (s == null) {
				s = new HashSet<>();
				map.put(code, s);
			}
			s.add(iri);
		}
		return map;
	}

	/**
	 * Get a set of literals
	 * 
	 * @param subj subject
	 * @param pred predicate
	 * @return set of literals
	 * @throws IOException when not literals
	 */
	private Set<Literal> getLiterals(Resource subj, IRI pred) throws IOException {
		Set<Literal> values = new HashSet<>();
		for(Statement s: m.getStatements(subj, pred, null)) {
			Value value = s.getObject();
			if (! (value instanceof Literal)) {
				throw new IOException("Not a literal " + value);
			}
			values.add((Literal) value);
		}
		return values;
	}
	
	/**
	 * Get a map with a single string per language.
	 * The language tag will be lowercased, and "normalized" (only first 2 characters of the tag are used)
	 * 
	 * @param subj subject
	 * @param pred predicate
	 * @return map of strings per language
	 * @throws IOException when language tag is missing or multiple values per language
	 */
	private Map<String,String> getLangString(Resource subj, IRI pred) throws IOException {
		Map<String,String> map = new HashMap<>();
		
		Set<Literal> values = getLiterals(subj, pred);
		for (Literal v: values) {
			Optional<String> lang = v.getLanguage();
			String code = "";
			if (lang.isPresent()) {
				code = lang.get().substring(0, 2).toLowerCase(Locale.US);
			}
			if (map.containsKey(code)) {
				LOG.warn("Existing {} value for {} {} {}", code, subj, pred);		
			} else {
				map.put(code, v.stringValue());
			}
		}
		return map;
	}

	/**
	 * Get a list of strings per language
	 * 
	 * @param subj subject
	 * @param pred predicate
	 * @return map of lists of strings per language
	 * @throws IOException when language tag is missing
	 */
	private Map<String,Set<String>> getLangStringList(Resource subj, IRI pred) throws IOException {
		Map<String,Set<String>> map = new HashMap<>();

		Set<Literal> values = getLiterals(subj, pred);
		for (Literal v: values) {
			Optional<String> lang = v.getLanguage();
			String code = "";
			if (lang.isPresent()) {
				code = lang.get().substring(0, 2).toLowerCase(Locale.US);
			} else {
				LOG.warn("No language tag for {} {} {}", subj, pred, v);
			}
			map.computeIfAbsent(code, h -> new HashSet<String>()).add(v.stringValue());
		}
		return map;
	}

	/**
	 * Get the names of the creators (can be persons or companies)
	 * 
	 * @param iri
	 * @return 
	 * @throws IOException
	 */
	private Map<String,Set<String>> getCreatorNames(Set<Value> creators) throws IOException {
		Map<String,Set<String>> names = new HashMap<>();
		
		for(Value v: creators) {
			Map<String,Set<String>> name = getLangStringList((Resource) v, FOAF.NAME);
			for(Map.Entry<String,Set<String>> entry: name.entrySet()) {
				names.merge(entry.getKey(), entry.getValue(), (s1, s2) -> {  s1.addAll(s2); return s1; }  );
			}
		}
		return names;
	}

	/**
	 * Get the ADMS IDs as string values
	 * 
	 * @param iri
	 * @return
	 * @throws IOException
	 */
	private Set<String> getAdmsIds(IRI iri) throws IOException {
		Set<String> adms = new HashSet<>();

		Set<Value> objs = getValues(iri, ADMS_IDENTIFIER);
		
		for(Value obj: objs) {
			String s = obj.stringValue();
			if (obj instanceof IRI && !s.contains("well-known/genid")) {
				adms.add(s);
			} else {
				if (obj instanceof BNode) {
					String notation = getString((Resource) obj, SKOS.NOTATION);
					if (notation != null && !notation.isEmpty()) {
						adms.add(notation);
					}
				}
			}
		}
		return adms;
	}

	/**
	 * Read the common DCAT Resource part of a DCAT Dataset or Dataservice
	 * 
	 * @param iri 
	 * @return DCAT resource
	 * @throws IOException when mandatory ID is missing
	 */
	private DataResource readResource(IRI iri, DataResource d) throws IOException {
		String id = getString(iri, DCTERMS.IDENTIFIER);
		if (id == null || id.isEmpty()) {
			throw new IOException("No identifier for " + iri);
		}
		d.setIRI(iri);
		
		d.setId(id);

		Set<String> ids = getAdmsIds(iri);				
		ids.add(id);

		d.setIds(ids);
		// check borh OWL (legacy) and DCAT version
		d.setVersion(getString(iri, OWL.VERSIONINFO));
		d.setVersion(getString(iri, DCAT.VERSION));
		d.setTitle(getLangString(iri, DCTERMS.TITLE));
		d.setDescription(getLangString(iri, DCTERMS.DESCRIPTION));
		d.setKeywords(getLangStringList(iri, DCAT.KEYWORD));

	//	d.setSubjects(getIRIs(iri, DCTERMS.SUBJECT));
		d.setThemes(getIRIs(iri, DCAT.THEME));
		d.setHvDCategories(getIRIs(iri, DCATAP_CATEGORIES));

		d.setPublisher(getIRI(iri, DCTERMS.PUBLISHER));
		
		Set<Value> creators = getValues(iri, DCTERMS.CREATOR);
		Map<String,Set<String>> names = getCreatorNames(creators);
		d.setCreators(names);
		
		Set<IRI> legislation = getIRIs(iri, DCATAP_LEGISLATION);
		// also add legislation that is mentioned in conformsTo
		legislation.addAll(getIRIs(iri, DCTERMS.CONFORMS_TO).stream()
										.filter(i -> i.toString().contains("/eli/"))
										.collect(Collectors.toSet()));		
		d.setLegislation(legislation);

		d.setAccrualPeriodicity(getIRI(iri, DCTERMS.ACCRUAL_PERIODICITY));
		d.setSpatial(getIRI(iri, DCTERMS.SPATIAL));
		d.setLicense(getIRI(iri, DCTERMS.LICENSE));
		d.setIssued(getDate(iri, DCTERMS.CREATED));
		d.setModified(getDate(iri, DCTERMS.MODIFIED));

		Resource contact = getResource(iri, DCAT.CONTACT_POINT);
		if (contact == null) {
			throw new IOException("No contact for " + iri);
		}
		d.setContactName(getLangString(contact, VCARD4.FN));
		d.setContactAddr(getLangIRI(contact, VCARD4.HAS_EMAIL));
		d.setContactSite(getLangIRI(contact, VCARD4.HAS_URL));

		Resource temp = getResource(iri, DCTERMS.TEMPORAL);
		if (temp != null) {
			d.setStartDate(getDate(temp, DCAT.START_DATE));
			d.setEndDate(getDate(temp, DCAT.END_DATE));
		}
		d.setLandingPage(getLangIRI(iri, DCAT.LANDING_PAGE));

		return d;
	}

	/**
	 * Read DCAT Distribution
	 * 
	 * @param dataset
	 * @throws IOException 
	 */
	private void readDistributions(Dataset dataset) throws IOException {
		List<Distribution> dists = new ArrayList<>();

		for (Statement stmt: m.getStatements(dataset.getIRI(), DCAT.HAS_DISTRIBUTION, null)) {
			IRI iri = (IRI) stmt.getObject();
			Distribution dist = new Distribution();
			dist.setIRI(iri);
			dist.setAccessURLs(getLangIRIs(iri, DCAT.ACCESS_URL));
			dist.setDownloadURLs(getLangIRIs(iri, DCAT.DOWNLOAD_URL));
			dist.setFormat(getIRI(iri, DCTERMS.FORMAT));
			dist.setLicense(getIRI(iri, DCTERMS.LICENSE));

			dists.add(dist);
		}
		dataset.setDistributions(dists);
	}

	/**
	 * Read DCAT Datasets and add them to the DCAT Catalog
	 * 
	 * @param catalog 
	 */
	private void readDatasets(Catalog catalog) {
		LOG.info("Reading datasets");

		int ok = 0;
		int skip = 0;

		for (Statement stmt: m.getStatements(null, RDF.TYPE, DCAT.DATASET)) {
			IRI iri = (IRI) stmt.getSubject();
			try {
				Dataset d = (Dataset) readResource(iri, new Dataset());
				readDistributions(d);
				catalog.addDataset(d.getId(), d);
				ok++;
			} catch (IOException ioe) {
				LOG.error("Skipping dataset {} : {}", iri, ioe.getMessage());
				skip++;
			}
		}
		LOG.info("Read {} datasets, skipping {}", ok, skip);
	}

	/**
	 * Read DCAT Dataservices and add them to the DCAT Catalog
	 * 
	 * @param catalog 
	 */
	private void readDataservices(Catalog catalog) throws IOException {
		LOG.info("Reading dataservices");
	
		int ok = 0;
		int skip = 0;

		for (Statement stmt: m.getStatements(null, RDF.TYPE, DCAT.DATA_SERVICE)) {
			IRI iri = (IRI) stmt.getSubject();
			try {
				Dataservice d = (Dataservice) readResource(iri, new Dataservice());				
				catalog.addDataservice(d.getId(), d);		
				ok++;
			} catch (IOException ioe) {
				LOG.error("Skipping dataservice {} : {}", iri, ioe.getMessage());
				skip++;
			}			
		}
		LOG.info("Read {} dataservices, skipping {}", ok, skip);
	}

	/**
	 * Lookup datasets served by dataservices (if available)
	 * 
	 * @param catalog
	 * @throws IOException 
	 */
	private void readServes(Catalog catalog) throws IOException {
		for(Dataservice service: catalog.getDataservices().values()) {
			IRI iri = service.getIRI();
			List<Dataset> datasets = new ArrayList<>();
			Set<IRI> refs = getIRIs(iri, DCAT.SERVES_DATASET);
		
			for (IRI ref: refs) {
				Dataset d = catalog.getDataset(ref);
				if (d != null) {
					datasets.add(d);
				} else {
					LOG.warn("Dataset {} not present, served by {}", ref, iri);
				}
			}
			service.setDatasets(datasets);
		}
	}

	/**
	 * Read CatalogRecords
	 * 
	 * @param catalog
	 * @throws IOException 
	 */
	private void readRecords(Catalog catalog) throws IOException {
		LOG.info("Reading Catalog records");
	
		int ok = 0;
		int skip = 0;

		for (Statement stmt: m.getStatements(null, RDF.TYPE, DCAT.CATALOG_RECORD)) {
			IRI iri = (IRI) stmt.getSubject();
			try {
				CatalogRecord rec = new CatalogRecord();
				rec.setId(getValue(iri, DCTERMS.IDENTIFIER).stringValue());
				rec.setTopics(getIRIs(iri, FOAF.PRIMARY_TOPIC).stream()
														.collect(Collectors.toSet()));
				catalog.addRecord(iri.stringValue(), rec);
				ok++;
			} catch (IOException ioe) {
				LOG.error(ioe.getMessage());
				skip++;
			}			
		}
		LOG.info("Read {} records, skipping {}", ok, skip);
	}
	
	/**
	 * Read SKOS terms
	 * 
	 * @param catalog
	 * @throws IOException 
	 */
	private void readTerms(Catalog catalog) throws IOException {
		LOG.info("Reading SKOS terms");
	
		int ok = 0;
		int skip = 0;

		for (Statement stmt: m.getStatements(null, RDF.TYPE, SKOS.CONCEPT)) {
			IRI iri = (IRI) stmt.getSubject();
			try {
				SkosTerm term = new SkosTerm();
				term.setLabel(getLangString(iri, SKOS.PREF_LABEL));
				catalog.addTerm(iri.stringValue(), term);
				ok++;
			} catch (IOException ioe) {
				LOG.error(ioe.getMessage());
				skip++;
			}			
		}
		LOG.info("Read {} terms, skipping {}", ok, skip);
	}

	/**
	 * Read SKOS terms
	 * 
	 * @param catalog
	 * @throws IOException 
	 */
	private void readOrganizations(Catalog catalog) throws IOException {
		LOG.info("Reading organizations");

		int ok = 0;
		int skip = 0;

		for (Statement stmt: m.getStatements(null, RDF.TYPE, FOAF.ORGANIZATION)) {
			IRI iri = (IRI) stmt.getSubject();
			try {
				Organization org = new Organization();
				String id = iri.stringValue();
				if (id.contains("org.belgif.be")) {
					org.setName(getLangString(iri, FOAF.NAME));
					catalog.addOrganization(id, org);
					ok++;
				}
			} catch (IOException ioe) {
				LOG.error(ioe.getMessage());
				skip++;
			}			
		}
		LOG.info("Read {} organizations, skipping {}", ok, skip);
	}

	/**
	 * Read from input
	 * 
	 * @return simplified DCAT catalog
	 * @throws IOException 
	 */
    public Catalog read() throws IOException {
		if (file != null) {
			try(BufferedReader r = Files.newBufferedReader(file)) {
				m = Rio.parse(r, "http://example.com", fmt);
			}
		} else {
			m = Rio.parse(is, fmt);
		}
		
		Catalog catalog = new Catalog();

		readDatasets(catalog);
		readDataservices(catalog);
		readServes(catalog);
		readRecords(catalog);
		readTerms(catalog);
		readOrganizations(catalog);

		return catalog;
    }

	/**
	 * Constructor
	 * 
	 * @param is input stream
	 * @param mime mime type
	 * @throws IOException 
	 */
	public DcatReader(InputStream is, String mime) throws IOException {
		Optional<RDFFormat> rfmt = Rio.getParserFormatForMIMEType(mime);
		if (!rfmt.isPresent()) {
			throw new IOException("Mime " + mime + " not supported");
		}
		this.fmt = rfmt.get();
		this.file = null;
		this.is = is;
	}
	
	/**
	 * Constructor
	 * 
	 * @param p
	 * @throws IOException 
	 */
	public DcatReader(Path p) throws IOException {
		Optional<RDFFormat> rfmt = Rio.getParserFormatForFileName(p.getFileName().toString());
		if (!rfmt.isPresent()) {
			throw new IOException("File format " + p.getFileName().toString() + " not supported");
		}
		this.fmt = rfmt.get();
		this.file = p;
		this.is = null;
	}
}
