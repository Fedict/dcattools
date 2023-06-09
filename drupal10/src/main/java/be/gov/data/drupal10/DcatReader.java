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
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Value;
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
 *
 * @author Bart.Hanssens
 */
public class DcatReader implements AutoCloseable {
	private final Repository repo;

	@Override
	public void close() throws Exception {
		repo.shutDown();
	}
/*
	public Map<IRI,Set<Value>> getValues(IRI subj) {
		try (RepositoryConnection conn = repo.getConnection()) {
			Model m = QueryResults.asModel(conn.getStatements(subj, null, null));
			return m.stream()
					.collect(
						Collectors.groupingBy(Statement::getPredicate, 
							Collectors.mapping(Statement::getObject, 
								Collectors.toSet())));
		}
	}
*/
	
	public Model getValues(IRI subj) {
		try (RepositoryConnection conn = repo.getConnection()) {
			return QueryResults.asModel(conn.getStatements(subj, null, null));
		}
	}

	private Set<IRI> getIDs(IRI dtype) {
		try (RepositoryConnection conn = repo.getConnection()) {
			return conn.getStatements(null, RDF.TYPE, dtype)
				.stream()
				.map(s -> (IRI) s.getSubject())
				.collect(Collectors.toSet());
		}
	}

	public Function<Value,String> toString = v -> v.stringValue();
	public Function<Value,URI> toURI = v -> URI.create(v.stringValue());

	public <T> Set<T> getLiterals(Model m, IRI key, String lang, Function<Value,T> func) {
		return m.filter(null, key, null).objects()
				.stream()
				.filter(Literal.class::isInstance)
				.map(Literal.class::cast)
				.filter(v -> v.getLanguage().orElse("").equals(lang))
				.map(func)
				.collect(Collectors.toSet());
	}

	public <T> T getLiteral(Model m, IRI key, String lang, Function<Value,T> func) {
		return getLiterals(m, key, lang, func).stream().findFirst().orElse(null);
	}

	public void getDatasets() {
		Set<IRI> ids = getIDs(DCAT.DATASET);
		for(IRI id: ids) {
			Model m = getValues(id);
			getLiteral(m, DCTERMS.TITLE, null, toString);
			getLiteral(m, DCTERMS.DESCRIPTION, null, toString);
			getLiterals(m, DCAT.KEYWORD, null, toString);
			getLiterals(m, DCAT.THEME, null, toURI);
		}
	}

	public DcatReader(File file) throws IOException {
		Sail mem = new MemoryStore();
		repo = new SailRepository(mem);
		
		try (RepositoryConnection conn = repo.getConnection()) {
			conn.add(file, (Resource) null);
		}
	}

}
