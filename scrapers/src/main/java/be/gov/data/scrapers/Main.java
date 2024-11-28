/*
 * Copyright (c) 2015, FPS BOSA DG DT
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
package be.gov.data.scrapers;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.concurrent.Callable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

/**
 * Executes a scraper, using the per-scraper properties file from resource jar
 * 
 * @author Bart Hanssens
 */
@Command(name = "scraper", mixinStandardHelpOptions = true, 
					description = "Scrapes open data sources and turn metadata into DCAT-AP.")
public class Main implements Callable<Integer> {
	@Option(names = {"-d", "--dir"}, description = "Output directory", defaultValue=".")
    private String dir;

	@Option(names = {"-n", "--name"}, description = "Name of datasource / scraper", required=true)
    private String name;

	@Option(names = {"-r", "--raw"}, description = "Raw output, don't apply scripts", defaultValue="false")
    private Boolean raw;

	private final static Logger LOG = LoggerFactory.getLogger(Main.class);


	@Override
	public Integer call() throws Exception {
		// find and load specific scraper
		try (BaseScraper scraper = BaseScraperFactory.getConfiguredScraper(name, dir)) {
			scraper.setRawOutput(raw);
			scraper.scrape();
			scraper.generateDcat();
			scraper.writeDcat();
			LOG.info("Done writing");
		} catch (IOException ex) {
			LOG.error("Error while scraping", ex);
			return -1;
		}
		return 0;
	}

	/**
	 * Main
	 *
	 * @param args
	 */
	public static void main(String[] args) {
		LOG.info("-- START --");
		
		CommandLine cl = new CommandLine(new Main());
		cl.setDefaultValueProvider(new CommandLine.PropertiesDefaultProvider());
		int exitCode = cl.execute(args);

		LOG.info("-- STOP --");

		System.exit(exitCode);
	}
}
