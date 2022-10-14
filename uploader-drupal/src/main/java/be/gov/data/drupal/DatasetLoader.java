/*
 * Copyright (c) 2022, FPS BOSA DG DT
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
package be.gov.data.drupal;


import be.gov.data.drupal.dao.Dataset;
import be.gov.data.drupal.dao.Distribution;
import be.gov.data.drupal.dao.Term;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.URL;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.vocabulary.DCAT;
import org.eclipse.rdf4j.model.vocabulary.DCTERMS;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.Rio;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *  Load and retrieve datasets to/from Drupal 9
 * 
 * @author Bart Hanssens
 */
public class DatasetLoader extends AbstractLoader {
	private final static Logger LOG = LoggerFactory.getLogger(DatasetLoader.class);
	
	private DateFormat fmt = new SimpleDateFormat();
	private TaxonomyLoader txl;
	private Map<String,Term> terms;

	/**
	 * 
	 * @throws IOException 
	 */
	private void cacheTerms() throws IOException {
		terms = new HashMap<>();
		for(String taxo: List.of("file_types", "licenses", "organizations", "themes")) {
			LOG.info("Caching taxonomy " + taxo);
			txl.getAllTerms(taxo).forEach(t -> terms.put(t.subject().toString(), t));
		}
	}

	/**
	 * Load datasets from DCAT-AP file
	 * 
	 * @param file SKOS file in Turtle format
	 * @return RDF model
	 * @throws IOException 
	 */
	private static Model loadDcat(File file) throws IOException {
		try(FileInputStream fis = new FileInputStream(file)) {
			return Rio.parse(fis, RDFFormat.NTRIPLES, (Resource) null);
		}
	}
	
	/**
	 * Return RDF resources from filtered model
	 * 
	 * @param m filtered model
	 * @return list of RDF resources
	 */
	private List<Resource> toResources(Model m) {
		return m.objects()
				.stream()
				.map(Resource.class::cast)
				.collect(Collectors.toList());
	}

	/**
	 * Return RDF resource or null from filtered model
	 * 
	 * @param m filtered model
	 * @return resource or null
	 */
	private Resource toResource(Model m) {
		return m.objects()
				.stream()
				.map(Resource.class::cast)
				.findFirst()
				.orElse(null);
	}

	/**
	 * Return map from filtered model, with language code as key
	 * 
	 * @param m filtered model
	 * @return map
	 */
	private Map<String,String> toNames(Model m) {
		return m.objects()
				.stream()
				.map(Literal.class::cast)
				.collect(Collectors.toMap(l -> l.getLanguage().orElse("und"), l -> l.stringValue()));
	}

	/**
	 * Return string value or null from filtered model
	 * 
	 * @param m filtered model
	 * @return string or null
	 */
	private String toName(Model m) {
		return m.objects()
				.stream()
				.map(Literal.class::cast)
				.map(Literal::stringValue)
				.findFirst()
				.orElse(null);
	}

	/**
	 * Return date from filtered model
	 * 
	 * @param m filtered model
	 * @return date or null
	 */
	private Date toDate(Model m) {
		try {
			return fmt.parse(toName(m));
		} catch (ParseException ex) {
			return null;
		}
	}

	private List<UUID> toUUIDs(Model m) {
		List<UUID> uuids = new ArrayList<>();

		m.objects()
			.stream()
			.map(IRI.class::cast)
			.map(IRI::stringValue)
			.forEach(s -> { 
				Term t = terms.get(s);
				if (t == null) {
					LOG.warn("Could not map {}", s);
				} else {
					uuids.add(t.drupalID());
				}
			});	
		return uuids;
	}

	/**
	 * Get UUID or null from filtered model
	 * 
	 * @param m
	 * @return UUID or null
	 */
	private UUID toUUID(Model m) {
		Optional<String> iri = m.objects()
			.stream()
			.map(IRI.class::cast)
			.map(IRI::stringValue)
			.findFirst();
		
		if (iri.isPresent()) {
			Term t = terms.get(iri.get());
			if (t == null) {
				LOG.warn("Could not map {}", iri);
			} else {
				return t.drupalID();
			}
		}
		return null;
	}

	/**
	 * Build a distribution DAO from a model
	 * 
	 * @param m RDF model
	 * @param id IRI of the dataset
	 * @return 
	 */
	private Distribution buildDist(Model m, Resource iri) {
		return null;
	}

	/**
	 * Build a dataset DAO from a model
	 * 
	 * @param m RDF model
	 * @param id IRI of the dataset
	 * @return 
	 */
	private Dataset buildDataset(Model m, IRI iri) {
		String id = toName(m.filter(iri, DCTERMS.IDENTIFIER, null));
		Map<String,String> title = toNames(m.filter(iri, DCTERMS.TITLE, null));
		Map<String,String> desc = toNames(m.filter(iri, DCTERMS.DESCRIPTION, null));
		Map<String,String> author = toNames(m.filter(iri, DCTERMS.CREATOR, null));

		Date created = toDate(m.filter(iri, DCTERMS.CREATED, null));
		Date modified = toDate(m.filter(iri, DCTERMS.MODIFIED, null));
		m.filter(iri, DCTERMS.SPATIAL, null);
		
		Resource t = toResource(m.filter(iri, DCTERMS.TEMPORAL, null));
		Date start = (t != null) ? toDate(m.filter(t, DCAT.START_DATE, null)) : null;
		Date end = (t != null) ? toDate(m.filter(t, DCAT.END_DATE, null)) : null;	

		UUID organization = toUUID(m.filter(iri, DCTERMS.PUBLISHER, null));
		UUID geography = toUUID(m.filter(iri, DCTERMS.SPATIAL, null));		
		UUID frequency = toUUID(m.filter(iri, DCTERMS.ACCRUAL_PERIODICITY, null));
		UUID license = toUUID(m.filter(iri, DCTERMS.LICENSE, null));
		List<UUID> themes = toUUIDs(m.filter(iri, DCAT.THEME, null));
	
		List<Resource> resources = toResources(m.filter(iri, DCAT.HAS_DISTRIBUTION, null));
		List<Distribution> dists = resources.stream().map(d -> buildDist(m, d)).collect(Collectors.toList());
 
		Map<String,String> keywords = null;
		List<URL> pages = null;

		return new Dataset(iri, id, title, desc, author,
						created, modified, start, end,
						organization, geography, frequency, license, 
						themes, keywords,
						pages,
						dists,
						null);
	}
		
	public void load(File file) throws IOException {
		cacheTerms();
		Model m = loadDcat(file);
		
		Set<Resource> subjs = m.filter(null, RDF.TYPE, DCAT.DATASET).subjects();
		LOG.info("Found {} datasets", subjs);
		
		for(Resource subj: subjs) {
			buildDataset(m, (IRI) subj);
		}
	}

	/**
	 * Constructor
	 * 
	 * @param website base url of the website
	 * @param user user name
	 * @param pass password
	 */
	public DatasetLoader(String website, String user, String pass) {
		super(website, user, pass);
		
		txl = new TaxonomyLoader(website, user, pass);
	}
}
