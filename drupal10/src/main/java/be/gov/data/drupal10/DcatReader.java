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
package be.gov.data.drupal10;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Set;
import java.util.stream.Collectors;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.vocabulary.DCAT;
import org.eclipse.rdf4j.model.vocabulary.DCTERMS;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.query.QueryResults;
import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.repository.sail.SailRepository;
import org.eclipse.rdf4j.sail.Sail;
import org.eclipse.rdf4j.sail.memory.MemoryStore;

/**
 * Read a dcat file
 * 
 * @author Bart Hanssens
 */
public class DcatReader implements AutoCloseable {
	private final Repository repo;

	@Override
	public void close() throws Exception {
		repo.shutDown();
	}

	/**
	 * Get properties for a specific ID
	 * 
	 * @param subj
	 * @return RDF model
	 */
	private Model getValues(IRI subj) {
		try (RepositoryConnection conn = repo.getConnection()) {
			return QueryResults.asModel(conn.getStatements(subj, null, null));
		}
	}

	/**
	 * Get all IDs with a specific RDF class
	 * 
	 * @param dtype RDF class
	 * @return set of IRIs 
	 */
	private Set<IRI> getIDs(IRI dtype) {
		try (RepositoryConnection conn = repo.getConnection()) {
			return conn.getStatements(null, RDF.TYPE, dtype)
				.stream()
				.map(s -> (IRI) s.getSubject())
				.collect(Collectors.toSet());
		}
	}

	private URI toURI(IRI iri) {
		try {
			return new URI(iri.stringValue());
		} catch (URISyntaxException ex) {
			return null;
		}
	}

	private Set<URI> getURIs(Model m, IRI key) {
		return m.filter(null, key, null).objects()
				.stream()
				.filter(IRI.class::isInstance)
				.map(IRI.class::cast)
				.map(v -> toURI(v))
				.filter(v -> v != null)
				.collect(Collectors.toSet());
	}

	/**
	 * Get non-blank literals in a specific language
	 *
	 * @param m RDF model
	 * @param prop property
	 * @param lang language code
	 * @return set of strings
	 */
	private Set<String> getLiterals(Model m, IRI prop, String lang) {
		return m.filter(null, prop, null).objects()
				.stream()
				.filter(Literal.class::isInstance)
				.map(Literal.class::cast)
				.filter(v -> v.getLanguage().orElse("").equals(lang))
				.map(Literal::stringValue)
				.filter(v -> !v.isBlank())
				.collect(Collectors.toSet());
	}

	/**
	 * Get a non-blank literal in a specific language or null
	 *
	 * @param m RDF model
	 * @param prop property
	 * @param lang language code
	 * @return set of strings
	 */
	private String getLiteral(Model m, IRI prop, String lang) {
		return getLiterals(m, prop, lang).stream().findFirst().orElse(null);
	}

	/**
	 * Get all datasets
	 */
	public void getDatasets() {
		Set<IRI> ids = getIDs(DCAT.DATASET);
		for(IRI id: ids) {
			Model m = getValues(id);
			getLiteral(m, DCTERMS.TITLE, null);
			getLiteral(m, DCTERMS.DESCRIPTION, null);
			getLiterals(m, DCAT.KEYWORD, null);
			getURIs(m, DCAT.THEME);
			getURIs(m, DCTERMS.CREATOR);
			getURIs(m, DCTERMS.PUBLISHER);			
		}
	}

	/**
	 * Constructor
	 * 
	 * @param file RDF file
	 * @throws IOException 
	 */
	public DcatReader(File file) throws IOException {
		Sail mem = new MemoryStore();
		repo = new SailRepository(mem);
		
		try (RepositoryConnection conn = repo.getConnection()) {
			conn.add(file, (Resource) null);
		}
	}

}
