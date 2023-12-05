/*
 * Copyright (c) 2016, FPS BOSA DG DT
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
package be.gov.data.scrapers.biodiversity;

import be.gov.data.helpers.Storage;
import be.gov.data.scrapers.CkanJson;

import java.io.IOException;
import java.net.MalformedURLException;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.json.JsonObject;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.vocabulary.DCAT;
import org.eclipse.rdf4j.repository.RepositoryException;

/**
 * CKAN Belgian Biodiversity portal.
 *
 * @see http://www.biodiversity.be/
 * @author Bart Hanssens
 */
public class CkanBioDiversity extends CkanJson {

	// CKAN fields
	public final static String ADMIN_CONTACT = "administrative_contact";
	public final static Pattern REGEX_MAIL = Pattern.compile("([\\w._%-]+@[\\w.-]+\\.[A-Za-z]{2,8})");
	public final static String DWCA_URL = "dwca_url";

	@Override
	protected void ckanExtras(Storage store, IRI uri, JsonObject json, String lang)
			throws RepositoryException, MalformedURLException {
		String contact = json.getString(ADMIN_CONTACT, "");
		String email = extractEmail(contact);
		if (email != null && !email.isEmpty() && contact != null && !contact.isEmpty()) {
			contact = contact.replaceFirst(email, "").trim();
			parseContact(store, uri, contact, email);
		}
		parseURI(store, uri, json, DWCA_URL, DCAT.DOWNLOAD_URL);
	}
	
	/**
	 * Try to extract email from a string
	 *
	 * @param txt string
	 * @return empty string when not found
	 */
	private String extractEmail(String txt) {
		if (txt != null && !txt.isEmpty()) {
			Matcher m = REGEX_MAIL.matcher(txt);
			if (m.find()) {
				return m.group(1);
			}
		}
		return "";
	}

	/**
	 * Constructor
	 *
	 * @param prop
	 * @throws IOException
	 */
	public CkanBioDiversity(Properties prop) throws IOException {
		super(prop);
		setName("biodiversity");
	}
}
