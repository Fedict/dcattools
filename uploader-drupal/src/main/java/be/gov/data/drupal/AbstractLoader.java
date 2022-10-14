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
package be.gov.data.drupal;

import jakarta.json.JsonObject;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import org.apache.http.HttpHeaders;
import org.apache.http.HttpResponse;
import org.apache.http.client.fluent.Request;
import org.apache.http.entity.ContentType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *  Load and retrieve JSON objects to/from Drupal 9
 * 
 * @author Bart Hanssens
 */
public abstract class AbstractLoader {
	private final static Logger LOG = LoggerFactory.getLogger(AbstractLoader.class);

	private final String website;
	private final String user;
	private final String pass;
	private final String auth;

	/**
	 * Get base URL of the website
	 * 
	 * @return 
	 */
	public String getWebsite() {
		return website;
	}

	/**
	 * Get Basic Auth (Base64 encoded) username and password
	 * 
	 * @return 
	 */
	public String getBasicAuth() {
		return auth;
	}

	/**
	 * POST a JSON object to the Drupal website
	 * 
	 * @param path
	 * @param obj
	 * @return
	 * @throws IOException 
	 */
	public HttpResponse postRequest(String path, JsonObject obj) throws IOException {
		LOG.info(obj.toString());
	
		Request req = Request.Post(getWebsite() + path)
			.setHeader(HttpHeaders.AUTHORIZATION, "Basic " + getBasicAuth())
			.bodyString(obj.toString(), ContentType.create("application/vnd.api+json"));
		HttpResponse resp = req.execute().returnResponse();

		LOG.info(resp.toString());
		return resp;
	}

	/**
	 * Constructor
	 * 
	 * @param website base url of the website
	 * @param user user name
	 * @param pass password
	 */
	protected AbstractLoader(String website, String user, String pass) {
		this.website = website;
		this.user = user;
		this.pass = pass;
		this.auth = Base64.getEncoder().encodeToString((user + ":" + pass).getBytes(StandardCharsets.ISO_8859_1));
	}
}
