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
package be.gov.data.translater;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Authenticator;
import java.net.HttpURLConnection;
import java.net.PasswordAuthentication;
import java.net.ProxySelector;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang3.StringUtils;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.util.Models;
import org.eclipse.rdf4j.model.util.Values;
import org.eclipse.rdf4j.model.vocabulary.DCAT;
import org.eclipse.rdf4j.model.vocabulary.DCTERMS;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.RDFHandlerException;
import org.eclipse.rdf4j.rio.Rio;
import org.eclipse.rdf4j.rio.UnsupportedRDFormatException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Send and retrieve translations from and to the etranslation proxy 
 * (which in turn will send request to the EU eTranslation service)
 * 
 * @author Bart Hanssens
 */
public class Translater {
	private final static Logger LOG = LoggerFactory.getLogger(Translater.class);

	// only translated specific properties of specific classes to reduce the workload
	private final static List<IRI> PROPERTIES = List.of(DCTERMS.DESCRIPTION, DCTERMS.TITLE);
	private final static List<IRI> CLASSES = List.of(DCAT.DATASET, DCAT.DATA_SERVICE);

	private final static Map<String,Map<String,String>> CACHE = new HashMap<>();

	private final static String TRANSFORMED = "-t-";

	private final HttpClient client;
	private final String baseURL;
	
	private int delay = 60;
	private int maxSize = 5000;

	/**
	 * Send a translation HTTP request
	 * 
	 * @param text text to translate
	 * @param source source language code
	 * @param target target language code
	 * @throws IOException
	 * @throws InterruptedException 
	 */
	private void sendTranslationRequest(String text, String source, String target) throws IOException, InterruptedException {
		URI uri = null;
		try {
			uri = new URI(baseURL + "/request/submit");
		} catch(URISyntaxException ue) {
			throw new IOException(ue);
		}

		StringBuilder sb = new StringBuilder();
		sb.append("sourceLang=").append(source).append("&targetLang=").append(target).append("&text=");
		sb.append(URLEncoder.encode(text, StandardCharsets.UTF_8));

		HttpRequest submit = HttpRequest.newBuilder()
								.POST(BodyPublishers.ofString(sb.toString()))
								.header("Content-Type", "application/x-www-form-urlencoded")
								.uri(uri)
								.build();
		HttpResponse<String> resp = client.send(submit, BodyHandlers.ofString(StandardCharsets.UTF_8));
		if (resp.statusCode() != HttpURLConnection.HTTP_ACCEPTED) {
			throw new IOException("Submit error " + resp.statusCode());
		}
	}

	/**
	 * Retrieve a translation from the proxy.
	 * If a translation is not available, assume it hasn't been requested yet and submit it for translation,
	 * then wait-and-retry a few times
	 * 
	 * @param text text to translate
	 * @param source source language code
	 * @param target target language code
	 * @return translated text or null (when retry fails repeatedly)
	 * @throws IOException
	 * @throws InterruptedException 
	 */
	private String getTranslation(String text, String source, String target) throws IOException, InterruptedException {
		String sha1 = DigestUtils.sha1Hex(text);
		
		Map<String, String> inCache = CACHE.get(sha1);

		if (inCache != null) {
			String hit = inCache.get(target + TRANSFORMED + source);
			if (hit != null) {
				LOG.debug("Cache hit for {} to {}", source, target);
				return hit;
			}
		}
		
		URI uri = null;
		try {
			uri = new URI(baseURL + "/request/retrieve?hash=" + sha1 + "&targetLang=" + target);
		} catch(URISyntaxException ue) {
			throw new IOException(ue);
		}
		HttpRequest retrieve = HttpRequest.newBuilder()
								.GET()
								.uri(uri)
								.build();
		HttpResponse<String> resp = client.send(retrieve, BodyHandlers.ofString(StandardCharsets.UTF_8));
		if (resp.statusCode() == HttpURLConnection.HTTP_OK) {
			// text was found
			return resp.body();
		}
		if (resp.statusCode() != HttpURLConnection.HTTP_NOT_FOUND) {
			throw new IOException("Retrieve error " + resp.statusCode());
		}
		// text not found, assume it hasn't been requested yet
		sendTranslationRequest(text, source, target);
		return null;	
	}

	/**
	 * Convert the non-blank literals into a map with language code as the key
	 * 
	 * @param literals literals (assumed to have a language tag)
	 * @return map
	 */
	private Map<String,String> toLangMap(Set<Literal> literals) {
		return literals.stream()
			.filter(l -> l.getLanguage().isPresent())
			.filter(l -> l.stringValue().trim().length() > 2)
			.collect(Collectors.toMap(l -> l.getLanguage().get(), l -> l.stringValue(), 
				(v1, v2) -> { 
					LOG.warn("Duplicate found {} {}", v1, v2); 
					return v1;
				} 
			));
	}
	
	/**
	 * Get the alphabetically lowest, not-translated language code of a series of literals
	 * 
	 * @param literals
	 * @return language code
	 */
	private String firstLang(Map<String,String> literals) {
		return literals.keySet().stream()
								.filter(t -> !t.contains(TRANSFORMED))
								.sorted()
								.findFirst().get();
	}

	/**
	 * Get the list of language codes a literal is available in, 
	 * without making a distinction between transformed (machine) or human translation
	 * 
	 * @param literals
	 * @return 
	 */
	private List<String> langTags(Map<String,String> literals) {
		return literals.keySet().stream()
								.map(t -> t.split(TRANSFORMED)[0])
								.distinct().toList();
	}

	/**
	 * Get the list of subject IRIs, limited to specific classes
	 * 
	 * @param m
	 * @return 
	 */
	private Set<Resource> getSubjects(Model m) {
		// limit translations to these CLASSES
		Set<Resource> subjects = new HashSet<>();
		for (IRI t: CLASSES) {
			subjects.addAll(m.filter(null, RDF.TYPE, t).subjects());
		}
		return subjects;
	}

	/**
	 * Get trimmed and possibly abbreviated string
	 * 
	 * @param body
	 * @param subj
	 * @param pred
	 * @return 
	 */
	private String getAbbrString(String body, Resource subj, IRI pred) {
		String str = body.trim();
		
		if (str.length() < 3) {
			LOG.warn("Text too short ({}) for {} {}, skipping", str.length(), subj, pred);
			return null;
		}

		if (str.length() >= maxSize) {
			LOG.warn("Text too long ({}, truncating) for {} {}", body.length(), subj, pred);
			str = StringUtils.abbreviate(body, maxSize);
		}
		return str;
	}

	/**
	 * Translate the literals of a set of properties for one or more classes
	 * 
	 * @param m RDF model
	 * @param langs languages to translate to
	 * @throws IOException 
	 */
	private int translationRound(Model m, List<String> langs) throws IOException {	
		int missing = 0;
		int count = 0;
		String translation;

		Set<Resource> subjects = getSubjects(m);

		LOG.info("Total nr of statements {}", m.size());
		for(Resource subj: subjects) {
			for (IRI pred: PROPERTIES) {
				LOG.debug("{} {}", subj, pred);
				List<String> wanted = new ArrayList<>(langs);
				Map<String,String> literals = toLangMap(Models.getPropertyLiterals(m, subj, pred));
				wanted.removeAll(langTags(literals));
	
				if (wanted.isEmpty()) {
					LOG.debug("All languages present for {} {}, skipping", subj, pred);
					continue;
				}
				if (literals.isEmpty()) {
					LOG.warn("No language literals for {} {}, skipping", subj, pred);
					continue;
				}
				// pick the lowest language tag as the source language
				String source = firstLang(literals);
				
				String str = getAbbrString(literals.get(source), subj, pred);
				if (str == null) {
					continue;
				}
				
				for (String target: wanted) {
					try {
						translation = getTranslation(str, source, target);
						if (++count % 200 == 0) {
							LOG.info("{} translations requested", count);
						}
						if (translation != null) {
							m.add(subj, pred, Values.literal(translation, target + TRANSFORMED + source));
						} else {
							missing++;
						}
					} catch (InterruptedException|IOException ioe) {
						missing++;
						LOG.error("Failed to translate {} {} to {}: {}", subj, pred, target, ioe.getMessage());	
					}
				}
			}
		}
		LOG.info("Round finished, {} translations requested", count);
		return missing;
	}
	
	/**
	 * Check if translation is still progressing or is stuck (= number of items to translate is not going down)
	 * 
	 * @param progress
	 * @param missing
	 * @return 
	 */
	private boolean checkStuck(List<Integer> progress, int missing) {
		progress.add(missing);
		int size = progress.size();
		if (size >= 5 && progress.get(size - 1).equals(progress.get(size - 5))) {
			LOG.error("Stuck...");
			return true;		
		}
		return false;
	}

	/**
	 * Use an existing file as cache
	 * 
	 * @param in
	 * @throws IOException 
	 */
	public void cache(InputStream in) throws IOException {
		Model m = Rio.parse(in, "http://data.gov.be", RDFFormat.NTRIPLES);

		Set<Resource> subjects = getSubjects(m);

		for(Resource subj: subjects) {
			for (IRI pred: PROPERTIES) {
				LOG.debug("{} {}", subj, pred);
				Map<String,String> literals = toLangMap(Models.getPropertyLiterals(m, subj, pred));
				
				if (literals.isEmpty()) {
					LOG.warn("No language literals for {} {}, skipping", subj, pred);
					continue;
				}
				// pick the lowest language tag as the source language
				String source = firstLang(literals);
				String body = getAbbrString(literals.get(source), subj, pred);
				if (body == null) {
					continue;
				}
		
				String sha1 = DigestUtils.sha1Hex(body);
				CACHE.put(sha1, literals);
			}
		}
		LOG.info("Caching {}", CACHE.size());
	}

	/**
	 * Translate titles and descriptions
	 * 
	 * @param in input stream
	 * @param out output streams
	 * @param langs languages to translate to
	 * @throws IOException 
	 * @throws java.lang.InterruptedException 
	 */
	public void translate(InputStream in, OutputStream out, List<String> langs) throws IOException, InterruptedException {
		Model m = Rio.parse(in, "http://data.gov.be", RDFFormat.NTRIPLES);
	
		List<Integer> progress = new ArrayList<>();
		int missing;
		boolean stuck = false;

		do {
			missing = translationRound(m, langs);
			if (missing > 0) {
				LOG.info("Missing translations: {}", missing);
				TimeUnit.SECONDS.sleep(delay);
				stuck = checkStuck(progress, missing);
			}
		} while (missing > 0 && !stuck);

		try {
			Rio.write(m, out, "http://data.gov.be", RDFFormat.NTRIPLES);
		} catch (RDFHandlerException|UnsupportedRDFormatException|URISyntaxException ex) {
			throw new IOException(ex);
		}
	}

	/**
	 * Constructor
	 * 
	 * @param baseURL URL of the proxy
	 * @param user HTTP Basic user name
	 * @param pass HTTP Basic password
	 */
	public Translater(String baseURL, String user, String pass) {
		this.client = HttpClient.newBuilder()
			.authenticator(new Authenticator() {
				@Override
				protected PasswordAuthentication getPasswordAuthentication() {
					return new PasswordAuthentication(user, pass.toCharArray());
			}})
			.proxy(ProxySelector.getDefault()).build();
		this.baseURL = baseURL;
	}
}
