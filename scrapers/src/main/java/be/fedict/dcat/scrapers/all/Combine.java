/*
 * Copyright (c) 2016, FPS BOSA DG DT
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
package be.fedict.dcat.scrapers.all;

import be.fedict.dcat.helpers.Storage;
import be.fedict.dcat.scrapers.BaseScraper;
import be.fedict.dcat.scrapers.Cache;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Properties;
import java.util.stream.Collectors;

import org.eclipse.rdf4j.repository.RepositoryException;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.RDFParseException;

/**
 * Combine data of all scraped sources into 1
 *
 * @see http://data.gov.be
 * @author Bart Hanssens
 */
public class Combine extends BaseScraper {

	@Override
	public void generateDcat(Cache cache, Storage store) throws IOException {
		Path root = Paths.get(".");

		List<File> files = Files.walk(root).map(Path::toFile)
								.filter(File::isFile)
								.filter(f -> f.toString().endsWith(".nt"))
								.filter(f -> !f.toString().endsWith("enhanced.nt"))
								.filter(f -> !f.getParentFile().toString().equals("all"))
								.collect(Collectors.toList());
		for (File f: files) {
			logger.info("Reading {}", f);
			// Load turtle file into store
			try (InputStream in = new BufferedInputStream(new FileInputStream(f))) {
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
	public Combine(Properties prop) throws IOException {
		super(prop);
		setName("all");
	}

}
