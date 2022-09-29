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
package be.gov.data.uploader.skos;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Set;
import org.eclipse.rdf4j.model.util.Values;
import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.Test;

/**
 *
 * @author Bart.Hanssens
 */
public class TaxonomyLoaderTest {
	@Test
	void loadTerms() throws URISyntaxException, IOException {
		File file = new File(this.getClass().getResource("/filetypes.ttl").toURI());
		TaxonomyLoader loader = new TaxonomyLoader();
		Set<Term> terms = loader.parse(file);
		
		assertEquals(1, terms.size());
		assertEquals("http://publications.europa.eu/resource/authority/file-type/CSV", 
					terms.iterator().next().subject().toString());
	}

	@Test
	void getTerm() throws IOException {
		TaxonomyLoader loader = new TaxonomyLoader();
		Term term = loader.getTerm("https://datagovbe.rovin.be", "file_types", Values.iri("http://data.gov.be/bin"));

		assertEquals(1, term.values().size());
		assertEquals("BIN", term.values().get("und"));
	}

	@Test
	void getAllTerms() throws IOException {
		TaxonomyLoader loader = new TaxonomyLoader();
		Set<Term> allTerms = loader.getAllTerms("https://datagovbe.rovin.be", "file_types");
		assertEquals(2, allTerms.size());
	}
}
