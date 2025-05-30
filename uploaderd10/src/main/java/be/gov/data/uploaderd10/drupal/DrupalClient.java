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
package be.gov.data.uploaderd10.drupal;

import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
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
import java.nio.file.Files;
import java.nio.file.Path;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONPointer;
import org.json.JSONPointerException;

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
	private final Path cacheDir;
	
	private String token;
	private String logout;
	private String uid;

	/**
	 * Get HTTP builder with token
	 * 
	 * @return builder
	 */
	private HttpRequest.Builder getHttpBuilder() {
		return HttpRequest.newBuilder()
			.header("X-CSRF-Token", token)
			.header("User-Agent", "Drupal Updater");
	}

	/**
	 * Add taxonomy terms to a map
	 * 
	 * @param map map
	 * @param arr JSON Array 
	 */
	private void termsToMap(Map<String,Integer> map, JSONArray arr) {
		JSONPointer tid = new JSONPointer("/tid/0/value");
		JSONPointer uri = new JSONPointer("/field_uri/0/uri");
		for (Object obj: arr) {
			try {
				map.put(
					(String) uri.queryFrom((JSONObject)obj),
					(int) tid.queryFrom((JSONObject) obj));
			} catch (JSONPointerException e) {
				LOG.error("Error in JSON {} {}", e.getMessage(), obj.toString());
			}
		}
	}

	/**
	 * Log in with username and password
	 * 
	 * @param user user name
	 * @param pass password
	 * @throws IOException
	 * @throws InterruptedException 
	 */
	public void login(String user, String pass) throws IOException, InterruptedException {
		HttpRequest re = HttpRequest.newBuilder()
				.GET()
				.uri(URI.create(baseURL))
				.build();
		HttpResponse<String> r = client.send(re, BodyHandlers.ofString());
		String str = new JSONObject().put("name", user).put("pass", pass).toString();
		HttpRequest request = HttpRequest.newBuilder()
				.POST(BodyPublishers.ofString(str))
				.uri(URI.create(baseURL + "/user/login?_format=json"))
				.build();
		HttpResponse<String> response = client.send(request, BodyHandlers.ofString());
		LOG.debug(response.body());
		
		if (response.statusCode() == 403) {
			throw new IOException("Authentication denied :" + response.body());
		}

		JSONObject obj = new JSONObject(response.body());
		this.token = (String) obj.get("csrf_token");

		if (this.token != null) {
			LOG.info("Logged in");
			this.logout = (String) obj.get("logout_token");
			this.uid = (String) ((JSONObject) obj.getJSONObject("current_user")).get("uid");
		} else {
			LOG.error("Failed to get CSRF login token");
		}
	}

	/**
	 * Log out of Drupal
	 * 
	 * @throws IOException
	 * @throws InterruptedException 
	 */
	public void logout() throws IOException, InterruptedException {
		if (logout == null) {
			LOG.error("No logout token");
			return;
		}
		HttpRequest request = getHttpBuilder()
				.POST(BodyPublishers.ofString(""))
				.uri(URI.create(baseURL + "/user/logout?_format=json&token=" + logout))
				.build();
		HttpResponse<String> response = client.send(request, BodyHandlers.ofString());
		LOG.debug(response.body());

		this.token = null;
		this.uid = null;
		this.logout = null;
		
		LOG.info("Logged out");
	}

	private Map<String,Integer> loadCache(String taxo) {
		if (cacheDir == null) {
			LOG.debug("Cache directory not set");
			return null;
		}
		Path cache = cacheDir.resolve(taxo);
		if (! cache.toFile().exists()) {
			LOG.warn("Cache file {} not found", cache);
			return null;
		}
		try (InputStream is = Files.newInputStream(cache);
			ObjectInputStream ois = new ObjectInputStream(is)) {
			LOG.info("Reading {} from cache", taxo);
			return (Map<String,Integer>) ois.readObject();
		} catch(IOException|ClassNotFoundException e) {
			LOG.error("Error reading cache {} : {}", cache, e.getMessage());
			return null;
		}
	}

	private void storeCache(String taxo, Map<String,Integer> map) {
		if (cacheDir == null) {
			LOG.debug("Cache directory not set");
			return;
		}
		if (!cacheDir.toFile().exists() && !cacheDir.toFile().mkdirs()) {
			LOG.error("Cache directory {} not found and could not be created", cacheDir);
			return;
		}

		Path cache = cacheDir.resolve(taxo);
		try (OutputStream os = Files.newOutputStream(cache);
			ObjectOutputStream oos = new ObjectOutputStream(os)) {
			LOG.info("Writing {} to cache", taxo);
			oos.writeObject(map);
		} catch(IOException e) {
			LOG.error("Error writing cache {} : {}", cache, e.getMessage());
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
	public Map<String,Integer> getTaxonomy(String taxo) throws IOException, InterruptedException {
		Map<String,Integer> map;
		
		map = loadCache(taxo);
		if (map != null) {
			return map;
		}
		map = new HashMap<>();
		
		for(int page = 0; ; page++) {
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
		LOG.info("{}: {} terms", taxo, map.size());
		storeCache(taxo, map);

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
	public Integer createDataset(DrupalDataset d) throws IOException, InterruptedException {
		JSONObject obj = new JSONObject(d.toMap());		
		LOG.debug("{}", obj.toString());

		HttpRequest request = getHttpBuilder()
				.header("Content-type", "application/json")
				.POST(BodyPublishers.ofString(obj.toString()))
				.uri(URI.create(baseURL + "/node/dataset?_format=json"))
				.build();
		HttpResponse<String> response = client.send(request, BodyHandlers.ofString());

		if (response.statusCode() == 201) {
			JSONObject o = new JSONObject(response.body());
			DrupalDataset created = DrupalDataset.fromMap(o.toMap());
			return created.nid();
		} else {
			LOG.error("Create failed, code {}, {} {}", response.statusCode(), response.body(), obj.toString());
		}
		return -1;
	}

	/**
	 * Update a dataset in a specific language, or add a translation
	 * 
	 * @param id
	 * @param d dataset
	 * @param lang language code
	 * @return true if successful
	 * @throws IOException
	 * @throws InterruptedException 
	 */
	public boolean updateDataset(Integer id, DrupalDataset d, String lang) throws IOException, InterruptedException {
		JSONObject obj = new JSONObject(d.toMap());

		HttpRequest request = getHttpBuilder()
				.header("Content-type", "application/json")
				.method("PATCH", BodyPublishers.ofString(obj.toString()))
				.uri(URI.create(baseURL + "/node/" + id.toString() + "?_format=json&_translation=" + lang))
				.build();
		HttpResponse<String> response = client.send(request, BodyHandlers.ofString());
		if (response.statusCode() == 200 || response.statusCode() == 201) {
			return true;
		} else {
			LOG.error("Update {} failed, code {}, {} {}", id, response.statusCode(), response.body(), obj.toString());
		}
		return false;
	}

	/**
	 * Delete a dataset on Drupal;
	 * 
	 * @param id
	 * @return
	 * @throws IOException
	 * @throws InterruptedException 
	 */
	public boolean deleteDataset(Integer id) throws IOException, InterruptedException {
		HttpRequest request = getHttpBuilder()
				.DELETE()
				.uri(URI.create(baseURL + "/node/" + id + "?_format=json"))
				.build();
		HttpResponse<String> response = client.send(request, BodyHandlers.ofString());
		if (response.statusCode() == 204) {
			return true;
		} else {
			LOG.error("Delete {} failed, code {}", id, response.statusCode());
		}
		return false;
	}

	/**
	 * Get all datasets
	 * 
	 * @param lang language code
	 * @return dataset if successful
	 * @throws IOException
	 * @throws InterruptedException 
	 */
	public List<DrupalDataset> getDatasets(String lang) throws IOException, InterruptedException {
		List<DrupalDataset> lst = new ArrayList<>();
		Set<Integer> ids = new HashSet<>();

		// paginated result set
		for(int page = 0; ; page++) {
			HttpRequest request = getHttpBuilder()
				.GET()
				.uri(URI.create(baseURL + "/" + lang + "/api/v1/content/dataset/" + uid + "?_format=json&page=" + page))
				.build();

			HttpResponse<String> response = client.send(request, BodyHandlers.ofString());
			if (response.statusCode() == 503) {
				LOG.warn("Backend fetch failed, retrying...");
				response = client.send(request, BodyHandlers.ofString());
			}
			if (response.statusCode() != 200) {
				throw new IOException("Failed to retrieve page " + page + " as JSON " + response.body());
			}
			JSONArray datasets = new JSONArray(response.body());
			if (datasets.isEmpty()) {
				break;
			}
			for (Object obj: datasets) {
				DrupalDataset ds = DrupalDataset.fromMap(((JSONObject) obj).toMap());
				if (ids.add(ds.nid())) {
					lst.add(ds);
					if (lst.size() % 100 == 0) {
						LOG.info("Retrieved {} datasets from site", lst.size());
					}
				} else {
					LOG.error("Dataset {} already in the list", ds.nid());
				}
			}
		}
		LOG.info("Retrieved {} datasets from site", lst.size());
		return lst;
	}

	/**
	 * Constructor
	 * 
	 * @param baseURL 
	 * @param cacheDir 
	 */
	public DrupalClient(String baseURL, Path cacheDir) {
		this.baseURL = baseURL;
		this.cacheDir = cacheDir;

		CookieManager cm = new CookieManager();
		CookieHandler.setDefault(cm);

		client = HttpClient.newBuilder()
			.cookieHandler(CookieHandler.getDefault())
			.version(Version.HTTP_1_1)
			.followRedirects(Redirect.NORMAL)
			.build();
	}
}
