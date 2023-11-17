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
import java.net.ProxySelector;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import org.apache.commons.codec.digest.DigestUtils;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Literal;

import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.util.Models;
import org.eclipse.rdf4j.model.util.Values;
import org.eclipse.rdf4j.model.vocabulary.DCTERMS;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.RDFHandlerException;
import org.eclipse.rdf4j.rio.Rio;
import org.eclipse.rdf4j.rio.UnsupportedRDFormatException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Bart Hanssens
 */
public class Translater {
	private final static Logger LOG = LoggerFactory.getLogger(Main.class);

	private final HttpClient client;
	private final String baseURL;
	private final String authHeader;
	
	private int delay = 60;
	private int retry = 5;

	private void sendTranslationRequest(String body, String source, String target) throws IOException, InterruptedException {
		URI uri = null;
		try {
			uri = new URI(baseURL + "/request/submit");
		} catch(URISyntaxException ue) {
			throw new IOException(ue);
		}
		HttpRequest submit = HttpRequest.newBuilder()
								.POST(BodyPublishers.ofString(body))
								.uri(uri)
								.header("Authorization", authHeader)
								.build();
		HttpResponse<String> resp = client.send(submit, BodyHandlers.ofString(StandardCharsets.UTF_8));
		if (resp.statusCode() == 500) {
			throw new IOException(resp.body());
		}
	}

	private String getTranslation(String body, String source, String target) throws IOException, InterruptedException {
		String sha1 = DigestUtils.sha1Hex(body);
		
		URI uri = null;
		try {
			uri = new URI(baseURL + "/request/retrieve?hash=" + sha1 + "&targetLang=" + target);
		} catch(URISyntaxException ue) {
			throw new IOException(ue);
		}	
		HttpRequest retrieve = HttpRequest.newBuilder()
								.GET()
								.uri(uri)
								.header("Authorization", authHeader)
								.build();
		HttpResponse<String> resp = client.send(retrieve, BodyHandlers.ofString(StandardCharsets.UTF_8));
		if (resp.statusCode() == 200) {
			return resp.body();
		}

		sendTranslationRequest(body, source, target);
		for(int i = retry; i > 0; i--) {
			TimeUnit.SECONDS.sleep(delay);
			resp = client.send(retrieve, BodyHandlers.ofString(StandardCharsets.UTF_8));
			if (resp.statusCode() == 200) {
				return resp.body();
			}
		}
		LOG.error("No translation for {} {}, retried {}", source, target, retry);
		return null;
	}

	private Map<String,String> toLangMap(Set<Literal> literals) {
		return literals.stream()
			.filter(l -> l.getLanguage().isPresent())
			.collect(Collectors.toMap(l -> l.getLanguage().get(), l -> l.stringValue()));
	}
	
	private String firstLang(Map<String,String> literals) {
		return literals.keySet().stream().sorted().findFirst().get();
	}

	public void translate(InputStream in, OutputStream out, List<String> langs) throws IOException {
		List<IRI> preds = List.of(DCTERMS.DESCRIPTION, DCTERMS.TITLE);

		Model m = Rio.parse(in, "http://data.gov.be", RDFFormat.NTRIPLES);
		
		for(Resource subj: m.subjects()) {
			for (IRI pred: preds) {
				List<String> missing = new ArrayList<>(langs);
				Map<String,String> literals = toLangMap(Models.getPropertyLiterals(m, subj, pred));
				missing.removeAll(literals.keySet());
				
				if (missing.isEmpty()) {
					LOG.debug("All languages present for {} {}", subj, pred);
					continue;
				}

				String source = firstLang(literals);
				String body = literals.get(source);

				for (String target: missing) {
					LOG.debug("Get translation {} for {} {}", target, subj, pred);
					try {
						String translation = getTranslation(body, source, target);
						if (translation != null) {
							m.add(subj, pred, Values.literal(translation, target));
							LOG.info("Translation added");
						}
					} catch (InterruptedException|IOException ioe) {
						LOG.error("Failed to translate {} {} to", subj, pred, target);	
					}
				}
			}
		}

		try {
			Rio.write(m, out, "http://data.gov.be", RDFFormat.NTRIPLES);
		} catch (RDFHandlerException|UnsupportedRDFormatException|URISyntaxException ex) {
			throw new IOException(ex);
		}
	}

	public Translate(String baseURL, String user, String pass) {
		this.client = HttpClient.newBuilder().proxy(ProxySelector.getDefault()).build();
		this.baseURL = baseURL;
		this.authHeader = "Basic " + Base64.getEncoder().encodeToString((user + ":'" + pass).getBytes());
	}
}
