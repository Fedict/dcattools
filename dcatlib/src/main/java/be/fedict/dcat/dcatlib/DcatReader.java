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

package be.fedict.dcat.dcatlib;

import be.fedict.dcat.dcatlib.model.Catalog;
import be.fedict.dcat.dcatlib.model.DataResource;
import be.fedict.dcat.dcatlib.model.Dataservice;
import be.fedict.dcat.dcatlib.model.Dataset;

import java.io.IOException;
import java.io.InputStream;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.vocabulary.DCAT;
import org.eclipse.rdf4j.model.vocabulary.DCTERMS;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.Rio;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Read DCAT-AP v2 input into a simplified model for data.gov.be
 * 
 * @author Bart Hanssens
 */
public class DcatReader {
	private final static Logger LOG = LoggerFactory.getLogger(DcatReader.class);
	private final static SimpleDateFormat DATE_FMT = new SimpleDateFormat();
	
	private Model m;
	
	private <T extends Value> T getValue(IRI subj, IRI pred, Class<T> clazz) {
		T val = null;
		for(Statement s: m.getStatements(subj, pred, null)) {
			val = (T) s.getObject();
		}
		return val;
	}

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
			throw new IOException("More than 1 value for " + subj + " " + pred);
		}
		return objects.iterator().next();
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
		try {
			return DATE_FMT.parse(value.stringValue());
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
			throw new IOException("Not a IRI " + value);
		}
		return (IRI) value;
	}

	/**
	 * Get a set of IRIs
	 * 
	 * @param subj subject
	 * @param pred predicate
	 * @return set of literals
	 * @throws IOException when not IRIs
	 */
	private Set<IRI> getIRIs(Resource subj, IRI pred) throws IOException {
		Set<IRI> values = new HashSet<>();
		for(Statement s: m.getStatements(subj, pred, null)) {
			Value value = s.getObject();
			if (! (value instanceof IRI)) {
				throw new IOException("Not a IRI " + value);
			}
			values.add((IRI) value);
		}
		return values;
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
	 * Get a single string per language
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
			if (!lang.isPresent()) {
				throw new IOException("No lang label " + subj + " " + pred);
			}
			String prev = map.put(lang.get(), v.stringValue());
			if (prev != null) {
				throw new IOException("Existing value " + lang.get() + " " + subj + " " + pred);		
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
			if (!lang.isPresent()) {
				throw new IOException("No lang label " + subj + " " + pred);
			}
			Set<String> list = map.get(lang.get());
			if (list == null) {
				map.put(lang.get(), new HashSet<>());
				list = map.get(lang.get());
			}
			list.add(v.stringValue());
		}
		return map;
	}

	private DataResource readResource(IRI iri) throws IOException {
		String id = getString(iri, DCTERMS.IDENTIFIER);
		if (id == null || id.isEmpty()) {
			throw new IOException("No identifier for " + iri);
		}
		DataResource d = new DataResource();

		d.setId(id);

		d.setTitle(getLangString(iri, DCTERMS.TITLE));
		d.setDescription(getLangString(iri, DCTERMS.DESCRIPTION));
		d.setKeywords(getLangStringList(iri, DCAT.KEYWORD));
		
		d.setThemes(getIRIs(iri, DCAT.THEME));
		d.setCreator(getIRI(iri, DCTERMS.CREATOR));
		d.setPublisher(getIRI(iri, DCTERMS.PUBLISHER));
		d.setAccrualPeriodicity(getIRI(iri, DCTERMS.ACCRUAL_PERIODICITY));
		d.setSpatial(getIRI(iri, DCTERMS.SPATIAL));
		d.setLicense(getIRI(iri, DCTERMS.LICENSE));

		d.setIssued(getDate(iri, DCTERMS.CREATED));
		d.setModified(getDate(iri, DCTERMS.MODIFIED));
		Resource res = getResource(iri, DCTERMS.TEMPORAL);
		if (res != null) {
			d.setStartDate(getDate(res, DCAT.START_DATE));
			d.setEndDate(getDate(res, DCAT.END_DATE));
		}

		return d;
	}
	
	private void readDatasets(Catalog catalog) throws IOException {
		for (Statement stmt: m.getStatements(null, RDF.TYPE, DCAT.DATASET)) {
			IRI iri = (IRI) stmt.getSubject();
			Dataset d = (Dataset) readResource(iri);
			
			catalog.addDataset(d.getId(), d);
		}
	}

	private void readDataservices(Catalog catalog) throws IOException {
		for (Statement stmt: m.getStatements(null, RDF.TYPE, DCAT.DATA_SERVICE)) {
			IRI iri = (IRI) stmt.getSubject();
			Dataservice d = (Dataservice) readResource(iri);
			if (d == null) {
				continue;
			}

			catalog.addDataservice(d.getId(), d);
		}
	}
	
	/**
	 * Read from input
	 * 
	 * @param is inputstream
	 * @return simplified DCAT catalog
	 * @throws IOException 
	 */
    public Catalog read(InputStream is) throws IOException {
		m = Rio.parse(is, "http://example.com", RDFFormat.NTRIPLES);

		Catalog catalog = new Catalog();
		
		readDatasets(catalog);
		readDataservices(catalog);

		return catalog;
    }
}
