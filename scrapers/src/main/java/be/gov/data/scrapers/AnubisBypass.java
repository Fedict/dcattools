/*
 * Copyright (c) 2026, FPS BOSA
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
package be.gov.data.scrapers;

import be.gov.data.dcat.helpers.Fetcher;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import org.apache.http.cookie.Cookie;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

/**
 * Bypass Anubis blocker
 * See also https://anubis.techaro.lol/docs/design/how-anubis-works/
 * 
 * @author Bart.Hanssens
 */
public class AnubisBypass {
    private static final String CHALLENGE = "/.within.website/x/cmd/anubis/api/pass-challenge";	
	private static final HexFormat HEX = HexFormat.of();
	private static final ObjectMapper MAPPER = new ObjectMapper();
	
	protected final static Logger LOG = LoggerFactory.getLogger(AnubisBypass.class);
	protected static MessageDigest DIGESTER;

	static {
		try {
			DIGESTER = MessageDigest.getInstance("SHA-256");
		} catch (NoSuchAlgorithmException ex) {
			// really shouldn't happen...
			LOG.error("SHA-256 algorithm not found");
		}
	}

	private record Solved(long nonce, String hex) {};
	
	/**
	 * Solve the challenge
	 * 
	 * @param rnd random string
	 * @param difficulty number of leading '0's
	 * @return answer
	 */
	private static Solved solve(String rnd, int difficulty) {
		String prefix = "0".repeat(difficulty);
		long nonce = -1;
		String hex;

		LOG.info("Solving anubis challenge {}, difficulty {}", rnd, difficulty);
	 	do {
			String data = rnd + ++nonce;
			byte[] hash = DIGESTER.digest(data.getBytes(StandardCharsets.UTF_8));
			hex = HEX.formatHex(hash);
		} while (!hex.startsWith(prefix));

		return new Solved(nonce, hex);
    }

	/**
	 * Get cookies
	 * 
	 * @param url
	 * @return
	 * @throws IOException 
	 */
	public static List<Cookie> getCookie(URL url) throws IOException {
		Fetcher f = new Fetcher();

		String page = f.makeRequest(url);
		Document doc = Jsoup.parse(page);
		Element script  = doc.getElementById("anubis_challenge");
		if (script == null) {
			LOG.warn("No anubis script tag found");
			return List.of();
		}

		JsonNode root = MAPPER.readTree(script.data());
		JsonNode challenge = root.get("challenge");
		if (challenge == null) {
			throw new IOException("No anubis challenge in JSON tree found");
		}
		
		String id = challenge.get("id").asString();
		String rnd = challenge.get("randomData").asString();
		int difficulty = challenge.get("difficulty").asInt();
		
		if (difficulty < 0 || difficulty > 5) {
			throw new IOException("Invalid anubis difficulty " + difficulty);
		}

		long start = System.currentTimeMillis();
		Solved solved = solve(rnd, difficulty);
		long duration = System.currentTimeMillis() - start;
				
		try {
			URL pass = url.toURI().resolve(CHALLENGE).toURL();
			LOG.info("Sending solution to pass {} ", pass);

			return f.makeCookieRequest(pass, Map.of("id", id, "response", solved.hex, 
															"redir", url.toString(),
															"nonce", Long.toString(solved.nonce()),
															"elapsedTime", Long.toString(duration)));
		} catch (URISyntaxException ex) {
			throw new IOException(ex);
		}
	}
}
