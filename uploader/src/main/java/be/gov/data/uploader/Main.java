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
package be.gov.data.uploader;

import be.gov.data.helpers.Storage;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Properties;

import org.eclipse.rdf4j.repository.RepositoryException;
import org.eclipse.rdf4j.rio.RDFParseException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Main class. Updates data.gov.be portal using DCAT files.
 *
 * @author Bart Hanssens <bart.hanssens@fedict.be>
 */
public class Main {

	private final static Logger LOGGER = LoggerFactory.getLogger(Main.class);

	private final static Properties prop = new Properties();
	private static Storage store = null;

	/**
	 * Exit cleanly.
	 *
	 * @param code return code
	 */
	private static void exit(int code) {
		if (store != null) {
			try {
				store.shutdown();
			} catch (RepositoryException ex) {
				LOGGER.error("Error shutting down repository", ex);
			}
		}
		LOGGER.info("-- STOP --");
		System.exit(code);
	}

	/**
	 * Read RDF file.
	 */
	private static void readRDF() {
		store = new Storage();
		try {
			store.startup();
		} catch (RepositoryException ex) {
			LOGGER.error("Error starting repository", ex);
			exit(-3);
		}

		String rdfin = prop.getProperty(Drupal.PROP_PREFIX + ".rdfin");
		try (BufferedReader r
			= Files.newBufferedReader(Paths.get(rdfin), StandardCharsets.UTF_8)) {
			store.read(r);
		} catch (IOException ex) {
			LOGGER.error("Could not read from rdf file {}", rdfin, ex);
			exit(-4);
		} catch (RepositoryException | RDFParseException ex) {
			LOGGER.error("Repository error", ex);
			exit(-4);
		}
	}

	/**
	 * Set proxy, if needed.
	 *
	 * @param d Drupal instance
	 */
	private static void setProxy(Drupal d) {
		String proxy = System.getProperty("http.proxyHost", "");
		String port = System.getProperty("http.proxyPort", "");
		if (!proxy.isEmpty()) {
			d.setProxy(proxy, Integer.parseInt(port));
		}
	}

	/**
	 * Update Drupal site
	 */
	public static void drupal() {
		String u = prop.getProperty(Drupal.PROP_PREFIX + ".drupal");
		URL url = null;

		try {
			url = new URL(u);
		} catch (MalformedURLException ex) {
			LOGGER.error("Error setting Drupal home to {}", u, ex);
			exit(-5);
		}

		String l = prop.getProperty(Drupal.PROP_PREFIX + ".languages");
		String[] langs = l.split(",");
		Drupal d = new Drupal(url, langs, store);

		setProxy(d);

		String user = prop.getProperty(Drupal.PROP_PREFIX + ".user");
		String pass = prop.getProperty(Drupal.PROP_PREFIX + ".pass");
		String userid = prop.getProperty(Drupal.PROP_PREFIX + ".userid");

		d.setUserPassID(user, pass, userid);

		try {
			d.update();
		} catch (IOException ex) {
			LOGGER.error("IO exception while updating", ex);
			exit(-5);
		} catch (RepositoryException ex) {
			LOGGER.error("Repository exception while updating", ex);
			exit(-6);
		}
	}

	/**
	 * Main
	 *
	 * @param args
	 */
	public static void main(String[] args) {
		LOGGER.info("-- START --");
		if (args.length == 0) {
			LOGGER.error("No config file");
			exit(-1);
		}

		File config = new File(args[0]);
		try {
			LOGGER.debug("Using config file " + config);
			prop.load(new FileInputStream(config));
		} catch (IOException ex) {
			LOGGER.error("I/O Exception while reading {}", config, ex);
			exit(-2);
		}

		readRDF();

		drupal();
	}
}
