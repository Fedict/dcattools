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
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.Model;
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
 *
 * @author Bart Hanssens
 */
public class DcatReader {
	private static Logger LOG = LoggerFactory.getLogger(DcatReader.class);
	private Model m;
	
	private <T extends Value> T getValue(IRI subj, IRI pred, Class<T> clazz) {
		T val = null;
		for(Statement s: m.getStatements(subj, pred, null)) {
			val = (T) s.getObject();
		}
		return val;
	}

	private String getString(IRI subj, IRI pred) throws IOException {
		Set<Value> objects = m.filter(subj, pred, null).objects();
		if (objects.size() > 1) {
			throw new IOException();
		}
		return "";
	}

	private <T extends Value> Set<T> getValues(IRI subj, IRI pred, Class<T> clazz) {
		Set<T> values = new HashSet<>();
		for(Statement s: m.getStatements(subj, pred, null)) {
			values.add((T) s.getObject());
		}
		return values;
	}

	private Map<String,String> getLangLiteral(IRI subj, IRI pred) throws IOException {
		Set<Literal> values = getValues(subj, pred, Literal.class);
		Map<String,String> map = new HashMap<>();
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

	private Map<String,Set<String>> getLangLiterals(IRI subj, IRI pred) throws IOException {
		Set<Literal> values = getValues(subj, pred, Literal.class);
		Map<String,Set<String>> map = new HashMap<>();
		for (Literal v: values) {
			Optional<String> lang = v.getLanguage();
			if (!lang.isPresent()) {
				throw new IOException("No lang label " + subj + " " + pred);
			}
			Set<String> list = map.get(lang.get());
			if (list == null) {
				map.put(lang.get(), new HashSet<String>());
				list = map.get(lang.get());
			}
			list.add(v.stringValue());
		}
		return map;
	}

	private DataResource readResource(IRI iri) throws IOException {
		String id = getString(iri, DCTERMS.IDENTIFIER);
		if (id.isEmpty()) {
			throw new IOException("No identifier for " + iri);
		}
		DataResource d = new DataResource();

		d.setId(id);
		d.setTitle(getLangLiteral(iri, DCTERMS.TITLE));
		d.setDescription(getLangLiteral(iri, DCTERMS.DESCRIPTION));
		d.setKeywords(getLangLiterals(iri, DCAT.KEYWORD));

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
	
    public Catalog read(InputStream is) throws IOException {
		m = Rio.parse(is, "http://example.com", RDFFormat.NTRIPLES);

		Catalog catalog = new Catalog();
		
		readDatasets(catalog);
		readDataservices(catalog);

		return catalog;
    }
}
