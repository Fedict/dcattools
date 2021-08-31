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
package be.fedict.dcat.scrapers;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Properties;

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
	 * Load properties from resources scraper.properties file
	 * 
	 * @param name name of the scraper
	 * @return properties or null
	 */
	private static Properties loadProperties(String name) throws IOException {
		Properties prop = null;
		String file = BaseScraper.PKG_PREFIX + "/" + name + "/scraper.properties";

		logger.info("Read properties from {}", file);
		try(InputStream is = Main.class.getResourceAsStream(file)) {
			if (is == null) {
				throw new IOException("Could not read from " + file);
			}
			prop = new Properties();
			prop.load(is);
		}
		return prop;
	}

	/**
	 * Load an configure a scraper
	 *
	 * @param prop propertie for additional configuration
	 */
	private static BaseScraper configureScraper(Properties prop) {
		BaseScraper s = null;

		try {
			String cname = prop.getProperty(BaseScraper.PROP_PREFIX + ".classname");
			Class<? extends BaseScraper> c = Class.forName(cname).asSubclass(BaseScraper.class);
			s = c.getConstructor(Properties.class).newInstance(prop);
		} catch (ClassNotFoundException | InstantiationException | NoSuchMethodException
				| IllegalAccessException | InvocationTargetException ex) {
			logger.error("Scraper class could not be loaded", ex);
		}
		return s;
	}

	/**
	 * Main
	 *
	 * @param args
	 */
	public static void main(String[] args) {
		logger.info("-- START --");
		if (args.length < 2) {
			logger.warn("No scraper specified");
			logger.info("Usage: name [true|false] <data-directory>");
			exit(-1);
		}

		String name = args[0];
		boolean scrape = Boolean.valueOf(args[1]); // false = don't scrape but convert from cache

		Properties prop = null;
		try {
			prop = loadProperties(name);
		} catch (IOException ioe) {
			logger.error("Could not read properties for {}", name);
			exit(-2);
		}
		
		String dir = (args.length == 3) ? args[2] : ".";
		String dataDir = String.join(File.separator, dir, "data", name);
		String cache = String.join(File.separator, dataDir, "cache");

		prop.setProperty(BaseScraper.PROP_PREFIX + ".cache", cache);		

		// find and load specific scraper
		try (BaseScraper scraper = configureScraper(prop)) {
			if (scraper != null) {
				// output file
				if (scrape) {
					scraper.scrape();
				}
				scraper.generateDcat();
				scraper.enhance();
					
				String outfile = String.join(File.separator, dataDir, scraper.getName() + ".nt");
				logger.info("Write results to {}", outfile);
				try (BufferedWriter bw = Files.newBufferedWriter(Paths.get(outfile), StandardCharsets.UTF_8)) {
					scraper.writeDcat(bw);
				} catch (IOException ex) {
					logger.error("Error while writing to {}", ex, outfile);
					exit(-3);
				}
			}
		} catch (IOException ex) {
			logger.error("Error while scraping", ex);
			exit(-4);
		}
	}
}
