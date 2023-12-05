/*
 * Copyright (c) 2023, Bart.Hanssens
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
package be.gov.data.uploaderd10.drupal;

import java.io.IOException;
import java.net.CookieHandler;
import java.net.CookieManager;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpClient.Redirect;
import java.net.http.HttpClient.Version;
import java.net.http.HttpRequest;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.util.HashMap;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONPointer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Drupal 10 custom REST client
 * 
 * @author Bart Hanssens
 */
public class DrupalClient {
	private static final Logger LOG = LoggerFactory.getLogger(DrupalClient.class);

	private final HttpClient client;
	private final String baseURL;
	private String token;

	/**
	 * Log in with username and password
	 * 
	 * @param user user name
	 * @param pass password
	 * @return CSRF token
	 * @throws IOException
	 * @throws InterruptedException 
	 */
	public String login(String user, String pass) throws IOException, InterruptedException {
		String str = new JSONObject().put("name", user).put("pass", pass).toString();
		HttpRequest request = HttpRequest.newBuilder()
				.POST(BodyPublishers.ofString(str))
				.uri(URI.create(baseURL + "/user/login?_format=json"))
				.build();
		HttpResponse<String> response = client.send(request, BodyHandlers.ofString());
		LOG.debug(response.body());
		JSONObject obj = new JSONObject(response.body());
		this.token = (String) obj.get("csrf_token");
		return this.token;
	}

	/**
	 * Get HTTP builder with token
	 * 
	 * @return builder
	 */
	private HttpRequest.Builder getHttpBuilder() {
		return HttpRequest.newBuilder().header("X-CSRF-Token", token);
	}

	/**
	 * Add taxonomy terms to a map
	 * 
	 * @param map map
	 * @param arr JSON Array 
	 */
	private void termsToMap(Map map, JSONArray arr) {
		JSONPointer tid = new JSONPointer("/tid/0/value");
		JSONPointer name = new JSONPointer("/name/0/value");
		for (Object obj: arr) {
			map.put((int) tid.queryFrom((JSONObject) obj),
					(String) name.queryFrom((JSONObject)obj));
		}
	}
	
	/**
	 * Get a Drupal taxonomy as a map with ID as key and taxonomy term as value
	 * 
	 * @param taxo name of the taxonomy
	 * @return map
	 * @throws IOException
	 * @throws InterruptedException 
	 */
	public Map<Integer,String> getTaxonomy(String taxo) throws IOException, InterruptedException {
		Map<Integer,String> map = new HashMap<>();
		
		for(int page =1; ; page++) {
			HttpRequest request = getHttpBuilder()
				.GET()
				.uri(URI.create(baseURL + "/en/api/v1/taxonomy/" + taxo + "?_format=json&page=" + page))
				.build();
			HttpResponse<String> response = client.send(request, BodyHandlers.ofString());
			JSONArray obj = new JSONArray(response.body());
			if (obj.isEmpty()) {
				break;
			}
			termsToMap(map, obj);
		}
		return map;
	}

	/**
	 * Create a new dataset on Drupal site
	 * 
	 * @param d dataset object
	 * @return true if successful
	 * @throws IOException
	 * @throws InterruptedException 
	 */
	public boolean createDataset(Dataset d) throws IOException, InterruptedException {
		JSONObject obj = new JSONObject(d.toMap());
		LOG.debug(obj.toString());

		HttpRequest request = getHttpBuilder()
				.header("Content-type", "application/json")
				.POST(BodyPublishers.ofString(obj.toString()))
				.uri(URI.create(baseURL + "/node?_format=json"))
				.build();
		HttpResponse<String> response = client.send(request, BodyHandlers.ofString());
		return (response.statusCode() == 201);
	}

	/**
	 * Delete a dataset on Drupal;
	 * 
	 * @param id
	 * @return
	 * @throws IOException
	 * @throws InterruptedException 
	 */
	public boolean deleteDataset(String id) throws IOException, InterruptedException {
			HttpRequest request = getHttpBuilder()
				.DELETE()
				.uri(URI.create(baseURL + "/node/" + id + "?_format=json"))
				.build();
		HttpResponse<String> response = client.send(request, BodyHandlers.ofString());
		LOG.debug(response.body());
		return (response.statusCode() == 204);
	}

	/**
	 * Constructor
	 * 
	 * @param baseURL 
	 */
	public DrupalClient(String baseURL) {
		this.baseURL = baseURL;

		CookieManager cm = new CookieManager();
		CookieHandler.setDefault(cm);

		client = HttpClient.newBuilder()
			.cookieHandler(CookieHandler.getDefault())
			.version(Version.HTTP_1_1)
			.followRedirects(Redirect.NORMAL)
			.build();
	}
}
