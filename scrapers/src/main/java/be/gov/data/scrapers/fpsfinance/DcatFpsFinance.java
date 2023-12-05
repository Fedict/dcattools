/*
 * Copyright (c) 2022, FPS BOSA DG DT
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
package be.gov.data.scrapers.fpsfinance;

import be.gov.data.scrapers.Cache;
import be.gov.data.scrapers.Dcat;
import be.gov.data.scrapers.Page;
import java.io.IOException;
import java.net.URL;

import java.util.Properties;

/**
 * DCAT file for BeST opendata
 *
 * @see https://opendata.bosa.be/
 * @author Bart Hanssens
 */
public class DcatFpsFinance extends Dcat {
	
	/**
	 * Scrape DCAT catalog.
	 *
	 * @param cache
	 * @throws IOException
	 */
	@Override
	protected void scrapeCat(Cache cache) throws IOException {
		URL url = getBase();
		String content = makeRequest(url);
		// FIX incorrect suborganizations
		content = content.replace(
			"<dct:creator><foaf:Organization xsi:schemaLocation=\"http://www.w3.org/1999/02/22-rdf-syntax-ns# http://www.openarchives.org/OAI/2.0/rdf.xsd\" rdf:about=\"https://org.belgif.be/id/CbeRegisteredEntity/0308357159\">",
			"<dct:creator><foaf:Organization xsi:schemaLocation=\"http://www.w3.org/1999/02/22-rdf-syntax-ns# http://www.openarchives.org/OAI/2.0/rdf.xsd\">");

		cache.storePage(url, "all", new Page(url, content));
	}
	
	/**
	 * Constructor
	 * 
	 * @param prop 
	 * @throws IOException 
	 */
	public DcatFpsFinance(Properties prop) throws IOException {
		super(prop);
		setName("fpsfinance");
	}
}
