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
package be.fedict.dcat.translater;

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

	private final static String TRANSFORMED = "-t-";

	private final HttpClient client;
	private final String baseURL;
	
	private int delay = 300;
	private int maxSize = 5000;

	/**
	 * e
	 * @param delay 
	 */
	public void setDelay(int delay) {
		this.delay = delay;
	}

	/**
	 * 
	 * @param maxSize 
	 */
	public void setMaxSize(int maxSize) {
		this.maxSize = maxSize;
	}

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
	 * Convert the literals into a map with language code as the key
	 * 
	 * @param literals literals (assumed to have a language tag)
	 * @return map
	 */
	private Map<String,String> toLangMap(Set<Literal> literals) {
		return literals.stream()
			.filter(l -> l.getLanguage().isPresent())
			.collect(Collectors.toMap(l -> l.getLanguage().get(), l -> l.stringValue(), 
				(v1, v2) -> { 
					LOG.warn("Duplicate found {} {}", v1, v2); 
					return v1;
				} 
			));
	}
	
	/**
	 * Get the alphabetically lowest language code of a series of literals
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

	private List<String> langTags(Map<String,String> literals) {
		return literals.keySet().stream()
								.map(t -> t.split(TRANSFORMED, 1)[0])
								.distinct().toList();
	}

	/**
	 * Translate titles and descriptions
	 * 
	 * @param m RDF model
	 * @param preds predicates to translate
	 * @param langs languages to translate to
	 * @throws IOException 
	 */
	private int translationRound(Model m, List<IRI> preds, List<String> langs) throws IOException {	
		int missing = 0;
		int count = 0;

		for(Resource subj: m.filter(null, RDF.TYPE, DCAT.DATASET).subjects()) {
			for (IRI pred: preds) {
				LOG.debug("{} {}", subj, pred);
				List<String> wanted = new ArrayList<>(langs);
				Map<String,String> literals = toLangMap(Models.getPropertyLiterals(m, subj, pred));
				wanted.removeAll(langTags(literals));
				
				if (wanted.isEmpty()) {
					LOG.debug("All languages present for {} {}", subj, pred);
					continue;
				}
				if (literals.isEmpty()) {
					LOG.warn("No language literals for {} {}", subj, pred);
					continue;
				}
				// pick the lowest language tag as the source language
				String source = firstLang(literals);
				String body = literals.get(source);
				if (body.length() >= maxSize) {
					LOG.warn("Text too long ({}, truncating) for {} {}", body.length(), subj, pred);
					body = StringUtils.abbreviate(body, maxSize);
				}

				for (String target: wanted) {
					LOG.debug("Get translation for {} {} to {}", subj, pred, target);

					try {
						String translation = getTranslation(body, source, target);
						count++;
						if (count % 200 == 0) {
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
	 * Translate titles and descriptions
	 * 
	 * @param in input stream
	 * @param out output streams
	 * @param langs languages to translate to
	 * @throws IOException 
	 * @throws java.lang.InterruptedException 
	 */
	public void translate(InputStream in, OutputStream out, List<String> langs) throws IOException, InterruptedException {
		List<IRI> preds = List.of(DCTERMS.DESCRIPTION, DCTERMS.TITLE);
		Model m = Rio.parse(in, "http://data.gov.be", RDFFormat.NTRIPLES);
	
		int missing;
		do {
			missing = translationRound(m, preds, langs);
			if (missing > 0) {
				LOG.info("Missing translations: {}", missing);
				TimeUnit.SECONDS.sleep(delay);
			}
		} while (missing > 0);

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
