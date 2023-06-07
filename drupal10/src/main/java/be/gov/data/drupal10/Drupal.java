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
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONPointer;

/**
 * Drupal REST client
 * 
 * @author Bart.Hanssens
 */
public class Drupal {
	private final HttpClient client;
	private final String baseURL;
	private final String baseAuth;
	private String token;
	
	private final JSONPointer tid = new JSONPointer("/tid/0/value");
	private final JSONPointer name = new JSONPointer("/name/0/value");

	/**
	 * Get an HTTP request builder with authorization headers
	 * 
	 * @return 
	 */
	private HttpRequest.Builder getBuilder() {
		HttpRequest.Builder builder = HttpRequest.newBuilder();
		if (baseAuth != null) {
			builder.header("Authorization", baseAuth);
		}
		if (token != null) {
			builder.header("X-CSRF-Token", token);
		}
		return builder;
	}

	/**
	 * Log in with a username and password
	 * 
	 * @param user
	 * @param pass
	 * @return true if successful
	 * @throws IOException
	 * @throws InterruptedException 
	 */
	public boolean login(String user, String pass) throws IOException, InterruptedException {
		String str = new JSONObject().put("name", user).put("pass", pass).toString();
		HttpRequest request = getBuilder()
				.POST(BodyPublishers.ofString(str))
				.uri(URI.create(baseURL + "/user/login?_format=json"))
				.build();
		HttpResponse<String> response = client.send(request, BodyHandlers.ofString());
		System.err.println(response.body());
		JSONObject obj = new JSONObject(response.body());
		this.token = (String) obj.get("csrf_token");
		
		return (this.token != null);
	}

	/**
	 * Get taxonomy as a map
	 * 
	 * @param taxo name of the taxonomy
	 * @return map
	 * @throws IOException
	 * @throws InterruptedException 
	 */
	public Map<Integer,String> getTaxonomy(String taxo) throws IOException, InterruptedException {
		Map<Integer,String> map = new HashMap<>();
		
		// paginated result set
		for(int page = 1; ; page++) {
			HttpRequest request = getBuilder().GET()
				.uri(URI.create(baseURL + "/en/api/v1/taxonomy/" + taxo + "?_format=json&page=" + page))
				.build();

			HttpResponse<String> response = client.send(request, BodyHandlers.ofString());
			JSONArray terms = new JSONArray(response.body());
			if (terms.isEmpty()) {
				break;
			}
			for (Object obj: terms) {
				map.put((int) tid.queryFrom((JSONObject) obj), (String) name.queryFrom((JSONObject)obj));
			}
		}
		return map;
	}

	/**
	 * Create a new dataset
	 * 
	 * @param d dataset
	 * @return true if successful
	 * @throws IOException
	 * @throws InterruptedException 
	 */
	public boolean createDataset(Dataset d) throws IOException, InterruptedException {
		JSONObject obj = new JSONObject(d.toMap());

		HttpRequest request = getBuilder()
				.header("Content-type", "application/json")
				.POST(BodyPublishers.ofString(obj.toString()))
				.uri(URI.create(baseURL + "/node/dataset?_format=json"))
				.build();
		HttpResponse<String> response = client.send(request, BodyHandlers.ofString());
		return (response.statusCode() == 201);
	}

	/**
	 * Create a new dataset
	 * 
	 * @param d dataset
	 * @return true if successful
	 * @throws IOException
	 * @throws InterruptedException 
	 */
	public boolean updateDataset(Dataset d, String lang) throws IOException, InterruptedException {
		JSONObject obj = new JSONObject(d.toMap());

		HttpRequest request = getBuilder()
				.header("Content-type", "application/json")
				.method("PATCH", BodyPublishers.ofString(obj.toString()))
				.uri(URI.create(baseURL + "/node/dataset/" + d.id() + "?_format=json&_translation=" + lang))
				.build();
		HttpResponse<String> response = client.send(request, BodyHandlers.ofString());
		return (response.statusCode() == 201);
	}

	/**
	 * Get a dataset
	 * 
	 * @param id
	 * @param lang language code
	 * @return dataset if successful
	 * @throws IOException
	 * @throws InterruptedException 
	 */
	public Dataset getDataset(String id, String lang) throws IOException, InterruptedException {
		HttpRequest request = getBuilder()
				.GET()
				.uri(URI.create(baseURL + "/" + lang + "/node/dataset/" + id + "?_format=json"))
				.build();
		HttpResponse<String> response = client.send(request, BodyHandlers.ofString());
		JSONObject json = new JSONObject(response.body());
		return Dataset.fromMap(json.toMap());
	}

	/**
	 * Delete a dataset
	 * 
	 * @param id
	 * @return
	 * @throws IOException
	 * @throws InterruptedException 
	 */
	public boolean deleteDataset(String id) throws IOException, InterruptedException {
		HttpRequest request = getBuilder().DELETE()
				.uri(URI.create(baseURL + "/node/dataset/" + id + "?_format=json"))
				.build();
		HttpResponse<String> response = client.send(request, BodyHandlers.ofString());

		return (response.statusCode() == 204);
	}

	/**
	 * Constructor
	 * 
	 * @param baseURL Drupal base URL
	 * @param baseAuth basic authentication credentials or null
	 */
	public Drupal(String baseURL, String baseAuth) {
		this.baseURL = baseURL;
		this.baseAuth = (baseAuth != null) 
			? "Basic " + Base64.getEncoder().encodeToString(baseAuth.getBytes())
			: null;

		CookieManager cm = new CookieManager();
		CookieHandler.setDefault(cm);

		client = HttpClient.newBuilder()
			.cookieHandler(CookieHandler.getDefault())
			.version(Version.HTTP_1_1)
			.followRedirects(Redirect.NORMAL)
			.build();
	}
}
