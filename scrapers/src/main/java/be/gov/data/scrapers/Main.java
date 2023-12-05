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

import java.io.File;
import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Executes a scraper, using the per-scraper properties file from resource jar
 * 
 * @author Bart Hanssens
 */
public class Main {
	private final static Logger logger = LoggerFactory.getLogger(Main.class);

	/**
	 * Exit cleanly
	 *
	 * @param code return code
	 */
	private static void exit(int code) {
		logger.info("-- STOP --");
		System.exit(code);
	}

	/**
	 * Main
	 *
	 * @param args
	 */
	public static void main(String[] args) {
		logger.info("-- START --");
		if (args.length == 0) {
			logger.warn("No scraper specified");
			logger.info("Usage: name <data-directory>");
			exit(-1);
		}

		String name = args[0];
		String dir = (args.length == 2) ? args[1] : ".";
		String dataDir = String.join(File.separator, dir, "data", name);

		// find and load specific scraper
		try (BaseScraper scraper = BaseScraperFactory.getConfiguredScraper(name, dataDir)) {
			scraper.scrape();
			scraper.generateDcat();
			scraper.writeDcat();
		} catch (IOException ex) {
			logger.error("Error while scraping", ex);
			exit(-5);
		}
	}
}
