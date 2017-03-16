/*
 * Copyright (c) 2016, Bart Hanssens <bart.hanssens@fedict.be>
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

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.json.JsonString;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * CKAN Brussels Region.
 *
 * @author Bart Hanssens <bart.hanssens@fedict.be>
 */
public class CkanBrussels extends CkanRDF {

	private final Logger logger = LoggerFactory.getLogger(CkanBrussels.class);

	/**
	 * Get the list of all the CKAN packages (DCAT Dataset).
	 *
	 * @return List of URLs
	 * @throws MalformedURLException
	 * @throws IOException
	 */
	@Override
	protected List<URL> scrapePackageList() throws MalformedURLException, IOException {
		List<URL> urls = new ArrayList<>();
		URL getPackages = new URL(getBase(), Ckan.API_LIST);

		JsonObject obj = makeJsonRequest(getPackages);
		if (!obj.getBoolean(Ckan.SUCCESS)) {
			return urls;
		}
		JsonArray arr = obj.getJsonArray(Ckan.RESULT);
		for (JsonString str : arr.getValuesAs(JsonString.class)) {
			String url = str.getString();
			if (url.endsWith("-harvester")) {
				logger.info("Remove dummy dataset {}", url);
			} else {
				urls.add(ckanDatasetURL(url));
			}
		}
		return urls;
	}

	/**
	 * Constructor
	 *
	 * @param caching DB cache file
	 * @param storage SDB file to be used as triple store backend
	 * @param base base URL
	 */
	public CkanBrussels(File caching, File storage, URL base) {
		super(caching, storage, base);
		setName("brussels");
	}
}
