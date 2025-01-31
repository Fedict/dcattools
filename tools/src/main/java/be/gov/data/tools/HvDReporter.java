/*
 * Copyright (c) 2025, FPS BOSA DG SD
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
package be.gov.data.tools;

import be.gov.data.dcatlib.DcatReader;
import be.gov.data.dcatlib.model.Catalog;
import be.gov.data.dcatlib.model.DataResource;
import be.gov.data.dcatlib.model.Dataservice;
import be.gov.data.dcatlib.model.Dataset;
import be.gov.data.dcatlib.model.Organization;
import be.gov.data.dcatlib.model.SkosTerm;

import com.opencsv.CSVWriter;
import com.opencsv.CSVWriterBuilder;
import com.opencsv.ICSVWriter;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.Map;
import java.util.stream.Collectors;
import org.eclipse.rdf4j.model.IRI;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Write a basic report on High Value Datasets (and DataServices)
 * 
 * @author Bart.Hanssens
 */
public class HvDReporter {
	private final static Logger LOG = LoggerFactory.getLogger(HvDReporter.class);

	private final static String[] HEADER =
		new String[]{ "Type","URI","Identifier","Category","CategoryName",
			"TitleNL", "TitleFR", "TitleEN",
			"PublisherID","PublisherNameNL", "PublisherNameFR",
			"LicenseID",
			"LandingPage",
			"Download/Endpoint",
			"Contact"};

	/**
	 * Write one line of HvD
	 * 
	 * @param <T>
	 * @param type dataset or services
	 * @param resources list of dataset or dataservices
	 * @param terms skos terms
	 * @param orgs organizations
	 * @param writer 
	 */
	private static <T extends DataResource> void writeLines(String type, Collection<T> resources, 
					Map<String,SkosTerm> terms, Map<String,Organization> orgs, ICSVWriter writer) {
		int count = 0;

		for(DataResource d: resources) {
			if (!d.isHvd()) {
				continue;
			}
			count++;
			writer.writeNext(new String[]{type, "", d.getId(),
					d.getHvDCategories().stream().map(IRI::stringValue).collect(Collectors.joining("\n")),
					d.getHvDCategories().stream().map(IRI::stringValue)
						.map(t -> (terms.get(t) != null) ? terms.get(t).getLabel("en") : "")
						.collect(Collectors.joining("\n")),
					d.getTitle("nl"), d.getTitle("fr"), d.getTitle("en"),
					d.getPublisher().stringValue(),
					orgs.getOrDefault(d.getPublisher().stringValue(), new Organization()).getName("nl"),
					orgs.getOrDefault(d.getPublisher().stringValue(), new Organization()).getName("fr"),
					(d.getLicense() != null) ? d.getLicense().stringValue() : null,
					d.getLandingPage().values().stream().map(IRI::stringValue).collect(Collectors.joining("\n")),
					d.getDownloadURLs("").stream().map(IRI::stringValue).collect(Collectors.joining("\n")),
					(d.getContactAddr("") != null) ? d.getContactAddr("").stringValue() : null
			});	
		}
		LOG.info("HvD {}: {}", type, count);
	}

	/**
	 * Create CSV report
	 * 
	 * @param reader
	 * @param writer
	 * @throws IOException 
	 */
	private static void createReport(DcatReader reader, ICSVWriter writer) throws IOException {
		Catalog cat = reader.read();
			
		try (writer) {	
			writer.writeNext(HEADER);
			writeLines("Dataset", cat.getDatasets().values(), cat.getTerms(), cat.getOrganizations(), writer);
			writer.flush();
			writeLines("Dataservice", cat.getDataservices().values(), cat.getTerms(), cat.getOrganizations(), writer);
			writer.flush();
		}
	}

	/**
	 * Main program
	 *
	 * @param args
	 */
	public static void main(String[] args) {
		LOG.info("-- START --");
		if (args.length < 2) {
			LOG.error("No input or output file");
			System.exit(-1);
		}
		
		DcatReader reader = null;
		try {
			reader = new DcatReader(Paths.get(args[0]));
		} catch (IOException ioe) {
			LOG.error("Could not read {}: {} ", args[0], ioe.getMessage());
			System.exit(-2);
		}

		ICSVWriter writer = null;
		try {
			writer = new CSVWriterBuilder(Files.newBufferedWriter(Paths.get(args[1])))
										.withSeparator(';').withQuoteChar('"').build();
			createReport(reader, writer);
		} catch (IOException ioe) {
			LOG.error("Could not write to {} : {}", args[1], ioe.getMessage());
			System.exit(-3);
		}
	}
}
