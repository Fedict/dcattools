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
package be.gov.data.scrapers.geobe;

import be.gov.data.scrapers.Cache;
import be.gov.data.scrapers.Dcat;
import be.gov.data.scrapers.Page;
import java.io.IOException;
import java.net.URL;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

/**
 * Scraper for the NGI Geo.be portal "micro-service"
 * 
 * @see https://www.geo.be
 * @author Bart Hanssens
 */
public class DcatGeoBe extends Dcat {
	/**
	 * Scrape DCAT catalog.
	 *
	 * @param cache
	 * @throws IOException
	 */
	@Override
	protected void scrapeCat(Cache cache) throws IOException {
		int size = 1;

		boolean tryAgain = true;

		String prevhash = "";

		for(int start = 0; ;start += size) {
			URL url = new URL(getBase().toString() + "?f=dcat&startindex=" + start + "&limit=" + size);
			String xml = makeRequest(url);
			
			if (!tryAgain) {
				prevhash = detectLoop(prevhash, xml);
			}
			if (!xml.contains("Dataset") && !xml.contains("DataService")) {
				if (tryAgain) {
					LOG.info("Might be last page, try next one");
					tryAgain = false;
					continue;
				} else {
					LOG.info("Last (empty) page");
					break;
				}
			}
			tryAgain = true;
			cache.storePage(url, "all", new Page(url, xml));
			sleep();
		}
	}
	
	@Override
	public void scrape() throws IOException {
		LOG.info("Start scraping");
		Cache cache = getCache();

		Set<URL> urls = cache.retrievePageList();
		if (urls.isEmpty()) {
			scrapeCat(cache);
		}
		LOG.info("Done scraping");
	}

	/**
	 * Constructor
	 * 
	 * @param prop 
	 * @throws IOException 
	 */
	public DcatGeoBe(Properties prop) throws IOException {
		super(prop);
		setName("geobe");
	}
}
