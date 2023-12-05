/*
 * Copyright (c) 2023, FPS BOSA DG DT
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
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.util.Properties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Scraper interface
 * 
 * @author Bart Hanssens
 */
public class BaseScraperFactory {
	private final static Logger logger = LoggerFactory.getLogger(BaseScraperFactory.class);

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
	 * Load and configure a scraper
	 *
	 * @param prop properties for additional configuration
	 */
	private static BaseScraper configureScraper(Properties prop) throws IOException {
		try {
			String cname = prop.getProperty(BaseScraper.PROP_PREFIX + ".classname");
			Class<? extends BaseScraper> c = Class.forName(cname).asSubclass(BaseScraper.class);
			return c.getConstructor(Properties.class).newInstance(prop);
		} catch (ClassNotFoundException | InstantiationException | NoSuchMethodException
				| IllegalAccessException | InvocationTargetException ex) {
			throw new IOException("Scraper class could not be loaded", ex);
		}
	}

	/**
	 * Get a configured scraper
	 * 
	 * @param name alias
	 * @param dataDir data directory
	 * @return 
	 * @throws java.io.IOException 
	 */
	public static BaseScraper getConfiguredScraper(String name, String dataDir) throws IOException {
		Properties prop = loadProperties(name);
		
		prop.setProperty(BaseScraper.PROP_PREFIX + ".datadir", dataDir);
		// set cache, note that the scraper will not use the site-to-be-scraped when the cache is not empty
		String cache = String.join(File.separator, dataDir, "cache");
		prop.setProperty(BaseScraper.PROP_PREFIX + ".cache", cache);
		
		return configureScraper(prop);
	}

}
