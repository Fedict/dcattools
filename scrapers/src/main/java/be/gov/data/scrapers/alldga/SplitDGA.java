/*
 * Copyright (c) 2026, FPS BOSA
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
package be.gov.data.scrapers.alldga;

import be.gov.data.dcat.helpers.Storage;
import be.gov.data.dcat.vocab.DATAGOVBE;
import be.gov.data.scrapers.BaseScraper;
import be.gov.data.scrapers.Cache;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import java.util.Properties;
import java.util.stream.Stream;

import org.eclipse.rdf4j.repository.RepositoryException;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.RDFParseException;

/**
 * Split the "protected data" from the full export for the EDP
 *
 * @see http://data.gov.be
 * @author Bart Hanssens
 */
public class SplitDGA extends BaseScraper {
	@Override
	public void generateDcat(Cache cache, Storage store) throws IOException {
		Path root = Path.of(getDataDir()).getParent();

		Optional<File> file;
		try(Stream<Path> path = Files.walk(root)) {
			file = path.map(Path::toFile)
						.filter(File::isFile)
						.filter(f -> f.getName().equals("all.nt"))
						.filter(f -> !f.getParentFile().toString().equals("all"))
						.findFirst();
		}

		if (file.isPresent()) {
			LOG.info("Reading {}", file.get());
			// Load turtle file into store
			try (InputStream in = new BufferedInputStream(new FileInputStream(file.get()))) {
				store.add(in, RDFFormat.NTRIPLES);
			} catch (RDFParseException ex) {
				throw new RepositoryException(ex);
			}
		}
		generateCatalog(store);
	}

	@Override
	public void scrape() throws IOException {
		// empty on purpose
	}

	/**
	 * Constructor
	 *
	 * @param prop
	 * @throws IOException
	 */
	public SplitDGA(Properties prop) throws IOException {
		super(prop);
		setName("alldga");
	}
}
