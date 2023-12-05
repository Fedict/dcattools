/*
 * Copyright (c) 2019, FPS BOSA DG DT
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
package be.gov.data.scrapers.globe;

import be.gov.data.helpers.Storage;
import be.gov.data.scrapers.Ckan;
import be.gov.data.scrapers.CkanJson;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import javax.json.JsonArray;
import javax.json.JsonObject;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.repository.RepositoryException;

/**
 * CKAN BTC-CTB. Currently (9/2016) the JSON version is more reliable than the RDF
 *
 * @see https://www.glo-be.be/
 * @author Bart Hanssens
 */
public class CkanGlobe extends CkanJson {

	private final static String BTC = "be-dgd";

	@Override
	protected void ckanExtras(Storage store, IRI uri, JsonObject json, String lang)
		throws RepositoryException, MalformedURLException {
		//
	}

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
		URL getPackages = new URL(getBase(), Ckan.API_ORG + BTC + "&include_datasets=true");

		JsonObject obj = makeJsonRequest(getPackages);
		if (!obj.getBoolean(Ckan.SUCCESS)) {
			return urls;
		}
		JsonObject res = obj.getJsonObject(Ckan.RESULT);
		JsonArray pkgs = res.getJsonArray(Ckan.PACKAGES);
		for (JsonObject pkg : pkgs.getValuesAs(JsonObject.class)) {
			urls.add(ckanDatasetURL(pkg.getString("name")));
		}
		return urls;
	}

	/**
	 * Constructor
	 *
	 * @param prop
	 * @throws IOException
	 */
	public CkanGlobe(Properties prop) throws IOException {
		super(prop);
		setName("globe");
	}

}
