/*
 * Copyright (c) 2023, FPS BOSA
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
package be.gov.data.drupal10;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Processes DCAT-AP v2 file(s) to update data.gov.be open data portal.
 * 
 * @author Bart Hanssens
 */
public class Main {
	private final static Logger LOG = LoggerFactory.getLogger(Main.class);
	
	/**
	 * Exit with error code
	 * 
	 * @param code
	 * @param msg 
	 */
	private static void exit(int code, String msg) {
		LOG.error(msg);
		System.exit(code);
	}

	/**
	 * Load properties from file
	 * 
	 * @param file
	 * @return 
	 */
	private static Properties loadProperties(String file) {
        Path p = Paths.get(file);
		
		Properties prop = new Properties();
		try (InputStream is = Files.newInputStream(p.toAbsolutePath())) {
			prop.load(is);
		} catch (IOException ioe) {
			exit(-2, ioe.getMessage());
		}
		return prop;
	}

	/**
	 * Main
	 * 
	 * @param args 
	 */
    public static void main(String[] args) {
		LOG.info("Start");
		
		if (args.length == 0) {
			exit(-1, "Missing property files");
		}
		Properties prop = loadProperties(args[0]);
		
		// user authentication
		Drupal d10 = new Drupal(prop.getProperty("datagovbe.url"), prop.getProperty("datagovbe.auth"));
		try {
			d10.login(prop.getProperty("datagovbe.user"), prop.getProperty("datagovbe.pass"));
			for(String lang: new String[] { "nl", "fr", "de", "en" }) {
				List<Dataset> datasets = d10.getDatasets(lang);
				System.err.println(datasets);
			}
		} catch (IOException | InterruptedException ioe) {
			exit(-3, ioe.getMessage());
		}
		
		try {
			File f = new File(prop.getProperty("dcat.file"));
			DcatReader r = new DcatReader(f);
			r.getDatasets("nl");
		} catch (IOException ioe) {
			exit(-4, ioe.getMessage());
		}
	
		LOG.info("Done");
    }
}
