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
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;

import org.eclipse.rdf4j.repository.RepositoryException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Bart Hanssens <bart.hanssens@fedict.be>
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
	 * Get required property
	 *
	 * @param prop properties
	 * @param name unprefixed property
	 * @return value of the property
	 * @throws IOException if property is empty
	 */
	private static String getRequired(Properties prop, String name) throws IOException {
		String p = prop.getProperty(Scraper.PROP_PREFIX + "." + name, "");
		if (p.isEmpty()) {
			throw new IOException("Property missing: " + p);
		}
		return p;
	}

	/**
	 * Load a scraper and scrape the site.
	 *
	 * @param cache location of cache file
	 * @param prop propertie for additional configuration
	 */
	private static Scraper getScraper(String cache, Properties prop) {
		Scraper s = null;

		try {
			String cname = getRequired(prop, "classname");
			Class<? extends Scraper> c = Class.forName(cname).asSubclass(Scraper.class);

			String url = getRequired(prop, "url");

			s = c.getConstructor(File.class, URL.class).newInstance(new File(cache), new URL(url));

			s.setDefaultLang(getRequired(prop, "deflanguage"));
			s.setAllLangs(getRequired(prop, "languages").split(","));

			String delay = prop.getProperty(Scraper.PROP_PREFIX + ".http.delay", "500");
			s.setDelay(Integer.valueOf(delay));

			s.setProperties(prop, Scraper.PROP_PREFIX);
		} catch (ClassNotFoundException | InstantiationException | NoSuchMethodException
				| IllegalAccessException | InvocationTargetException ex) {
			logger.error("Scraper class could not be loaded", ex);
			exit(-3);
		} catch (MalformedURLException ex) {
			logger.error("Base URL invalid", ex);
			exit(-3);
		} catch (IOException ex) {
			logger.error("Property not found", ex);
		}
		return s;
	}

	/**
	 * Write result of scrape to DCAT file
	 *
	 * @param outFile path
	 * @param scraper scraper instance
	 */
	private static void writeDcat(Path outFile, Scraper scraper) throws IOException {
		try (BufferedWriter bw = Files.newBufferedWriter(outFile, StandardCharsets.UTF_8)) {
			scraper.writeDcat(bw);
		} catch (IOException ex) {
			logger.error("Error writing output file {}", outFile, ex);
			exit(-5);
		} catch (RepositoryException ex) {
			logger.error("Repository error", ex);
			exit(-6);
		}
	}

	/**
	 * Main
	 *
	 * @param args
	 */
	public static void main(String[] args) {
		logger.info("-- START --");
		if (args.length == 0) {
			System.err.println("Usage: name <data-directory>");
			logger.error("No scraper specified");
			exit(-1);
		}

		String name = args[0];
		Properties prop = new Properties();

		String dir = (args.length == 2) ? args[1] : ".";

		// load properties from resources
		try(InputStream is = Scraper.class.getResourceAsStream("/be/fedict/dcat/scrapers/" + name + "/scraper.properties")) {
			prop.load(is);
		} catch (IOException ex) {
			logger.error("I/O Exception while reading {}", name, ex);
			exit(-2);
		}

		// create data directory if not exists
		String dataDir = String.join(File.separator, dir, "data", name);
		try {
			Files.createDirectories(Paths.get(dataDir));
		} catch (IOException ex) {
			logger.error("Could not create data directory {}", dataDir);
			exit(-3);
		}
	
		String cache = String.join(File.separator, dataDir, "cache");

		// find and load specific scraper
		Scraper scraper = getScraper(cache, prop);
		if (scraper == null) {
			logger.error("Scraper not found");
			exit(-4);			
		}

		// output file
		String outfile = String.join(File.separator, dataDir, scraper.getName() + ".nt");

		try {
			scraper.scrape();
			writeDcat(Paths.get(outfile), scraper);
		} catch (IOException ex) {
			logger.error("Error while scraping", ex);
			exit(-5);
		}
	}
}
