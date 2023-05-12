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
package be.gov.data.uploader.drupal;

import be.gov.data.drupal.TaxonomyLoader;
import be.gov.data.drupal.dao.Term;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Set;

import org.eclipse.rdf4j.model.util.Values;
import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 *
 * @author Bart.Hanssens
 */
public class TaxonomyLoaderTest {
	private static String site;
	private static String user;
	private static String pass;
	
	@BeforeAll
    public static void setUp() {
		site = System.getProperty("site", "");
		user = System.getProperty("user", "");
		pass = System.getProperty("pass", "");		
    }
	
	@Test
	void loadTermsSimple() throws URISyntaxException, IOException {
		File file = new File(this.getClass().getResource("/filetypes.ttl").toURI());
		TaxonomyLoader loader = new TaxonomyLoader(site, user, pass);
		List<Term> terms = loader.parse(file);
		
		assertEquals(2, terms.size());
	}

	@Test
	void postTermsSimple() throws URISyntaxException, IOException {
		File file = new File(this.getClass().getResource("/filetypes.ttl").toURI());
		TaxonomyLoader loader = new TaxonomyLoader(site, user, pass);
		List<Term> terms = loader.parse(file);
		for(Term t: terms) {
			loader.postTerm("file_types", t);
		}
	}

	@Test
	void loadTermsTree() throws URISyntaxException, IOException {
		File file = new File(this.getClass().getResource("/licenses.ttl").toURI());
		TaxonomyLoader loader = new TaxonomyLoader(site, user, pass);
		List<Term> terms = loader.parse(file);
		
		assertEquals(5, terms.size());
	}

	@Test
	void postTermsTree() throws URISyntaxException, IOException {
		File file = new File(this.getClass().getResource("/licenses.ttl").toURI());
		TaxonomyLoader loader = new TaxonomyLoader(site, user, pass);
		List<Term> terms = loader.parse(file);
		for(Term t: terms) {
			loader.postTerm("licenses", t);
		}
	}

	@Test
	void getTerm() throws IOException {
		TaxonomyLoader loader = new TaxonomyLoader(site, user, pass);
		Term term = loader.getTerm("file_types", Values.iri("http://data.gov.be/bin"));

		assertEquals(1, term.values().size());
		assertEquals("BIN", term.values().get("und"));
	}

	@Test
	void getAllTerms() throws IOException {
		TaxonomyLoader loader = new TaxonomyLoader(site, user, pass);
		Set<Term> allTerms = loader.getAllTerms("file_types");
		assertEquals(2, allTerms.size());
	}
}
